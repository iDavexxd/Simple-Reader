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
        
        // Check if the source requires custom headers for image loading
        boolean isWebp = url.toLowerCase().contains(".webp");
        java.util.Map<String, String> sourceHeaders = null;
        app.simplereader.repository.MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        if (src != null) {
            sourceHeaders = src.getImageHeaders();
        }
        boolean needsHeaders = sourceHeaders != null && !sourceHeaders.isEmpty();
        
        if (!isWebp && url.startsWith("http") && !needsHeaders) {
            // Use JavaFX native background loading with built-in progress tracking
            Image img;
            if (app.simplereader.model.AppConfig.get().limitPageQuality) {
                img = new Image(url, 0, 720, true, true, true);
            } else {
                img = new Image(url, true);
            }
            
            img.progressProperty().addListener((obs, old, val) -> {
                if (disposed || this.chapter != currentTaskChapter) return;
                if (index == currentPageIndex) {
                    view.setLoadingProgress(val.doubleValue());
                }
            });
            
            img.errorProperty().addListener((obs, old, isError) -> {
                if (!isError || disposed || this.chapter != currentTaskChapter) return;
                loadingPages.remove(index);
                String errorMsg = img.getException() != null ? img.getException().getMessage() : "desconocido";
                Logger.error("Error cargando página " + index + ": " + errorMsg);
                if (index == currentPageIndex) {
                    view.showErrorOverlay();
                }
            });
            
            img.progressProperty().addListener((obs, old, val) -> {
                if (val.doubleValue() >= 1.0 && !img.isError()) {
                    if (disposed || this.chapter != currentTaskChapter) return;
                    loadingPages.remove(index);
                    app.simplereader.service.Cache.getInstance().getPagesLRU().put(currentTaskChapter.getPage(index), img);
                    if (index == currentPageIndex) {
                        view.setImageViewImage(img);
                        view.fitImageToScreen();
                        resetZoom();
                    }
                    Logger.info("Page " + index + " loaded (native bg).");
                }
            });
        } else {
            // Webp, local files, or sources with custom headers: use CompletableFuture with indeterminate progress
            view.setLoadingProgress(-1);
            
            CompletableFuture.supplyAsync(() -> {
                if (disposed || this.chapter != currentTaskChapter) return null;
                return loadWebpOrNative(url, false, index);
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
                        Logger.info("Page " + index + " loaded (async).");
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
    }
    
    private Image loadWebpOrNative(String url, boolean background, int index) {
        if (url.startsWith("http")) {
            try {
                app.simplereader.repository.MangaSource src = manga != null ? app.simplereader.controller.SourceManager.getInstance().getSource(manga.getSourceID()) : null;
                HttpURLConnection conn = (HttpURLConnection) app.simplereader.service.Http.getConnection(url, src);
                
                // Si la conexión manual arroja código de error, lanzamos excepción para ir al fallback
                if (conn.getResponseCode() >= 400) {
                    throw new java.io.IOException("HTTP " + conn.getResponseCode());
                }
                
                long totalBytes = conn.getContentLengthLong();
                try (InputStream in = conn.getInputStream();
                     java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
                     
                    byte[] chunk = new byte[8192];
                    long bytesRead = 0;
                    int n;
                    long lastUpdate = 0;
                    
                    while ((n = in.read(chunk)) != -1) {
                        buffer.write(chunk, 0, n);
                        bytesRead += n;
                        
                        if (totalBytes > 0 && index == currentPageIndex) {
                            long now = System.currentTimeMillis();
                            if (now - lastUpdate > 50) { // Max 20fps update to avoid UI lag
                                lastUpdate = now;
                                double progress = (double) bytesRead / totalBytes;
                                Platform.runLater(() -> {
                                    if (!disposed && index == currentPageIndex) {
                                        view.setLoadingProgress(progress);
                                    }
                                });
                            }
                        }
                    }
                    
                    // Finalizamos asegurándonos de que la barra llegue al 100% si es la actual
                    if (totalBytes > 0 && index == currentPageIndex) {
                        Platform.runLater(() -> {
                            if (!disposed && index == currentPageIndex) {
                                view.setLoadingProgress(1.0);
                            }
                        });
                    }
                    
                    try (java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(buffer.toByteArray())) {
                        if (url.toLowerCase().contains(".webp")) {
                            BufferedImage bimg = ImageIO.read(bin);
                            if (bimg != null) {
                                if (app.simplereader.model.AppConfig.get().limitPageQuality && bimg.getHeight() > 1080) {
                                    int newHeight = 1080;
                                    int newWidth = (int) (bimg.getWidth() * (1080.0 / bimg.getHeight()));
                                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                                    java.awt.Graphics2D g2d = scaled.createGraphics();
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                                    g2d.dispose();
                                    bimg.flush();
                                    bimg = scaled;
                                }
                                Image fxImg = SwingFXUtils.toFXImage(bimg, null);
                                bimg.flush();
                                return fxImg;
                            }
                        } else {
                            if (app.simplereader.model.AppConfig.get().limitPageQuality) {
                                return new Image(bin, 0, 1080, true, true);
                            }
                            return new Image(bin);
                        }
                    }
                }
            } catch (Exception e) {
                
                if (!url.toLowerCase().contains(".webp") && app.simplereader.model.AppConfig.get().limitPageQuality) {
                    return new Image(url, 0, 720, true, true, background);
                }
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
                    
                    if (bimg != null) {
                        if (app.simplereader.model.AppConfig.get().limitPageQuality && bimg.getHeight() > 1080) {
                            int newHeight = 1080;
                            int newWidth = (int) (bimg.getWidth() * (1080.0 / bimg.getHeight()));
                            java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics2D g2d = scaled.createGraphics();
                            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                            g2d.dispose();
                            bimg.flush();
                            bimg = scaled;
                        }
                        Image fxImg = SwingFXUtils.toFXImage(bimg, null);
                        bimg.flush();
                        return fxImg;
                    }
                } catch (Exception e) {
                    Logger.error("Error decodificando webp local (" + e.getClass().getSimpleName() + "). Usando fallback nativo.");
                }
                // Si falla o no se pudo cargar con ImageIO, hacemos el fallback a JavaFX
                return new Image(url, background);
            } else {
                if (app.simplereader.model.AppConfig.get().limitPageQuality) {
                    return new Image(url, 0, 720, true, true, background);
                }
                return new Image(url, background);
            }
        }
    }
    
    public Image getPage(int index) {
        String url = chapter.getPage(index);
        return app.simplereader.service.Cache.getInstance().getPagesLRU().get(url, k -> loadWebpOrNative(k, true, index));
    }
    
    public void preloadPage(int index, ExecutorService executor) {
        if (index < 0 || index >= totalPages()) return;
        if (app.simplereader.service.Cache.getInstance().getPagesLRU().getIfPresent(chapter.getPage(index)) != null) return;
        if (!loadingPages.add(index)) return;
        
        Chapter currentTaskChapter = this.chapter;
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null;
            if (Math.abs(index - currentPageIndex) > 3) return null; // Cancela precargas si saltaste la página muy rápido
            
            String url = currentTaskChapter.getPage(index);
            return loadWebpOrNative(url, false, index);
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