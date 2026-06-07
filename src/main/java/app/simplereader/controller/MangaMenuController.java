package app.simplereader.controller;

import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnMangaMenu;
import app.simplereader.views.ScnReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;


/**
 *
 * @author david
 */
public class MangaMenuController {
    
   
    
    private String DOWNLOADS_FOLDER = AppConfig.DATA_FOLDER;
    
    private final ScnMangaMenu view;
    private static MangaMenuController instance;
    private final SceneController nav = SceneController.getInstance();
    private final LibraryController library = LibraryController.getInstance();
    private final MainMenuController mainMenuController = MainMenuController.getInstance();
    
    private final SourceManager source = SourceManager.getInstance();
    
    private final HashSet<String> isDownloading;
    
    private Manga manga;
    private boolean isReversed = false;
    
    private final ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "preloader");
        t.setDaemon(true);
        return t;
    });
    
    public MangaMenuController(ScnMangaMenu view){
        this.view = view;
        isDownloading  = new HashSet<>();
    }
    
    public static void doInstance(ScnMangaMenu view){
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
        view.doCategoryButtons();
    }
    
    public void openChapter(Chapter chapter){
        if (chapter.hasPages()) {
            navigateToReader(chapter);
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
                navigateToReader(chapter);
                return; // Importante: retornar para que no siga con la carga web
            }
        }
        
        MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
        if (src != null) {
            preloader.submit(() -> {
                try{
                    List<String> pages = src.getPages(manga.getMangaID(), chapter.getChapterID());
                    chapter.setPages(pages);
                    if (chapter.hasPages()) {
                        Platform.runLater(() -> navigateToReader(chapter));
                    } else {
                        Platform.runLater(() -> Logger.noPagesAlert(chapter.getTitle()));
                    }
                } catch (Exception e){
                    Platform.runLater(() -> Logger.error("Error loading chapter: "+chapter.getChapterID()));
                }
            });
        }
        
    }

    private void navigateToReader(Chapter chapter){
        List<Chapter> chapters = manga.getChapters();
        int index = chapters != null ? chapters.indexOf(chapter) : -1;
        Logger.info("Selected: " + chapter.getTitle());
        nav.goTo(new ScnReader(nav, manga, chapter, index));
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
        // Trabajo lento (HTTP) en el pool, no en un Thread suelto
        preloader.submit(() -> {
            try {
                // 1. Respaldamos las INSTANCIAS VIEJAS
                List<Chapter> oldChapters = manga.getChapters();

                source.fetchMangaData(manga);

                // 2. Traemos las INSTANCIAS NUEVAS
                MangaSource src = source.getSource(manga.getSourceID());
                List<Chapter> newChapters = src.getChapters(manga.getMangaID());

                // 3. Creamos una lista para meter los objetos definitivos
                List<Chapter> finalChapters = new ArrayList<>();

                if (newChapters != null) {
                    for (Chapter newCap : newChapters) {
                        Chapter matchingOldCap = null;

                        if (oldChapters != null) {
                            for (Chapter oldCap : oldChapters) {
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

                // 4. Guardamos los capítulos actualizados en el manga
                manga.setChapters(finalChapters);
                source.saveManga(manga);

                // 5. Guardar en la biblioteca para asegurar la persistencia
                if (library.onLibrary(manga)) {
                    library.saveLibrary();
                }

                // 6. Refrescar la vista en el hilo de la UI
                Platform.runLater(() -> {
                    view.setTitle(manga.getTitle());
                    view.setAuthor(manga.getAuthor());
                    view.setDescrition(manga.getDescription());
                    view.setTags(getTags());
                    view.doReloadChapters();
                    view.loadCover(manga.getCoverURL());
                });
            } catch (Exception e) {
                Platform.runLater(() -> Logger.error("Error reloading manga: " + e.getMessage()));
            }
        });
    }
    
    public void downloadChapter(Chapter chapter){
        // 1. Validaciones iniciales en el hilo de UI
        if(chapter == null || manga == null) return;
        File folder = new File(DOWNLOADS_FOLDER + manga.getSourceID() + "/downloads/" + manga.getMangaID() + "/" + chapter.getChapterID());
        
        // --- INICIO DE LA MODIFICACIÓN ---
        // Ya no cancelamos aquí si hay *algún* archivo, porque queremos poder reanudar
        // Solo verificamos si la carpeta existe. Si no, se creará después.
        // --- FIN DE LA MODIFICACIÓN ---
        
        if(isDownloading.contains(chapter.getChapterID())){
            Logger.error("Ya estas descargando ese capitulo.");
            return;
        }else{
            isDownloading.add(chapter.getChapterID());
        }
        if(manga.getSourceID().equals("local")) return;

        preloader.submit(() -> {
            try {
                // 2. Creación del directorio
                if(!folder.exists()) {
                    folder.mkdirs();
                }

                // 3. Obtener las URLs de las páginas
                List<String> pagesURL = source.getSource(manga.getSourceID()).getPages(manga.getMangaID(), chapter.getChapterID());
                
                // Si ya están todas las páginas descargadas, evitamos procesar y avisamos
                File[] existingFiles = folder.listFiles();
                if (existingFiles != null && existingFiles.length >= pagesURL.size()) {
                    Logger.info("El capítulo ya está descargado por completo.");
                    chapter.setDownloaded(true);
                    return; // Terminamos la tarea aquí
                }

                // 4. Descargar cada página
                int pageNumber = 1;
                HttpClient httpClient = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                
                for (String urlString : pagesURL) {
                    
                    // Definir la extensión y el nombre del archivo primero
                    String extension = ".jpg";
                    int lastDot = urlString.lastIndexOf('.');
                    if (lastDot > 0 && lastDot < urlString.length() - 1) {
                        String potentialExt = urlString.substring(lastDot);
                        if(potentialExt.contains("?")) potentialExt = potentialExt.split("\\?")[0];
                        if(potentialExt.length() <= 5) extension = potentialExt;
                    }
                    
                    String fileName = String.format("%03d%s", pageNumber, extension);
                    File outputFile = new File(folder, fileName);

                    // --- NUEVA VALIDACIÓN: Saltar si ya existe ---
                    if (outputFile.exists() && outputFile.length() > 0) {
                        Logger.info("Página ya existe, omitiendo: " + fileName);
                        pageNumber++;
                        continue; // Salta a la siguiente iteración del loop for
                    }
                    
                    boolean downloaded = false;
                    for (int attempt = 1; attempt <= 3 && !downloaded; attempt++) {
                        try {
                            if (attempt > 1) {
                                pagesURL = source.getSource(manga.getSourceID()).getPages(manga.getMangaID(), chapter.getChapterID());
                                urlString = pagesURL.get(pageNumber - 1);
                                fileName = String.format("%03d%s", pageNumber, extension);
                                outputFile = new File(folder, fileName);
                            }
                            
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(urlString))
                                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                    .header("Referer", "https://mangadex.org/")
                                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                    .timeout(Duration.ofSeconds(15))
                                    .GET()
                                    .build();
                            
                            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                            
                            if (response.statusCode() == 200) {
                                try (InputStream in = response.body()) {
                                    Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    Logger.info("Downloaded: " + fileName);
                                    downloaded = true;
                                }
                            } else {
                                throw new IOException("HTTP " + response.statusCode());
                            }
                        } catch (IOException | InterruptedException e) {
                            Logger.error("Error (intento " + attempt + "/3) página " + pageNumber + ": " + urlString);
                            if (attempt < 3) {
                                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (!downloaded) break;
                    pageNumber++;
                    
                } 
                File[] downloadedFiles = folder.listFiles();
                if (downloadedFiles != null && downloadedFiles.length >= pagesURL.size()) {
                    chapter.setDownloaded(true);
                    Logger.info("Proceso de descarga finalizado para "+chapter.getTitle()+".");
                }
                
            } finally {
                // 5. Limpieza garantizada
                Platform.runLater(() -> {
                    isDownloading.remove(chapter.getChapterID());
                });
            }
        });
    }
    
    
}
