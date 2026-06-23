package app.simplereader.model;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author david
 */
public class Category {
    private final String name;
    
    private final HashMap<String, Manga> mangas;
    private boolean hide = false;
    
    public Category(String name){
        this.name = name;
        this.mangas = new HashMap<>();
    }
    
    public boolean isHide(){
        return this.hide;
    }
    
    public void setHide(boolean bool){
        this.hide = bool;
    }
    
    public String getName(){
        return this.name;
    }
    
    public HashMap<String, Manga> getMangas(){
        return this.mangas;
    }
    
    public void addManga(Manga manga){
        if(!this.mangas.containsKey(manga.getUniqueID())){
            this.mangas.put(manga.getUniqueID(), manga);
        }
    }
    
    public void removeManga(Manga manga){
        this.mangas.remove(manga.getUniqueID());
    }
}
