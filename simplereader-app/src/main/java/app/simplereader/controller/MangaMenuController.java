package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.service.Downloader;
import app.simplereader.views.ScnMangaMenu;
import app.simplereader.views.ScnReader;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;

/**
 *
 * @author david
 */
public class MangaMenuController {
    
    private final String DOWNLOADS_FOLDER = AppConfig.DATA_FOLDER;
    
    // 1. Quitamos 'final' para poder destruirlo en el cleanup
    private ScnMangaMenu view;
    
    // 2. Bandera para cancelar tareas en vuelo si el usuario sale rápido del menú
    private volatile boolean disposed = false;
    
    private static MangaMenuController instance;
    private final SceneController nav = SceneController.getInstance();
    private final LibraryController library = LibraryController.getInstance();
    private final MainMenuController mainMenuController = MainMenuController.getInstance();
    
    private final SourceManager source = SourceManager.getInstance();
    
    private Manga manga;
    private boolean isReversed = false;
    
    private final ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "preloader-mangamenu");
        t.setDaemon(true);
        return t;
    });
    
    public MangaMenuController(ScnMangaMenu view){
        this.view = view;
    }
    
    public static void doInstance(ScnMangaMenu view){
        // 3. Limpiamos la instancia vieja antes de crear la nueva
        if (instance != null) {
            instance.cleanup();
        }
        instance = new MangaMenuController(view);
    }
    
    public static MangaMenuController getInstance(){
        return instance;
    }
    
    public void init(Manga manga){
        this.manga = manga;
    }
    
    public void addToLibrary(String category) {
        SourceManager.getInstance().saveManga(this.manga);
        library.addManga(this.manga, category);
        library.saveLibrary();
        mainMenuController.reloadMangas();
    }
    
    public void removeManga(Manga manga, Category category){
        library.removeMangaFrom(category, manga);
        library.saveLibrary();
        mainMenuController.reloadMangas();
    }
    
    public void doReloadCategoryButtons(){
        if (!disposed && view != null) {
            view.doCategoryButtons();
        }
    }
    
    public void openChapter(Chapter chapter){
        app.simplereader.views.ScnLoading.getInstance().setAborted(false);
        Platform.runLater(() -> nav.goTo(app.simplereader.views.ScnLoading.getInstance()));
        
        if (chapter.hasPages()) {
            Platform.runLater(() -> {
                if (!app.simplereader.views.ScnLoading.getInstance().isAborted()) navigateToReader(chapter);
            });
            return;
        }
        Logger.info("Cargando páginas para: " + chapter.getTitle());
        
        File pagesFolder = new File(DOWNLOADS_FOLDER + manga.getSourceID() + "/downloads/" + manga.getMangaID() + "/" + chapter.getChapterID());
        if (pagesFolder.exists() && chapter.isDownloaded()) {
            File[] files = pagesFolder.listFiles();
            if (files != null && files.length > 0) {
                List<String> localPages = new ArrayList<>();
                
                java.util.Arrays.sort(files);
                
                for (File file : files) {
                    localPages.add(file.toURI().toString()); 
                }
                
                chapter.setPages(localPages);
                Platform.runLater(() -> {
                    if (!app.simplereader.views.ScnLoading.getInstance().isAborted()) navigateToReader(chapter);
                });
                return; 
            }
        }
        
        MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        if (src != null) {
            preloader.submit(() -> {
                if (disposed) return; // Abortar si el menú ya se cerró
                try{
                    List<String> pages = src.getPages(manga.getMangaID(), chapter.getChapterID());
                    if (disposed) return; 
                    
                    chapter.setPages(pages);
                    Platform.runLater(() -> {
                        if (!disposed && !app.simplereader.views.ScnLoading.getInstance().isAborted()) {
                            navigateToReader(chapter);
                        }
                    });
                } catch (Exception e){
                    Platform.runLater(() -> {
                        if (!disposed) {
                            Logger.error("Error loading chapter("+e.getClass().getSimpleName()+")"+chapter.getChapterID());
                            nav.backScene();
                        }
                    });
                }
            });
        }
        
    }

    private void navigateToReader(Chapter chapter){
        // Las covers ya no se necesitan → liberar memoria
        app.simplereader.service.Cache.getInstance().getSharedCache().invalidateAll();
        
        List<Chapter> chapters = manga.getChapters();
        int index = chapters != null ? chapters.indexOf(chapter) : -1;
        Logger.info("Selected: " + chapter.getTitle());
        ScnReader reader = ScnReader.getInstance();
        reader.updateReader(manga, chapter, index);
        nav.goTo(reader);
    }
    
    public Chapter findFirstUnreadChapter(){
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return null;
        
        return chapters.stream()
                .filter(c -> !c.isReaded())
                .findFirst()
                .orElse(null);
    }
    
    public List<Chapter> getChapters(){
        List<Chapter> chapters = manga.getChapters();
        return chapters != null ? chapters : List.of();
    }
    
    public String getTags(){
        if (manga.getTags() == null || manga.getTags().isEmpty()) return "";
        return String.join(", ", manga.getTags());
    }
    
    public boolean isReversed(){
        return isReversed;
    }
    
    public void toggleChapterOrder(){
        isReversed = !isReversed;
    }
    
    public Manga getManga(){
        return manga;
    }
    
    public void reloadManga(){
        preloader.submit(() -> {
            if (disposed) return;
            view.doShowLoadingPane();
            try {
                String remoteCover = source.getSource(manga.getSourceID()).getCoverURL(manga.getMangaID());
                if (disposed) return;
                
                if (remoteCover != null && remoteCover.startsWith("http")) {
                    String currentURL = manga.getCoverURL();
                    if (currentURL == null || !currentURL.equals(remoteCover)) {
                        String fileName = remoteCover.substring(remoteCover.lastIndexOf('/') + 1);
                        if (fileName.contains("?")) {
                            fileName = fileName.substring(0, fileName.indexOf('?'));
                        }
                        if (fileName.isEmpty() || !fileName.contains(".")) {
                            fileName = manga.getMangaID() + ".jpg";
                        }
                        String coverDir = AppConfig.DATA_FOLDER + manga.getSourceID() + "/covers/";
                        String localCoverPath = coverDir + fileName;
                        try {
                            Files.createDirectories(Paths.get(coverDir));
                            try (InputStream in = new URL(remoteCover).openStream()) {
                                Files.copy(in, Paths.get(localCoverPath), StandardCopyOption.REPLACE_EXISTING);
                            }
                            if (currentURL != null && currentURL.startsWith("file://")) {
                                try {
                                    Files.deleteIfExists(Paths.get(URI.create(currentURL)));
                                } catch (Exception e) { /* ignore */ }
                            }
                            manga.setCoverURL("file://" + new File(localCoverPath).getAbsolutePath());
                        } catch (Exception e) {
                            Logger.info("Could not download new cover, keeping the old one ("+e.getClass().getSimpleName()+")");
                        }
                    }
                }
            
                if (disposed) return;
                
                List<Chapter> oldChapters = manga.getChapters();
                source.fetchMangaData(manga);

                MangaSource src = source.getSource(manga.getSourceID());
                List<Chapter> newChapters = src.getChapters(manga.getMangaID());

                if (disposed) return;

                List<Chapter> finalChapters = new ArrayList<>();

                if (newChapters != null) {
                    for (Chapter newCap : newChapters) {
                        Chapter matchingOldCap = null;

                        if (oldChapters != null) {
                            for (Chapter oldCap : oldChapters) {
                                // Aplicamos el cambio que mencionaste previamente: 
                                // Usar el ID en vez de la instancia del objeto
                                if (oldCap.getChapterID().equals(newCap.getChapterID())) {
                                    matchingOldCap = oldCap;
                                    break;
                                }
                            }
                        }

                        if (matchingOldCap != null) {
                            finalChapters.add(matchingOldCap);
                            Logger.info("Reload chapter: "+matchingOldCap.getTitle());
                        } else {
                            finalChapters.add(newCap);
                            Logger.info("Loaded NEW chapter: " + newCap.getTitle());
                        }
                    }
                }

                manga.setChapters(finalChapters);
                source.saveManga(manga);

                if (library.onLibrary(manga)) {
                    library.saveLibrary();
                }

                Platform.runLater(() -> {
                    if (disposed || view == null) return;
                    
                    view.setTitle(manga.getTitle());
                    view.setAuthor(manga.getAuthor());
                    view.setDescrition(manga.getDescription());
                    view.setTags(getTags());
                    view.doReloadChapters();
                    view.loadCover(manga.getCoverURL());
                    
                    view.doHideLoadingPane();

                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    view.doHideLoadingPane();
                    if (!disposed) Logger.error("Error reloading manga("+e.getClass().getSimpleName()+"): " + e.getMessage());
                });
            }
        });
    }
    
    public void downloadChapter(Chapter chapter){
        Downloader.getInstance().enqueue(chapter, manga);
    }
    
    // 4. Método para matar el hilo y destruir enlaces con la UI
    public void cleanup() {
        disposed = true;
        preloader.shutdownNow();
        this.view = null;
    }
}