package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    
    private final Map<String, Category> categories = new HashMap<>();
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
    
    public boolean onCategory(Category category, Manga manga){
        
        if(category.getMangas().containsKey(manga.getMangaID())){
            return true;
        }
        return false;
        
    }
    public void removeCategory(String name){
        if (name.equals("Default")) return;
        Category cat = categories.get(name);
        if (cat != null) {
            Category defaultCat = categories.get("Default");
            for (Manga manga : cat.getMangas().values()) {
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
    
    public void removeMangaFrom(Category category, Manga manga){
        if(category != null) category.removeManga(manga);
    }
    
    public void removeManga(Manga manga){
        for (Category cat : categories.values()) {
            cat.removeManga(manga);
        }
    }
    
    public boolean onLibrary(Manga manga){
        return getAllMangas().contains(manga);
    }
    
    public void moveManga(Manga manga, String newCategory){
        for (Category cat : categories.values()) {
            cat.removeManga(manga);
        }
        addManga(manga, newCategory);
    }
    
    public HashMap<String,Manga> getMangasByCategory(String c){
        Category cat = categories.get(c);
        if (cat == null) return new HashMap<>();
        HashMap<String, Manga> mangas = cat.getMangas();
        return mangas != null ? mangas : new HashMap<>();
    }
    
    public List<Manga> getAllMangas(){
        List<Manga> all = new ArrayList<>();
        for (Category cat : categories.values()) {
            all.addAll(cat.getMangas().values());
        }
        return all;
    }
    
    private void loadLibrary(){
        File file = new File(LIBRARY_FILE);
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject raw = gson.fromJson(reader, JsonObject.class);
            if (raw == null) return;
            
            boolean migrated = false;
            for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
                JsonObject catObj = entry.getValue().getAsJsonObject();
                
                if (catObj.has("mangaList") && !catObj.has("mangas")) {
                    JsonArray mangaList = catObj.getAsJsonArray("mangaList");
                    JsonObject mangasMap = new JsonObject();
                    for (JsonElement elem : mangaList) {
                        JsonObject mangaObj = elem.getAsJsonObject();
                        String id = mangaObj.get("mangaID").getAsString();
                        mangasMap.add(id, mangaObj);
                    }
                    catObj.add("mangas", mangasMap);
                    catObj.remove("mangaList");
                    migrated = true;
                }
            }
            
            if (migrated) {
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(raw, writer);
                }
            }
            
            Type type = new TypeToken<Map<String, Category>>(){}.getType();
            Map<String, Category> data = gson.fromJson(raw, type);
            
            if (data != null) {
                for (Map.Entry<String, Category> entry : data.entrySet()) {
                    String catName = entry.getKey();
                    Category cat = entry.getValue();
                    
                    if (cat.getMangas() != null) {
                        for (Manga manga : cat.getMangas().values()) {
                            restoreChapterRefs(manga);
                        }
                    }
                    
                    categories.put(catName, cat);
                }
            }
        } catch (Exception e) {
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
            
            // 4. Guardamos directamente nuestro mapa de categorías entero
            try (FileWriter writer = new FileWriter(LIBRARY_FILE)) {
                gson.toJson(categories, writer);
            }
        } catch (IOException e) {
            Logger.error("Error guardando biblioteca: " + e.getMessage());
        }
    }
}
