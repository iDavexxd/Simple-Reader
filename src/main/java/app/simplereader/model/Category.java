package app.simplereader.model;

import app.simplereader.views.CategoryPane;
import app.simplereader.repository.Manga;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class Category {
    
    private String Name;
    private List<Manga> mangaList = new ArrayList<>();
    private CategoryPane pane = new CategoryPane(Name, mangaList);
    
    
    public Category(String name){
        this.Name = name;
    } // Constructor ahre que comentaba
    
    public List<Manga> getMangas(){
        return this.mangaList;
    }
    
    public void addManga(Manga manga){
        this.mangaList.add(manga);
    }
    
    public CategoryPane getPane(){
        return this.pane;
    }
    
    public String getName(){
        return this.Name;
    }
    
}
