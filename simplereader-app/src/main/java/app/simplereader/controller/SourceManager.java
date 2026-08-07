package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.LocalSource;
import app.simplereader.model.Manga;
import app.simplereader.repository.AppExtension;
import app.simplereader.repository.MangaSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 *
 * @author david
 */
public class SourceManager {
    
    private static SourceManager instance;

    private final List<MangaSource> sources = new ArrayList<>();
    private final List<AppExtension> extensions = new ArrayList<>();
    
    private final List<URLClassLoader> activeClassLoaders = new ArrayList<>();
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FOLDER = AppConfig.DATA_FOLDER;
    
    private SourceManager() {}
    
    public static SourceManager getInstance() {
        if (instance == null) {
            instance = new SourceManager();
        }
        return instance;
    }
    
    public void reloadSources(){
        sources.clear();
        extensions.clear();
        
        // --- NUEVO: Cerrar los ClassLoaders viejos antes de cargar los nuevos ---
        for (URLClassLoader loader : activeClassLoaders) {
            try {
                loader.close();
            } catch (IOException e) {
                Logger.error("Error cerrando ClassLoader: " + e.getMessage());
            }
        }
        activeClassLoaders.clear();
        
        app.simplereader.model.LocalExtension localExt = new app.simplereader.model.LocalExtension();
        extensions.add(localExt);
        for(MangaSource s : localExt.getSources()){
            registerSource(s);
        }
        
        loadSources();
    }
    
    public void loadSources(){
        File pluginsFolder = new File(AppConfig.PLUGIN_FOLDER);
        if(!pluginsFolder.exists()){
            try{
                pluginsFolder.mkdirs();
                Logger.info("Created plugins folder.");
            } catch(Exception e){
                Logger.error("Can't create plugins folder. -> "+e.getMessage());
            }
        }
        
        File[] jars = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            Logger.info("No plugins found.");
            return;
        }
        
        Logger.info("Loading " + jars.length + " plugin(s)...");
        
        for(File jar : jars){
            try{
                URLClassLoader child = new URLClassLoader(
                    new URL[]{jar.toURI().toURL()},
                    this.getClass().getClassLoader()
                );
                
                // --- NUEVO: Registrar el ClassLoader para cerrarlo después ---
                activeClassLoaders.add(child);
                
                ServiceLoader<AppExtension> loader = ServiceLoader.load(AppExtension.class, child);
                
                for (AppExtension extension : loader) {
                    extensions.add(extension);
                    for(MangaSource source : extension.getSources()){
                        registerSource(source);
                    }
                    Logger.info("Loaded plugin:  " + extension.getName());
                }                
            } catch (Exception e){
                Logger.error("Error loading plugin " + jar.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            
        }
    }
    
    public void registerSource(MangaSource source) {
        sources.add(source);
    }
    
    public MangaSource getSource(String id) {
        return sources.stream()
            .filter(s -> s.getID().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    public List<MangaSource> getAllSources() {
        return sources;
    }
    
    public List<AppExtension> getExtensions() {
        return extensions;
    }
    
    public List<Manga> searchManga(MangaSource source, String query) {
        return source.searchManga(query);
    }
    
    public void fetchMangaData(Manga manga) {
        MangaSource source = getSource(manga.getSourceID());
        if (source != null) {
            source.fetchMangaData(manga);
        }
    }
    
    public void saveManga(Manga manga) {
        try {
            String sourceId = manga.getSourceID();
            String mangaId = manga.getMangaID();
            String dir = DATA_FOLDER + sourceId + "/";
            String coverDir = dir + "covers/";
            
            Files.createDirectories(Paths.get(dir));
            Files.createDirectories(Paths.get(coverDir));
            
            String currentCover = manga.getCoverURL();
            if (currentCover != null && currentCover.startsWith("http")) {
                // Extraer solo la extensión del archivo de la URL (.jpg, .webp, .png, etc.)
                String urlFileName = currentCover.substring(currentCover.lastIndexOf('/') + 1);
                if (urlFileName.contains("?")) {
                    urlFileName = urlFileName.substring(0, urlFileName.indexOf('?'));
                }
                String ext = ".jpg"; // extensión por defecto
                if (urlFileName.contains(".")) {
                    ext = urlFileName.substring(urlFileName.lastIndexOf('.'));
                }
                // Siempre usar el ID del manga como nombre para evitar colisiones
                String safeMangaId = mangaId.replace("/", "_");
                String fileName = safeMangaId + ext;
                String localCoverPath = coverDir + fileName;
                File localCoverFile = new File(localCoverPath);
                
                if (!localCoverFile.exists()) {
                    Logger.info("Downloading cover for " + manga.getTitle());
                    downloadFile(currentCover, localCoverPath, getSource(sourceId));
                }
                
                manga.setCoverURL(localCoverFile.toURI().toURL().toString());
            }
            
            String safeMangaId = mangaId.replace("/", "_");
            String path = dir + safeMangaId + ".json";
            try (FileWriter writer = new FileWriter(path)) {
                gson.toJson(manga, writer);
            }
        } catch (Exception e) {
            Logger.error("Error guardando manga: " + e.getMessage());
        }
    }
    
    public Manga loadManga(String sourceID, String mangaID) {
        String safeMangaId = mangaID.replace("/", "_");
        String path = DATA_FOLDER + sourceID + "/" + safeMangaId + ".json";
        File file = new File(path);
        if (!file.exists()) return null;
        
        try (FileReader reader = new FileReader(file)) {
            Manga manga = gson.fromJson(reader, Manga.class);
            restoreChapterRefs(manga);
            return manga;
        } catch (IOException e) {
            Logger.error("Error cargando manga: " + e.getMessage());
            return null;
        }
    }
    
    private void restoreChapterRefs(Manga manga) {
        if (manga.getChapters() != null) {
            for (Chapter ch : manga.getChapters()) {
                ch.setManga(manga);
            }
        }
    }
    
    private void downloadFile(String url, String destination, MangaSource source) throws Exception {
        try (InputStream in = app.simplereader.service.Http.getInputStreamWithRetry(url, source)) {
            Files.copy(in, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}