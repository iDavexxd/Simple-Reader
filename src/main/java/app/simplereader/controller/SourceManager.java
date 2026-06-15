package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.LocalSource;
import app.simplereader.model.Manga;
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

    private List<MangaSource> sources = new ArrayList<>();
    
    // --- NUEVO: Lista para rastrear y poder cerrar los ClassLoaders ---
    private List<URLClassLoader> activeClassLoaders = new ArrayList<>();
    
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
        
        // --- NUEVO: Cerrar los ClassLoaders viejos antes de cargar los nuevos ---
        for (URLClassLoader loader : activeClassLoaders) {
            try {
                loader.close();
            } catch (IOException e) {
                Logger.error("Error cerrando ClassLoader: " + e.getMessage());
            }
        }
        activeClassLoaders.clear();
        
        registerSource(new LocalSource());
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
                
                ServiceLoader<MangaSource> loader = ServiceLoader.load(MangaSource.class, child);
                
                for (MangaSource source : loader) {
                    registerSource(source);
                    Logger.info("Loaded plugin:  " + source.getName());
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
                String fileName = currentCover.substring(currentCover.lastIndexOf('/') + 1);
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf('?'));
                }
                if (fileName.isEmpty() || !fileName.contains(".")) {
                    fileName = mangaId + ".jpg";
                }
                String localCoverPath = coverDir + fileName;
                File localCoverFile = new File(localCoverPath);
                
                if (!localCoverFile.exists()) {
                    Logger.info("Downloading cover for " + manga.getTitle());
                    downloadFile(currentCover, localCoverPath);
                }
                
                manga.setCoverURL(localCoverFile.toURI().toURL().toString());
            }
            
            String path = dir + mangaId + ".json";
            try (FileWriter writer = new FileWriter(path)) {
                gson.toJson(manga, writer);
            }
        } catch (IOException e) {
            Logger.error("Error guardando manga: " + e.getMessage());
        }
    }
    
    public Manga loadManga(String sourceID, String mangaID) {
        String path = DATA_FOLDER + sourceID + "/" + mangaID + ".json";
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
    
    private void downloadFile(String url, String destination) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}