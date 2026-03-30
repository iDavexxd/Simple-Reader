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
public class Chapter {
    private int chNum;
    private File folder;
    private List<File> pages = new ArrayList<>();
    private String chName = "Chapter";
    
    public Chapter(File folder) {
        this.folder = folder;
        this.chName = folder.getName();
        // cargar paginas al crear un capitulo.
        loadPages();
    }
    
    private void loadPages() {
        // Filtrar imagenes.
        File[] images = folder.listFiles((dir, nombre) ->
            nombre.endsWith(".jpg") ||
            nombre.endsWith(".jpeg") ||
            nombre.endsWith(".png")
        );
        if(images == null || images.length == 0) {
            Logger.warning("No hay paginas en: "+folder.getName());
            return;
        }
        //sortear y agregar a la lista
        Arrays.sort(images);
        for (File archivo : images) {
            pages.add(archivo);
            Logger.info("Loaded: "+archivo.getName());
        }
    }
    public String getChName() {
        return chName;
    }

    public void setChName(String chName) {
        this.chName = chName;
    }

    public void setChNum(int chNum) {
        this.chNum = chNum;
    }

    public void setPages(List<File> pages) {
        this.pages = pages;
    }

    public int getChNum() {
        return chNum;
    }

    public List<File> getPages() {
        return pages;
    }

    public File getFolder() {
        return folder;
    }
    
    
}
