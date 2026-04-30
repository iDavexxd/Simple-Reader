package app.simplereader.manga;

import app.simplereader.Logger;
import app.simplereader.interfaces.Manga;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author david
 */
public class MangaLoader {
    
    public static List<Manga> loadMangas() {
        List<Manga> lista = new ArrayList<>();
        lista.addAll(loadLocalMangas());
        lista.addAll(loadMangaDexMangas());
        return lista;
    }
    
    private static List<Manga> loadLocalMangas() {
        String home = System.getProperty("user.home");
        File mainfolder = new File(home + "/Documents/SimpleReader/mangas");
        if (!mainfolder.exists()) {
            mainfolder.mkdirs();
            Logger.info("Carpeta creada: " + mainfolder.getPath());
        }

        File[] mangas = mainfolder.listFiles((dir, nombre) -> 
            new File(dir, nombre).isDirectory()
        );
        if (mangas == null || mangas.length == 0) {
            Logger.info("No hay mangas locales.");
            return new ArrayList<>();
        }

        Arrays.sort(mangas);
        List<Manga> lista = new ArrayList<>();
        for (File subcarpeta : mangas) {
            lista.add(new LocalManga(subcarpeta, subcarpeta.getName(), "", ""));
        }
        return lista;
    }
    
    private static List<Manga> loadMangaDexMangas() {
        String home = System.getProperty("user.home");
        File dataFolder = new File(home + "/Documents/SimpleReader/data");
        
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
                lista.add(new mdManga(mangaID));
                Logger.info("MangaDex cargado: " + mangaID);
            } catch (Exception e) {
                Logger.error("Error leyendo: " + archivo.getName());
                e.printStackTrace();
            }
        }
        return lista;
    }
}
