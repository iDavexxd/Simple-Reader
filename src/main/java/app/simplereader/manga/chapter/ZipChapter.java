
package app.simplereader.manga.chapter;

import app.simplereader.Logger;
import app.simplereader.Sorter;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Manga;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javafx.scene.image.Image;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 *
 * @author david
 */
public class ZipChapter implements Chapter{
    
    private Integer number;
    private final File zipFile;
    private String name;
    private List<String> pages;
    private final Manga manga;    
    private int lastRead;
    
    public ZipChapter(Manga manga,File zipFile){
        this.zipFile = zipFile;
        String fname = zipFile.getName();
        int dot = fname.lastIndexOf('.');
        this.name = (dot != -1) ? fname.substring(0, dot) : fname;
        this.manga = manga;
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
    private void loadMetadata() {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry("ComicInfo.xml");
            if (entry == null) return;

            try (InputStream is = zip.getInputStream(entry)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(is);

                String num = doc.getElementsByTagName("Number").item(0).getTextContent();
                String title = doc.getElementsByTagName("Title").item(0).getTextContent();
                this.number = Integer.parseInt(num.trim());
                this.name = title;
            }
        } catch (Exception e) {
            Logger.warning("No se pudo leer ComicInfo.xml");
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
        if (name == null || number == null) {
            loadMetadata();
        }
        return name;
    }
    @Override
    public int getNum() {
        if (number == null) loadMetadata();
        return number != null ? number : -1;
    }
    
    @Override
    public boolean isReaded(){
        return manga.getReadedChapters().contains(this.name);
    }
    
    @Override
    public void markAsReaded(){
        if(!isReaded())
        {
            manga.getReadedChapters().add(this.name);
            Logger.info(this.name+" - Leido.");
        }
    }
    
    @Override
    public Integer getLastRead(){
        return lastRead;
    }
    
    @Override
    public void setLastRead(int s){
        this.lastRead = s;
        this.manga.getChapterLastPage().put(this.getName(), this.lastRead); 
    }
}
