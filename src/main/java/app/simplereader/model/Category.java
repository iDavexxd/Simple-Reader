package app.simplereader.model;

import app.simplereader.views.CategoryPane;
import java.util.ArrayList;
import java.util.List;
import app.simplereader.repository.MangaInterface;

/**
 *
 * @author david
 */
public class Category {
    
    private String Name;
    private List<MangaInterface> mangaList = new ArrayList<>();
    private CategoryPane pane = new CategoryPane(Name, mangaList);
    
    
    public Category(String name){
        this.Name = name;
    } // Constructor ahre que comentaba
    
    public List<MangaInterface> getMangas(){
        return this.mangaList;
    }
    
    public void addManga(MangaInterface manga){
        this.mangaList.add(manga);
    }
    
    public CategoryPane getPane(){
        return this.pane;
    }
    
    public String getName(){
        return this.Name;
    }
    
}
