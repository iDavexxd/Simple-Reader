package app.simplereader.model;

import java.util.List;

/**
 *
 * @author david
 */
public class Chapter {
    
    private String chID;
    private transient Manga manga;
    private String title;
    private String number;
    private transient List<String> pages;
    
    private boolean readed = false;
    private int lastRead = 0;
    
    public Chapter(String id, Manga manga){
        this.chID = id;
        this.manga = manga;
    }
    
    public boolean isReaded(){
        return readed;
    }
    
    public void markAsReaded(){
        this.readed = true;
    }
    
    public void doRead(){
        this.readed = true;
    }
    public void unRead(){
        this.readed = false;
    }
    
    public boolean hasPages(){
        return pages != null && !pages.isEmpty();
    }
    
    public int getPageCount(){
        return pages != null ? pages.size() : 0;
    }
    
    public int getLastRead(){
        return lastRead;
    }
    
    public void setLastRead(int lastRead){
        this.lastRead = lastRead;
    }
    
    /*
    Getters:
    */
    public String getChapterID(){
        return this.chID; 
    }
    
    public String getTitle(){
        return this.title;
    }
    
    public String getNumber(){
        return this.number;
    }
    
    public String getPage(int index){
        return this.pages.get(index);
    }
    
    /*
    Setters:
    */
    
    public void setTitle(String title){
        this.title = title;
    }
    
    public void setNumber(String number){
        this.number = number;
    }
    
    public void setPages(List<String> pages){
        this.pages = pages;
    }
    
    public void setManga(Manga manga){
        this.manga = manga;
    }
}
