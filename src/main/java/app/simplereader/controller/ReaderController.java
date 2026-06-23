package app.simplereader.controller;

import app.simplereader.service.Logger;
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
    // 1. Quitamos 'final' para poder destruirlo en el cleanup
    private ScnReader view;
    
    // 2. Agregamos flag volátil para que los hilos sepan cuándo detenerse
    private volatile boolean disposed = false; 
    
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
    private boolean startAtLastPage = false;
    
    // Se eliminó el LinkedHashMap local, ahora usamos la clase compartida app.simplereader.service.Cache
    
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
        disposed = false; // Reset the disposed flag so it can be reused
        
        // cache.clear(); ya no es necesario, el caché compartido se encarga
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
        app.simplereader.service.Cache.getInstance().getPagesLRU().remove(chapter.getPage(currentPageIndex));
        loadingPages.remove(currentPageIndex);
        view.setImageViewImage(null);
        loadCurrentImage();
    }
    
    public void loadCurrentImage() {
        Image cached = app.simplereader.service.Cache.getInstance().getPagesLRU().get(chapter.getPage(currentPageIndex));
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
        
        preloadAroundCurrent();
        
        Chapter currentTaskChapter = this.chapter; // Guardar el capítulo actual para validar después
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null; // Abortar si ya se cerró o se cambió de cap
            String url = currentTaskChapter.getPage(index);
            return loadWebpOrNative(url, false);
        }, preloader).thenAccept(img -> {
            if (disposed || this.chapter != currentTaskChapter) return; // Abortar si cambió de capítulo
            
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
                    Logger.info("Page " + index + " loaded (async).");
                } else {
                    Logger.error("Error cargando página " + index);
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
    
    private Image loadWebpOrNative(String url, boolean background) {
        if (url.startsWith("http")) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000); // 5 segundos de límite
                conn.setReadTimeout(10000);   // 10 segundos de límite
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                
                // Si la conexión manual arroja código de error, lanzamos excepción para ir al fallback
                if (conn.getResponseCode() >= 400) {
                    throw new java.io.IOException("HTTP " + conn.getResponseCode());
                }
                
                try (InputStream in = conn.getInputStream()) {
                    if (url.toLowerCase().contains(".webp")) {
                        BufferedImage bimg = ImageIO.read(in);
                        if (bimg != null) return SwingFXUtils.toFXImage(bimg, null);
                    } else {
                        return new Image(in);
                    }
                }
            } catch (Exception e) {
                
                return new Image(url, background);
            }
            return null;
        } else {
            if (url.toLowerCase().contains(".webp")) {
                try {
                    // Si la URL empieza con file:, la leemos como URL, sino como File directo
                    BufferedImage bimg;
                    if (url.startsWith("file:")) {
                        bimg = ImageIO.read(new java.net.URL(url));
                    } else {
                        bimg = ImageIO.read(new File(url));
                    }
                    
                    if (bimg != null) return SwingFXUtils.toFXImage(bimg, null);
                } catch (Exception e) {
                    Logger.error("Error decodificando webp local (" + e.getClass().getSimpleName() + "). Usando fallback nativo.");
                }
                // Si falla o no se pudo cargar con ImageIO, hacemos el fallback a JavaFX
                return new Image(url, background);
            } else {
                return new Image(url, background);
            }
        }
    }
    
    public Image getPage(int index) {
        String url = chapter.getPage(index);
        return app.simplereader.service.Cache.getInstance().getPagesLRU().computeIfAbsent(url, k -> loadWebpOrNative(k, true));
    }
    
    public void preloadPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (app.simplereader.service.Cache.getInstance().getPagesLRU().get(chapter.getPage(index)) != null) return;
        if (!loadingPages.add(index)) return;
        
        Chapter currentTaskChapter = this.chapter;
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null;
            if (Math.abs(index - currentPageIndex) > 3) return null; // Cancela precargas si saltaste la página muy rápido
            
            String url = currentTaskChapter.getPage(index);
            return loadWebpOrNative(url, false);
        }, preloader).thenAccept(img -> {
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
        preloadPage(currentPageIndex + 1);
        preloadPage(currentPageIndex + 2);
        preloadPage(currentPageIndex - 1);
        preloadPage(currentPageIndex - 2);
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
        lib.saveLibrary();
    }
    
    public void saveAndCleanup() {
        disposed = true;
        
        // cache.clear(); Ya no es necesario con caché global
        loadingPages.clear();
        
        lib.saveLibrary();
        
        // Al ser Singleton, ya no destruimos el preloader ni los lazos con la UI
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