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
    
    private final java.util.Map<Integer, Image> cache = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<Integer, Image>(5, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Image> eldest) {
                return size() > 5;
            }
        }
    );
    
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
        
        cache.clear();
        loadingPages.clear();
        view.setImageViewImage(null); 
        
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
    
    public void changeToChapter(Chapter next, int nextIndex) {
        if (next.hasPages()) {
            programmaticNav = true;
            loadChapter(next, nextIndex);
            if (chapterComboBox != null) {
                chapterComboBox.setValue(next);
            }
            programmaticNav = false;
            return;
        }
        
        MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        if (src == null) return;
        
        preloader.submit(() -> {
            if (disposed) return;
            try {
                List<String> pages = src.getPages(manga.getMangaID(), next.getChapterID());
                next.setPages(pages);
                if (disposed) return;
                
                if (next.hasPages()) {
                    Platform.runLater(() -> {
                        if (disposed) return;
                        programmaticNav = true;
                        loadChapter(next, nextIndex);
                        if (chapterComboBox != null) {
                            chapterComboBox.setValue(next);
                        }
                        programmaticNav = false;
                    });
                } else {
                    Platform.runLater(() -> {
                        if (!disposed) Logger.noPagesAlert(next.getTitle());
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!disposed) Logger.error("Error cargando capítulo: " + e.getMessage());
                });
            }
        });
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
            chapter.markAsReaded();
            
            if (next.hasPages()) {
                programmaticNav = true;
                loadChapter(next, nextIndex);
                if (chapterComboBox != null) {
                    chapterComboBox.setValue(next);
                }
                programmaticNav = false;
                return;
            }
            
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src == null) return;
            
            preloader.submit(() -> {
                if (disposed) return; // Validación temprana
                
                try {
                    List<String> pages = src.getPages(manga.getMangaID(), next.getChapterID());
                    next.setPages(pages);
                    
                    if (disposed) return; // Validación después de red
                    
                    if (next.hasPages()) {
                        Platform.runLater(() -> {
                            if (disposed) return; // Evitar actualizar UI si ya se cerró
                            
                            programmaticNav = true;
                            loadChapter(next, nextIndex);
                            if (chapterComboBox != null) {
                                chapterComboBox.setValue(next);
                            }
                            programmaticNav = false;
                        });
                    } else {
                        Platform.runLater(() -> {
                            if (!disposed) Logger.noPagesAlert(next.getTitle());
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (!disposed) Logger.error("Error cargando capítulo: " + e.getMessage());
                    });
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
            
            if (prev.hasPages()) {
                programmaticNav = true;
                loadChapter(prev, prevIndex);
                if (chapterComboBox != null) {
                    chapterComboBox.setValue(prev);
                }
                goToPage(prev.getPageCount() - 1);
                programmaticNav = false;
                return;
            }
            
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src == null) return;
            
            preloader.submit(() -> {
                if (disposed) return; // Validación
                
                try {
                    List<String> pages = src.getPages(manga.getMangaID(), prev.getChapterID());
                    prev.setPages(pages);
                    
                    if (disposed) return; 
                    
                    if (prev.hasPages()) {
                        Platform.runLater(() -> {
                            if (disposed) return;
                            
                            programmaticNav = true;
                            loadChapter(prev, prevIndex);
                            if (chapterComboBox != null) {
                                chapterComboBox.setValue(prev);
                            }
                            goToPage(prev.getPageCount() - 1);
                            programmaticNav = false;
                        });
                    } else {
                        Platform.runLater(() -> {
                            if (!disposed) Logger.noPagesAlert(prev.getTitle());
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (!disposed) Logger.error("Error cargando capítulo anterior: " + e.getMessage());
                    });
                }
            });
        }  
    }
    
    public void loadCurrentImage() {
        Image cached = cache.get(currentPageIndex);
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
                    cache.put(index, img);
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
                if (!disposed) Logger.error("Error cargando página " + index + ": " + e.getMessage());
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
                    conn.setConnectTimeout(5000); // 5 segundos de límite para conectar
                    conn.setReadTimeout(10000);   // 10 segundos de límite para descargar
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    try (InputStream in = conn.getInputStream()) {
                        bimg = ImageIO.read(in);
                    }
                } else {
                    bimg = ImageIO.read(new File(url));
                }
                
                if (bimg != null) {
                    return SwingFXUtils.toFXImage(bimg, null);
                }
            } catch (Exception e) {
                Logger.error("Error decodificando webp: " + e.getMessage());
            }
        }
        return new Image(url, background);
    }
    
    public Image getPage(int index) {
        return cache.computeIfAbsent(index, k -> {
            String url = chapter.getPage(k);
            return loadWebpOrNative(url, true);
        });
    }
    
    public void preloadPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (cache.get(index) != null) return;
        if (!loadingPages.add(index)) return;
        
        Chapter currentTaskChapter = this.chapter;
        
        CompletableFuture.supplyAsync(() -> {
            if (disposed || this.chapter != currentTaskChapter) return null;
            String url = currentTaskChapter.getPage(index);
            return loadWebpOrNative(url, false);
        }, preloader).thenAccept(img -> {
            if (disposed || this.chapter != currentTaskChapter) return;
            
            if (img != null && !img.isError()) {
                cache.put(index, img);
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
        if (inZoom) return;
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
        disposed = true; // 3. Se levanta la bandera para detener peticiones
        
        cache.clear();
        loadingPages.clear();
        
        // Al ser Singleton, ya no destruimos el preloader ni los lazos con la UI
    }
    
    public void saveAndCleanup() {
        disposed = true;
        
        cache.clear();
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