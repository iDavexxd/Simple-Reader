
package app.simplereader.manga.chapter;

import app.simplereader.Logger;
import app.simplereader.Sorter;
import app.simplereader.interfaces.Chapter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class ZipChapter implements Chapter{
    
    private final File zipFile;
    private final String name;
    private List<String> pages;
    public ZipChapter(File zipFile){
        this.zipFile = zipFile;
        String fname = zipFile.getName();
        int dot = fname.lastIndexOf('.');
        this.name = (dot != -1) ? fname.substring(0, dot) : fname;
    }
    
    private void loadPages(){
        if(this.pages != null) return;
        //crear lista
        this.pages = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipFile)) {
            zip.stream()
                .filter(entry -> !entry.isDirectory() && isImage(entry.getName()))
                .sorted((e1, e2) -> 
                    Sorter.compare(e1.getName(), e2.getName())
                )
                .forEach(entry -> {
                    pages.add(entry.getName());
                    Logger.info("Loaded: " + entry.getName());
                });

        } catch (IOException e) {
            Logger.error("Error leyendo zip: " + zipFile.getName());
        }
    }
    private boolean isImage(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".png");
    }
    
    @Override
    public int getPageCount() {
        loadPages();
        return this.pages.size();
    }

    @Override
    public Image getPage(int index) {
        loadPages();

        if (index < 0 || index >= pages.size()) {
            throw new IndexOutOfBoundsException("Página inválida: " + index);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry(pages.get(index));

            if(entry != null){
                try (InputStream is = zip.getInputStream(entry)) {
                    Logger.info("Loaded page: " + pages.get(index));
                    return new Image(is);
                }
            }else{
                return null;
            }

        } catch (IOException e) {
            Logger.error("Error cargando imagen: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean hasPages() {
        loadPages();
        return !this.pages.isEmpty();
    }
    

    @Override
    public String getName() {
        return this.name;
    }
    
}
