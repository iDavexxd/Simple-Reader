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
    private final File folder;
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
        Arrays.sort(images, (f1, f2) -> {
            String s1 = f1.getName().toLowerCase();
            String s2 = f2.getName().toLowerCase();

            int i = 0, j = 0;
            while (i < s1.length() && j < s2.length()) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);

                // Si ambos encuentran un número, comparamos el bloque numérico completo
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    StringBuilder num1 = new StringBuilder();
                    StringBuilder num2 = new StringBuilder();

                    while (i < s1.length() && Character.isDigit(s1.charAt(i))) {
                        num1.append(s1.charAt(i++));
                    }
                    while (j < s2.length() && Character.isDigit(s2.charAt(j))) {
                        num2.append(s2.charAt(j++));
                    }

                    long v1 = Long.parseLong(num1.toString());
                    long v2 = Long.parseLong(num2.toString());

                    if (v1 != v2) return Long.compare(v1, v2);
                } else {
                    // Si son letras, comparamos normal
                    if (c1 != c2) return c1 - c2;
                    i++;
                    j++;
                }
            }
            return s1.length() - s2.length();
        });
        for (File archivo : images) {
            pages.add(archivo);
            Logger.info("Loaded: "+archivo.getName());
        }
    }
    
    public List<File> reloadPages(){
        //Filtrar imagenes
        File[] images = folder.listFiles((dir, nombre) ->
            nombre.endsWith(".jpg") ||
            nombre.endsWith(".jpeg") ||
            nombre.endsWith(".png")
        );
        if(images == null || images.length == 0) {
            Logger.warning("No hay paginas en: "+folder.getName());
            this.pages.clear();
            return this.pages;
        }
        Arrays.sort(images, (f1, f2) -> {
            String s1 = f1.getName().toLowerCase();
            String s2 = f2.getName().toLowerCase();

            int i = 0, j = 0;
            while (i < s1.length() && j < s2.length()) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);

                // Si ambos encuentran un número, comparamos el bloque numérico completo
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    StringBuilder num1 = new StringBuilder();
                    StringBuilder num2 = new StringBuilder();

                    while (i < s1.length() && Character.isDigit(s1.charAt(i))) {
                        num1.append(s1.charAt(i++));
                    }
                    while (j < s2.length() && Character.isDigit(s2.charAt(j))) {
                        num2.append(s2.charAt(j++));
                    }

                    long v1 = Long.parseLong(num1.toString());
                    long v2 = Long.parseLong(num2.toString());

                    if (v1 != v2) return Long.compare(v1, v2);
                } else {
                    // Si son letras, comparamos normal
                    if (c1 != c2) return c1 - c2;
                    i++;
                    j++;
                }
            }
            return s1.length() - s2.length();
        });
        this.pages.clear();
        for (File archivo : images) {
            this.pages.add(archivo);
            Logger.info("Loaded: "+archivo.getName());
        }
        return this.pages;
    }
    
    public Boolean hasPages(){
        return !this.pages.isEmpty();
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
