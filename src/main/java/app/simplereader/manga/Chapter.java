package app.simplereader.manga;

import app.simplereader.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

/**
 *
 * @author david
 */
public class Chapter {
    private int chNum;
    private final File folder;
    private List<File> pages = new ArrayList<>();
    private List<String> zipPages = new ArrayList<>();
    private String chName = "Chapter";
    private ChapterType type;
    public Chapter(File folder, ChapterType type) {
        this.folder = folder;
        this.chName = folder.getName();
        this.type = type;
    }
    
    private void loadPages() {
        switch(type){
            case FOLDER -> {
                loadFromFolder();
            }
            case ZIP, CBZ -> {
                loadFromZip();
            }
        }
    }
    
    private void loadFromFolder(){
        if(this.pages != null){
            pages.clear();
        }
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
            this.pages.add(archivo);
            Logger.info("Loaded: "+archivo.getName());
        }
    }

    private void loadFromZip() {
        try (ZipFile zip = new ZipFile(folder)) {
            zip.stream()
                .filter(entry -> !entry.isDirectory() && isImage(entry.getName()))
                .sorted(Comparator.comparing(java.util.zip.ZipEntry::getName))
                .forEach(entry -> {
                    zipPages.add(entry.getName()); // guarda en zipPages ✅
                    Logger.info("Loaded: " + entry.getName());
                });
        } catch (IOException e) {
            Logger.error("No se pudo leer zip: " + folder.getName());
        }
    }
    private boolean isImage(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".png") ||
               lower.endsWith(".webp");
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
        if (type == ChapterType.FOLDER) {
            return !pages.isEmpty();
        } else {
            return !zipPages.isEmpty();
        }
    }
    public InputStream getInputStream(int index) throws IOException {
        ZipFile zip = new ZipFile(folder);
        java.util.zip.ZipEntry entry = zip.getEntry(zipPages.get(index)); // acceso directo ✅
        return zip.getInputStream(entry);
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
        loadPages();
        return pages;
    }
    
    public List<String> getZipPages(){
        loadPages();
        return zipPages;
    }

    public File getFolder() {
        return folder;
    }

    public ChapterType getType() {
        return type;
    }
    
    
}
