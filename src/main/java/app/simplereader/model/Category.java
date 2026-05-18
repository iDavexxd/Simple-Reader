package app.simplereader.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class Category {
    private final String name;
    
    private List<Manga> mangaList;
    
    public Category(String name){
        this.name = name;
        mangaList = new ArrayList<>();
    }
    
    public String getName(){
        return name;
    }
    
    public List<Manga> getMangas(){
        return mangaList;
    }
    
    public void addManga(Manga manga){
        if (!mangaList.contains(manga)) {
            mangaList.add(manga);
        }
    }
    
    public void removeManga(Manga manga){
        mangaList.remove(manga);
    }
}
