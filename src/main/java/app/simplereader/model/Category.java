package app.simplereader.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class Category {
    private final String name;
    
    private final List<Manga> mangaList;
    private boolean hide = false;
    
    public Category(String name){
        this.name = name;
        mangaList = new ArrayList<>();
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
    
    public List<Manga> getMangas(){
        return this.mangaList;
    }
    
    public void addManga(Manga manga){
        if (!this.mangaList.contains(manga)) {
            this.mangaList.add(manga);
        }
    }
    
    public void removeManga(Manga manga){
        this.mangaList.remove(manga);
    }
}
