package app.simplereader.model;

import app.simplereader.controller.Logger;
import app.simplereader.controller.Sorter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.image.Image;
import app.simplereader.repository.MangaInterface;
import app.simplereader.repository.ChapterInterface;

/**
 *
 * @author david
 */
public class FolderChapter implements ChapterInterface{
    
    private final File folder;
    private final List<File> pages = new ArrayList<>();
    private final String name;
    private final MangaInterface manga;
    private int lastRead;
    
    public FolderChapter(MangaInterface manga,File folder){
        this.folder = folder;
        this.name = folder.getName();
        this.manga = manga;
        
        Integer saved = manga.getChapterLastPage().get(this.name);
        this.lastRead = (saved != null) ? saved : 0; 
    }
    
    private void loadPages(){
        if(!pages.isEmpty()) return;
        // Filtrar imagenes.
        File[] images = folder.listFiles((dir, nombre) -> {
            String lower = nombre.toLowerCase();
            return lower.endsWith(".jpg") ||
                   lower.endsWith(".jpeg") ||
                   lower.endsWith(".png") ||
                   lower.endsWith(".webp");
        });
        if(images == null || images.length == 0) {
            Logger.warning("No hay paginas en: "+folder.getName());
            return;
        }
        //sortear y agregar a la lista
        Arrays.sort(images, (f1, f2) -> 
            Sorter.compare(f1.getName(), f2.getName())
        );
        for (File archivo : images) {
            this.pages.add(archivo);
            Logger.info("Loaded: "+archivo.getName());
        }
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
        return new Image(
            pages.get(index).toURI().toString(),
            AppConfig.get().WIDTH, 0,
            true,
            true,
            true
        );
    }
    @Override
    public boolean hasPages() {
        loadPages();
        return !pages.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getNum() {
        return 0; // o algo que ya tengas
    }
    
    @Override
    public boolean isReaded(){
        return this.manga.getReadedChapters().contains(this.name);
    }
    
    @Override
    public void markAsReaded(){
        if(!isReaded()){
            this.manga.getReadedChapters().add(this.name);
            if(this.manga.getUnreaded().contains(this)){
                this.manga.getUnreaded().remove(this);
                this.manga.getReaded().add(this);
            }
            Logger.info(this.name+" - Leido.");
        }
    }
    
    @Override
    public void setLastRead(int s){
        this.lastRead = s;
        this.manga.getChapterLastPage().put(this.getName(), this.lastRead);
    }
    
    @Override
    public Integer getLastRead(){
        return lastRead;
    }
    
}
