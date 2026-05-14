package app.simplereader.model;

import app.simplereader.controller.CategoryController;
import app.simplereader.controller.Logger;
import app.simplereader.repository.Manga;
import app.simplereader.repository.MangaSource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class MangadexSource implements MangaSource{
    private CategoryController manager;
    public MangadexSource(CategoryController manager){
        this.manager = manager;
    }
    
    @Override
    public List<Manga> loadMangas(){
        String home = System.getProperty("user.home");
        File dataFolder = new File(home + "/Documents/SimpleReader/data/MangaDex/");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
            return new ArrayList<>();
        }

        File[] archivos = dataFolder.listFiles((dir, nombre) -> 
            nombre.startsWith("md_") && nombre.endsWith(".json")
        );
        if (archivos == null || archivos.length == 0) {
            Logger.info("No hay mangas de MangaDex guardados.");
            return new ArrayList<>();
        }

        List<Manga> lista = new ArrayList<>();
        for (File archivo : archivos) {
            try {
                String contenido = java.nio.file.Files.readString(archivo.toPath());
                JsonObject json = JsonParser.parseString(contenido).getAsJsonObject();
                String mangaID = json.get("id").getAsString();
                mdManga manga = new mdManga(mangaID);
                Logger.info("Manga "+manga.getTitle()+" Categoría leída: " + manga.getCategory()); // ← debug
                if (manga.getCategory() == null) manga.setCategory("Default");

                Category category = manager.getCategories().get(manga.getCategory());
                if (category == null) {                                  // ← agrega esto
                    manga.setCategory("Default");
                    category = manager.getCategories().get("Default");
                }

                category.addManga(manga);
                Logger.info("Manga: "+manga.getTitle()+" - Category: "+manga.getCategory());
                lista.add(manga);
                
                Logger.info("MangaDex cargado: " + mangaID);
            } catch (Exception e) {
                Logger.error("Error leyendo: " + archivo.getName());
                e.printStackTrace();
            }
        }
        return lista;
    }
    
}
