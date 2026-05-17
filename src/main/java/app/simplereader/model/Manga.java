
package app.simplereader.model;

import app.simplereader.repository.MangaSource;
import java.util.List;

/**
 *
 * @author david
 */
public class Manga {
    
    private MangaSource source;
    
    private String id;
    
    
    
    private List<Chapter> chapters;
    
    
    public Manga(MangaSource source, String id){
        this.source = source;
        this.id = id;        
    }
    
    public String getTitle(){
        return source.getMangaTitle(this.id);
    }
    
    public String getAuthor(){
        return source.getMangaAuthor(this.id);
    }
    
    public List<Chapter> getChapters(){
        return source.getChapters(this.id);
    }
}
