package app.simplereader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author david
 */
public class CategoryManager {
    
    private static final String PATH = System.getProperty("user.home") + "/Documents/SimpleReader/data/user/categories.json";
    
    private List<String> nameList = new ArrayList<>();
    private HashMap<String, Category> categories = new HashMap<>(); // Nombre, Objeto categoria

    private List<Category> categoryList = new ArrayList<>();
    private final Gson gson = new Gson();
    
    public List<String> getNameList(){
        return this.nameList;
    }
    
    public HashMap<String, Category> getCategories(){
        return categories;
    }
    
    public CategoryManager(){
        loadCategories();
        saveCategories();
    }
    
    public void addCategory(String name){
        if (!nameList.contains(name)) {
            nameList.add(name);
            Category category = new Category(name);
            categories.put(name, category);
            categoryList.add(category);
            saveCategories();
        }
    }
    
    public void removeCategory(String name){
        if (!name.equals("Default") && nameList.contains(name)) {
            nameList.remove(name);
            categories.remove(name);
            categoryList.removeIf(c -> c.getName().equals(name));
            saveCategories();
        }
    }
    
    private void loadCategories(){
        File file = new File(PATH);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> loaded = gson.fromJson(reader, listType);
                if (loaded != null) nameList = loaded;
            } catch (IOException e) {
                Logger.error("Error cargando categorías: " + e.getMessage());
            }
        }
        
        createCategories();        
    }
    
    private void createCategories(){
        categories.put("Default", new Category("Default"));
        
        for (String name : nameList) {
            Category category = new Category(name);
            categoryList.add(category);
            categories.put(name, category);
        }
        
    }
    public void reloadCategories(){
        
    }
    
    public void saveCategories(){
        try {
            // Crear carpeta si no existe
            Files.createDirectories(Paths.get(PATH).getParent());
            
            try (Writer writer = new FileWriter(PATH)) {
                gson.toJson(nameList, writer);
                Logger.info("Categorías guardadas.");
            }
        } catch (IOException e) {
            Logger.error("Error guardando categorías: " + e.getMessage());
        }
    }

}
