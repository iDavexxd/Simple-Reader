package app.simplereader.controller;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 *
 * @author david
 */
public class ReaderController {
    private final ScnReader view;
    
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
    private boolean programmaticNav = false;
    
    private final Cache<Integer, Image> cache = Caffeine.newBuilder()
        .maximumSize(20)
        .build();
    
    private final Set<Integer> loadingPages = ConcurrentHashMap.newKeySet();
    private final ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
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
        cache.invalidateAll();
        loadingPages.clear();
        view.setImageViewImage(null); // limpia imagen del capítulo anterior
        
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
            view.setImageViewImage(null);
            goToPage(currentPageIndex + 1);
        } else {
            nextChapter();
        }   
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
            int nextIndex = chapterIndex + 1;
            Chapter next = chapters.get(nextIndex);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src == null) return;
            
            chapter.markAsReaded();
            
            preloader.submit(() -> {
                try {
                    List<String> pages = src.getPages(manga.getMangaID(), next.getChapterID());
                    next.setPages(pages);
                    
                    if (next.hasPages()) {
                        Platform.runLater(() -> {
                            programmaticNav = true;
                            loadChapter(next, nextIndex);
                            if (chapterComboBox != null) {
                                chapterComboBox.setValue(next);
                            }
                            programmaticNav = false;
                        });
                    } else {
                        Platform.runLater(() -> Logger.noPagesAlert(next.getTitle()));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> Logger.error("Error cargando capítulo: " + e.getMessage()));
                }
            });
        }
    }
    
    public void previousChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex > 0) {
            int prevIndex = chapterIndex - 1;
            Chapter prev = chapters.get(prevIndex);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src == null) return;
            
            preloader.submit(() -> {
                try {
                    List<String> pages = src.getPages(manga.getMangaID(), prev.getChapterID());
                    prev.setPages(pages);
                    
                    if (prev.hasPages()) {
                        Platform.runLater(() -> {
                            programmaticNav = true;
                            loadChapter(prev, prevIndex);
                            if (chapterComboBox != null) {
                                chapterComboBox.setValue(prev);
                            }
                            goToPage(prev.getPageCount() - 1);
                            programmaticNav = false;
                        });
                    } else {
                        Platform.runLater(() -> Logger.noPagesAlert(prev.getTitle()));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> Logger.error("Error cargando capítulo anterior: " + e.getMessage()));
                }
            });
        }  
    }
    
    public void loadCurrentImage() {
        Image cached = cache.getIfPresent(currentPageIndex);
        if (cached != null) {
            view.setImageViewImage(cached);
            view.fitImageToScreen();
            resetZoom();
            preloadAroundCurrent();
            Logger.info("Page " + currentPageIndex + " loaded (cache).");
            return;
        }
        
        // Cache miss → cargar en background para no congelar la UI
        int index = currentPageIndex; // capturar el índice actual (puede cambiar si el usuario navega)
        if (!loadingPages.add(index)) return;
        
        // Empezamos precarga de alrededores ya (en paralelo con la página actual)
        preloadAroundCurrent();
        
        CompletableFuture.supplyAsync(() -> {
            String url = chapter.getPage(index);
            return loadWebpOrNative(url, false);
        }, preloader).thenAccept(img -> {
            Platform.runLater(() -> {
                loadingPages.remove(index);
                if (img != null && !img.isError()) {
                    cache.put(index, img);
                    // Solo actualizar la UI si el usuario sigue viendo esta página
                    if (index == currentPageIndex) {
                        view.setImageViewImage(img);
                        view.fitImageToScreen();
                        resetZoom();
                    }
                    Logger.info("Page " + index + " loaded (async).");
                } else {
                    Logger.error("Error cargando página " + index);
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                loadingPages.remove(index);
                Logger.error("Error cargando página " + index + ": " + e.getMessage());
            });
            return null;
        });
    }
    
    private Image loadWebpOrNative(String url, boolean background) {
        if (url.toLowerCase().contains(".webp")) {
            try {
                BufferedImage bimg;
                if (url.startsWith("http")) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    // Engañamos al servidor para que crea que somos un navegador
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    try (InputStream in = conn.getInputStream()) {
                        bimg = ImageIO.read(in);
                    }
                } else {
                    // Por si tu MangaSource guarda los archivos localmente primero (CBZ/ZIP)
                    bimg = ImageIO.read(new File(url));
                }
                
                if (bimg != null) {
                    // toFXImage es síncrono y devuelve la imagen lista para tu ImageView.
                    return SwingFXUtils.toFXImage(bimg, null);
                }
            } catch (Exception e) {
                Logger.error("Error decodificando webp: " + e.getMessage());
            }
        }
        // Fallback al comportamiento nativo de JavaFX para JPG/PNG
        return new Image(url, background);
    }
    
    public Image getPage(int index) {
        return cache.get(index, k -> {
            String url = chapter.getPage(k);
            return loadWebpOrNative(url, true);
        });
    }
    
    public void preloadPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (cache.getIfPresent(index) != null) return;
        if (!loadingPages.add(index)) return;
        
        CompletableFuture.supplyAsync(() -> {
            String url = chapter.getPage(index);
            return loadWebpOrNative(url, false);
        }, preloader).thenAccept(img -> {
            if (!img.isError()) {
                cache.put(index, img);
                Logger.info("Preloaded page: " + index);
            }
            loadingPages.remove(index);
            if (index == currentPageIndex && img != null && !img.isError()) {
                Platform.runLater(() -> {
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
        preloadPage(currentPageIndex + 1);
        preloadPage(currentPageIndex + 2);
        preloadPage(currentPageIndex + 3);
        preloadPage(currentPageIndex - 1);
        preloadPage(currentPageIndex - 2);
        preloadPage(currentPageIndex - 3);
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
    
    public boolean isProgrammaticNav() {
        return programmaticNav;
    }
    
    public void cleanupResources() {
        cache.invalidateAll();
        preloader.shutdownNow();
    }
    
    public void saveAndCleanup() {
        cache.invalidateAll();
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
