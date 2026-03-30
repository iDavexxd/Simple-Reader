package app.simplereader.manga;

import app.simplereader.Logger;
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
        String home = System.getProperty("user.home");
        File mainfolder = new File(home + "/Documents/SimpleReader/mangas");
        if (!mainfolder.exists()) {
            mainfolder.mkdirs();
            Logger.info("Carpeta creada: " + mainfolder.getPath());
        }
        File[] mangas = mainfolder.listFiles((dir, nombre) -> 
            new File(dir, nombre).isDirectory()
        );
        if(mangas == null || mangas.length == 0) {
            Logger.info("No hay mangas!!");
            return new ArrayList<>();
        }
        
        Arrays.sort(mangas);
        List<Manga> lista = new ArrayList<>();
        for (File subcarpeta : mangas) {
            lista.add(new Manga(subcarpeta, subcarpeta.getName(), "", ""));
        }
        return lista;        
    }    
}
