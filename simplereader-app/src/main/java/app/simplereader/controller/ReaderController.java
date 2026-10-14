package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.service.Cache;
import app.simplereader.service.ImageManager;
import app.simplereader.views.ScnReader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 *
 * @author david
 */
public class ReaderController {
    // 1. Quitamos 'final' para poder destruirlo en el cleanup
    private ScnReader view;
    
    // 2. Agregamos flag volátil para que los hilos sepan cuándo detenerse
    private volatile boolean disposed = false; 
    
    private static ReaderController instance;
    private SceneController nav = SceneController.getInstance();
    private final LibraryController lib = LibraryController.getInstance();
    private final ImageManager imgManager = ImageManager.getInstance();
    private final Cache cache = Cache.getInstance();
    
    private Manga manga;
    private Chapter chapter;
    private int chapterIndex;
    private int currentPageIndex;
    
    private boolean menuVisible = true;
    private boolean inZoom = false;
    private boolean updatingUI = false;
    private boolean programmaticNav = false;
    private boolean startAtLastPage = false;
    
    
    
    private final Set<Integer> loadingPages = ConcurrentHashMap.newKeySet();
    
    // Ejecutor para la página actual (prioridad)
    private final ExecutorService currentLoader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "current-loader");
        t.setDaemon(true);
        return t;
    });

    // Ejecutor para precargar hacia adelante (máx 1 a la vez para ahorrar RAM)
    private final ExecutorService preloaderForward = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "preloader-fw");
        t.setDaemon(true);
        return t;
    });
    
    // Ejecutor para precargar hacia atrás
    private final ExecutorService preloaderBackward = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "preloader-bw");
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
        disposed = false; // Reset the disposed flag so it can be reused
        
        // Limpiar páginas del capítulo anterior para liberar RAM inmediatamente
        app.simplereader.service.Cache.getInstance().getPagesLRU().invalidateAll();
        loadingPages.clear();
        view.setImageViewImage(null); 
        
        this.chapter = chapter;
        this.chapterIndex = index;
        
        if (startAtLastPage) {
            this.currentPageIndex = Math.max(0, chapter.getPageCount() - 1);
            startAtLastPage = false;
        } else {
            this.currentPageIndex = chapter.isReaded() ? 0 : chapter.getLastRead();
        }
        
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
        
        if (chapterComboBox != null) {
            updatingUI = true;
            chapterComboBox.getItems().setAll(manga.getChapters());
            chapterComboBox.setValue(chapter);
            updatingUI = false;
        }
        
        if (chapterNameLabel != null) {
            chapterNameLabel.setText(chapter.getTitle());
        }
        
        if (totalPages() > 0) {
            if (paginaComboBox != null) paginaComboBox.setDisable(totalPages() <= 1);
            loadCurrentImage();
        } else {
            if (paginaComboBox != null) paginaComboBox.setDisable(true);
            view.showNoPagesOverlay();
        }
        
        view.doUpdatePageLabel();
    }
    
    public int totalPages() {
        return chapter != null ? chapter.getPageCount() : 0;
    }
    
    public void nextPage() {
        if (currentPageIndex < totalPages() - 1) {
            view.setImageViewImage(null);
            goToPage(currentPageIndex + 1);
        } else {
            nextChapter();
        }   
    }
    
    public void changeToChapter(Chapter next, int nextIndex) {
        app.simplereader.controller.MangaMenuController.getInstance().openChapter(next);
    }
    
    public void previousPage() {
        if (currentPageIndex > 0){
            view.setImageViewImage(null);
            goToPage(currentPageIndex - 1);
        } else {
            previousChapter();
        }   
    }
    
    public void goToPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        
        if (index == totalPages() - 1){
            chapter.markAsReaded();
        }
        
        currentPageIndex = index;
        if (!chapter.isReaded()) {
            chapter.setLastRead(currentPageIndex);
        }
        
        loadCurrentImage();
        if (paginaComboBox != null) {
            updatingUI = true;
            paginaComboBox.setValue(currentPageIndex + 1);
            view.doUpdatePageLabel();
            updatingUI = false;
        }
    }
    
    public void nextChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex < chapters.size() - 1) {
            int nextIndex = chapterIndex + 1;
            Chapter next = chapters.get(nextIndex);
            chapter.markAsReaded();
            
            app.simplereader.controller.MangaMenuController.getInstance().openChapter(next);
        }
    }
     public void previousChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex > 0) {
            int prevIndex = chapterIndex - 1;
            Chapter prev = chapters.get(prevIndex);
            
            startAtLastPage = true;
            app.simplereader.controller.MangaMenuController.getInstance().openChapter(prev);
        }
    }
    
    public void reloadCurrentImage() {
        app.simplereader.service.Cache.getInstance().getPagesLRU().invalidate(chapter.getPage(currentPageIndex));
        loadingPages.remove(currentPageIndex);
        view.setImageViewImage(null);
        loadCurrentImage();
    }
    
    
    public void loadCurrentImage() {
        Image cached = app.simplereader.service.Cache.getInstance().getPagesLRU().getIfPresent(chapter.getPage(currentPageIndex));
        if (cached != null) {
            view.setImageViewImage(cached);
            view.fitImageToScreen();
            resetZoom();
            preloadAroundCurrent();
            Logger.info("Page " + currentPageIndex + " loaded (cache).");
            return;
        }
        
        int index = currentPageIndex; 
        if (!loadingPages.add(index)) return;
        
        view.setLoadingProgress(0);
        preloadAroundCurrent();
        
        String url = chapter.getPage(index);
        Chapter currentTaskChapter = this.chapter;
        MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        
        java.util.function.Consumer<Double> progressCallback = progress -> {
            if (!disposed && this.chapter == currentTaskChapter && index == currentPageIndex) {
                view.setLoadingProgress(progress);
            }
        };
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null;
            return app.simplereader.service.ImageManager.getInstance().loadPage(url, src, index, progressCallback);
        }, currentLoader).thenAccept(img -> {
            if (disposed || this.chapter != currentTaskChapter) return;
            
            Platform.runLater(() -> {
                if (disposed || this.chapter != currentTaskChapter) return; 
                
                loadingPages.remove(index);
                if (img != null && !img.isError()) {
                    app.simplereader.service.Cache.getInstance().getPagesLRU().put(currentTaskChapter.getPage(index), img);
                    if (index == currentPageIndex) {
                        view.setImageViewImage(img);
                        view.fitImageToScreen();
                        resetZoom();
                    }
                    Logger.info("Page " + index + " loaded.");
                } else {
                    String errMsg = (img != null && img.getException() != null) ? img.getException().getMessage() : "Desconocido";
                    Logger.error("Error cargando página " + index + ": " + errMsg);
                    if (index == currentPageIndex) {
                        view.showErrorOverlay();
                    }
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                loadingPages.remove(index);
                if (!disposed) {
                    Logger.error("Error cargando página " + index + ": " + e.getMessage());
                    if (index == currentPageIndex) {
                        view.showErrorOverlay();
                    }
                }
            });
            return null;
        });
    }
    
    public void preloadPage(int index, ExecutorService executor) {
        if (index < 0 || index >= totalPages()) return;
        if (app.simplereader.service.Cache.getInstance().getPagesLRU().getIfPresent(chapter.getPage(index)) != null) return;
        if (!loadingPages.add(index)) return;
        
        Chapter currentTaskChapter = this.chapter;
        MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null;
            if (Math.abs(index - currentPageIndex) > 3) return null; // Cancela precargas si saltaste la página muy rápido
            
            String url = currentTaskChapter.getPage(index);
            return app.simplereader.service.ImageManager.getInstance().loadPage(url, src, index, null);
        }, executor).thenAccept(img -> {
            if (disposed || this.chapter != currentTaskChapter) return;
            
            if (img != null && !img.isError()) {
                app.simplereader.service.Cache.getInstance().getPagesLRU().put(currentTaskChapter.getPage(index), img);
                Logger.info("Preloaded page: " + index);
            }
            loadingPages.remove(index);
            
            if (index == currentPageIndex && img != null && !img.isError()) {
                Platform.runLater(() -> {
                    if (disposed) return;
                    view.setImageViewImage(img);
                    view.fitImageToScreen();
                    resetZoom();
                });
            }
        }).exceptionally(e -> {
            Logger.error("Error precargando página " + index);
            loadingPages.remove(index);
            return null;
        });
    }
    
    public void preloadAroundCurrent() {
        // Hacia adelante (usando su propio hilo)
        preloadPage(currentPageIndex + 1, preloaderForward);
        preloadPage(currentPageIndex + 2, preloaderForward);
        preloadPage(currentPageIndex + 3, preloaderForward);
        
        // Hacia atrás (usando su propio hilo)
        preloadPage(currentPageIndex - 1, preloaderBackward);
        preloadPage(currentPageIndex - 2, preloaderBackward);
    }
    
    public void resetZoom() {
        if (view != null && view.getVisor() != null) {
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
        if (view != null) view.setMenuVisible(false);
    }
    
    public void showMenu() {
        menuVisible = true;
        if (view != null) view.setMenuVisible(true);
    }
    
    public boolean isUpdatingUI() {
        return updatingUI;
    }
    
    public boolean isProgrammaticNav() {
        return programmaticNav;
    }
    
    public void cleanupResources() {
        disposed = true; 
        loadingPages.clear();
        app.simplereader.service.Cache.getInstance().getPagesLRU().invalidateAll();
        if (view != null) view.setImageViewImage(null);
        lib.saveLibrary();
        
        // Sugerir fuertemente al GC que limpie la memoria off-heap de las imágenes de JavaFX
        System.gc();
    }
    
    public void saveAndCleanup() {
        disposed = true;
        
        loadingPages.clear();
        app.simplereader.service.Cache.getInstance().getPagesLRU().invalidateAll();
        if (view != null) view.setImageViewImage(null);
        
        lib.saveLibrary();
        
        // Al ser Singleton, ya no destruimos el preloader ni los lazos con la UI
        System.gc();
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