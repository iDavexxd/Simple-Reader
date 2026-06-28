package app.simplereader.model;

import app.simplereader.repository.MangaSource;
import java.util.List;

/**
 *
 * @author david
 */
public class Manga {
    //datos
    private String mangaID,
            sourceID,title,author,description,coverURL;
    private List<String> tags;
    //Capitulos
    private List<Chapter> chapters;
    
    public Manga(String mangaID,String source){
       this.sourceID = source; 
       this.mangaID = mangaID;
    }
   
    /*
        Getters:
    */
    public String getSourceID(){
        return this.sourceID;
    }
    
    public String getMangaID(){
        return this.mangaID;
    }
    
    public String getUniqueID(){
        return this.sourceID + "|" + this.mangaID;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Manga manga = (Manga) obj;
        return (this.mangaID != null && this.mangaID.equals(manga.mangaID)) &&
               (this.sourceID != null && this.sourceID.equals(manga.sourceID));
    }

    @Override
    public int hashCode() {
        int result = mangaID != null ? mangaID.hashCode() : 0;
        result = 31 * result + (sourceID != null ? sourceID.hashCode() : 0);
        return result;
    }
    
    public String getTitle(){
        return this.title;
    }
    
    public String getAuthor(){
        return this.author;
    }
    
    public String getDescription(){
        return this.description;
    }
    
    public List<String> getTags(){
        return this.tags;
    }
    
    public String getCoverURL(){
        return this.coverURL;
    }
    
    public List<Chapter> getChapters(){
        return this.chapters;
    }
    
    /*
    Setters:
    */
    public void setTitle(String title){
        this.title = title;
    }
    
    public void setAuthor(String author){
        this.author = author;
    }
    public void setDescription(String desc){
        this.description = desc;
    }
    
    public void setTags(List<String> tags){
        this.tags = tags;
    }
    public void setCoverURL(String url){
        this.coverURL = url;
    }
    
    public void setChapters(List<Chapter> chapters){
        this.chapters = chapters;
    }
    
}