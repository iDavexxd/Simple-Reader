package app.simplereader.controller;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class ReaderController {
    private ScnReader view;
    
    private static ReaderController instance;
    private SceneController nav = SceneController.getInstance();
    private final LibraryController lib = LibraryController.getInstance();
    
    private Manga manga;
    private Chapter chapter;
    private int chapterIndex;
    private int currentPageIndex;
    
    private boolean menuVisible = true;
    private boolean inZoom = false;
    private boolean updatingUI = false;
    
    private final Set<Integer> loadingPages = Collections.synchronizedSet(new HashSet<>());
    private final java.util.Map<Integer, Image> cache = Collections.synchronizedMap(new java.util.HashMap<>());
    
    private ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "preloader");
        t.setDaemon(true);
        return t;
    });
    
    private ComboBox<Integer> paginaComboBox;
    private ComboBox<Chapter> chapterComboBox;
    private javafx.scene.control.Label chapterNameLabel;
    
    public ReaderController(ScnReader view){
        this.view = view;
    }
    
    public static void doInstance(ScnReader view){
        instance = new ReaderController(view);
    }
    
    public static ReaderController getInstance(){
        return instance;
    }
    
    public void init(Manga manga, Chapter chapter, int chapterIndex){
        this.manga = manga;
        this.chapter = chapter;
        this.chapterIndex = chapterIndex;
        this.currentPageIndex = chapter.isReaded() ? 0 : chapter.getLastRead();
    }
    
    public void setUIComponents(ComboBox<Integer> pagina, ComboBox<Chapter> caps, javafx.scene.control.Label chnameLabel){
        this.paginaComboBox = pagina;
        this.chapterComboBox = caps;
        this.chapterNameLabel = chnameLabel;
    }
    
    public void loadChapter(Chapter chapter, int index){
        cache.clear(); 
        preloader.shutdownNow();
        preloader = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "preloader");
            t.setDaemon(true);
            return t;
        });
        
        this.chapter = chapter;
        this.chapterIndex = index;
        this.currentPageIndex = chapter.isReaded() ? 0 : chapter.getLastRead();
        
        view.setChapter(chapter);
        nav.getStage().setTitle(view.getName());
        
        if (paginaComboBox != null) {
            paginaComboBox.getItems().clear();
            for (int i = 1; i <= chapter.getPageCount(); i++) {
                paginaComboBox.getItems().add(i);
            }
            updatingUI = true;
            paginaComboBox.setValue(currentPageIndex + 1);
            updatingUI = false;
        }
        
        if (chapterNameLabel != null) {
            chapterNameLabel.setText(chapter.getTitle());
        }
        
        if (totalPages() > 0) {
            loadCurrentImage();
        }
    }
    
    public int totalPages() {
        return chapter != null ? chapter.getPageCount() : 0;
    }
    
    public void nextPage() {
        if (currentPageIndex < totalPages() - 1) {
            goToPage(currentPageIndex + 1);
        } else {
            nextChapter();
        }   
    }
    
    public void previousPage() {
        if (currentPageIndex > 0) {
            goToPage(currentPageIndex - 1);
        } else {
            previousChapter();
        }
    }
    
    public void goToPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        
        if (index == totalPages() - 1){
            chapter.markAsReaded();
            lib.saveLibrary();
        }
        
        currentPageIndex = index;
        if (!chapter.isReaded()) {
            chapter.setLastRead(currentPageIndex);
        }
        
        loadCurrentImage();
        
        if (paginaComboBox != null) {
            updatingUI = true;
            paginaComboBox.setValue(currentPageIndex + 1);
            updatingUI = false;
        }
    }
    
    public void nextChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex < chapters.size() - 1) {
            Chapter next = chapters.get(chapterIndex + 1);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                List<String> pages = src.getPages(manga.getMangaID(), next.getChapterID());
                next.setPages(pages);
            }
            
            if (next.hasPages()) {
                chapter.markAsReaded();
                loadChapter(next, chapterIndex + 1);
                if (chapterComboBox != null) {
                    chapterComboBox.setValue(next);
                }
            } else {
                Logger.noPagesAlert(next.getTitle());
            }
        }
    }
    
    public void previousChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex > 0) {
            Chapter prev = chapters.get(chapterIndex - 1);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                List<String> pages = src.getPages(manga.getMangaID(), prev.getChapterID());
                prev.setPages(pages);
            }
            
            if (prev.hasPages()) {
                loadChapter(prev, chapterIndex - 1);
                if (chapterComboBox != null) {
                    chapterComboBox.setValue(prev);
                }
                if (paginaComboBox != null) {
                    updatingUI = true;
                    paginaComboBox.setValue(prev.getPageCount());
                    updatingUI = false;
                }
            } else {
                Logger.noPagesAlert(prev.getTitle());
            }
        }  
    }
    
    public void loadCurrentImage() {
        try {
            Image img = getPage(currentPageIndex);
            
            view.setImageViewImage(img);
            
            if (img.isBackgroundLoading() && img.getProgress() < 1.0) {
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            view.setImageViewImage(img);
                            view.fitImageToScreen();
                            resetZoom();
                        });
                    }
                });
            }
            
            preloadAroundCurrent();
            cleanCache();
            view.fitImageToScreen();
            resetZoom();
            
            Logger.info("Page " + currentPageIndex + " loaded.");
        } catch (Exception e) {
            Logger.error("Error cargando imagen: " + e.getMessage());
        }
    }
    
    public Image getPage(int index) {
        if (cache.containsKey(index)) return cache.get(index);
        Image img = new Image(chapter.getPage(index), true);
        cache.put(index, img);
        return img;
    }
    
    public void cleanCache() {
        int range = 3;
        System.gc();
        cache.keySet().removeIf(i -> Math.abs(i - currentPageIndex) > range);
    }
    
    public void preloadPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (cache.containsKey(index)) return;
        if (loadingPages.contains(index)) return;
        
        loadingPages.add(index);
        preloader.submit(() -> {
            try {
                Image img = new Image(chapter.getPage(index), true);
                if (img.isBackgroundLoading()) {
                    while (img.getProgress() < 1.0 && !img.isError()) {
                        Thread.sleep(10);
                    }
                }
                if (!img.isError()) {
                    cache.put(index, img);
                    Logger.info("Preloaded page: " + index);
                }
            } catch (Exception e) {
                Logger.error("Error precargando página " + index);
            } finally {
                loadingPages.remove(index);
            }
        });
    }
    
    public void preloadAroundCurrent() {
        preloadPage(currentPageIndex + 1);
        preloadPage(currentPageIndex + 2);
        preloadPage(currentPageIndex + 3);
        preloadPage(currentPageIndex - 1);
        preloadPage(currentPageIndex - 2);
    }
    
    public void resetZoom() {
        if (view.getVisor() != null) {
            // 1.0 significa 100% (tamaño normal sin zoom)
            view.getVisor().setScaleX(1.0);
            view.getVisor().setScaleY(1.0);
        }
        inZoom = false;
    }
    
    public void setInZoom(boolean zoom) {
        this.inZoom = zoom;
    }
    
    public boolean isInZoom() {
        return inZoom;
    }
    
    public boolean isMenuVisible() {
        return menuVisible;
    }
    
    public void hideMenu() {
        menuVisible = false;
        view.setMenuVisible(false);
    }
    
    public void showMenu() {
        if (inZoom) return;
        menuVisible = true;
        view.setMenuVisible(true);
    }
    
    public boolean isUpdatingUI() {
        return updatingUI;
    }
    
    public void cleanupResources() {
        cache.clear();
        preloader.shutdownNow();
    }
    
    public void saveAndCleanup() {
        cache.clear();
        preloader.shutdownNow();
        lib.saveLibrary();
    }
    
    public Manga getManga() {
        return manga;
    }
    
    public Chapter getChapter() {
        return chapter;
    }
    
    public int getChapterIndex() {
        return chapterIndex;
    }
    
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }
}
