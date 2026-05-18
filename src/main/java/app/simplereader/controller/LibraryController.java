package app.simplereader.controller;

import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author david
 */
public class LibraryController {
    
    private static LibraryController instance;
    
    private Map<String, Category> categories = new HashMap<>();
    
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String LIBRARY_FILE = AppConfig.DATA_FOLDER + "library.json";
    
    public static LibraryController getInstance(){
        if (instance == null) {
            instance = new LibraryController();
        }
        return instance;
    }
    
    private LibraryController(){
        categories.put("Default", new Category("Default"));
        loadLibrary();
    }
    
    /*
    Categorias:
    */    
    public void addCategory(String name){
        if (!categories.containsKey(name)) {
            categories.put(name, new Category(name));
        }
    }
    
    public void removeCategory(String name){
        if (name.equals("Default")) return;
        Category cat = categories.get(name);
        if (cat != null) {
            Category defaultCat = categories.get("Default");
            for (Manga manga : cat.getMangas()) {
                defaultCat.addManga(manga);
            }
            categories.remove(name);
        }
    }
    
    public Category getCategory(String name){
        return categories.get(name);
    }
    
    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }
    
    /*
    Mangas:
    */
    
    public void addManga(Manga manga, String category){
        Category cat = categories.get(category);
        if (cat == null) {
            cat = categories.get("Default");
        }
        cat.addManga(manga);
    }
    
    public void removeManga(Manga manga){
        for (Category cat : categories.values()) {
            cat.removeManga(manga);
        }
    }
    
    public void moveManga(Manga manga, String newCategory){
        for (Category cat : categories.values()) {
            cat.removeManga(manga);
        }
        addManga(manga, newCategory);
    }
    
    public List<Manga> getMangasByCategory(String c){
        Category cat = categories.get(c);
        return cat != null ? cat.getMangas() : new ArrayList<>();
    }
    
    public List<Manga> getAllMangas(){
        List<Manga> all = new ArrayList<>();
        for (Category cat : categories.values()) {
            all.addAll(cat.getMangas());
        }
        return all;
    }
    
    private void loadLibrary(){
        File file = new File(LIBRARY_FILE);
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, List<Manga>>>(){}.getType();
            Map<String, List<Manga>> data = gson.fromJson(reader, type);
            
            if (data != null) {
                for (Map.Entry<String, List<Manga>> entry : data.entrySet()) {
                    String catName = entry.getKey();
                    List<Manga> mangaList = entry.getValue();
                    
                    Category cat = categories.get(catName);
                    if (cat == null) {
                        cat = new Category(catName);
                        categories.put(catName, cat);
                    }
                    
                    for (Manga manga : mangaList) {
                        // REPARAR REFERENCIAS DE CAPÍTULOS
                        restoreChapterRefs(manga);
                        cat.addManga(manga);
                    }
                }
            }
        } catch (IOException e) {
            Logger.error("Error cargando biblioteca: " + e.getMessage());
        }
    }
    
    private void restoreChapterRefs(Manga manga) {
        if (manga.getChapters() != null) {
            for (Chapter ch : manga.getChapters()) {
                ch.setManga(manga); // Reconectar el capítulo con su padre
            }
        }
    }
    
    public void saveLibrary(){
        Logger.info("Guardando libreria...");
        try {
            Files.createDirectories(Paths.get(LIBRARY_FILE).getParent());
            
            Map<String, List<Manga>> data = new HashMap<>();
            for (Category cat : categories.values()) {
                data.put(cat.getName(), cat.getMangas());
            }
            
            try (FileWriter writer = new FileWriter(LIBRARY_FILE)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            Logger.error("Error guardando biblioteca: " + e.getMessage());
        }
    }
}
