package app.simplereader.repository;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import java.util.List;

/**
 *
 * @author david
 */
public interface MangaSource {   
    
    String getID();
    String getName();
    String getLang();
    
    String getCoverURL(String MangaID);
    List<Manga> searchManga(String query);
    List<Chapter> getChapters(String mangaID);
    List<String> getPages(String MangaID,String chapterID);
    
    
    void fetchMangaData(Manga manga);
    
    /**
     * Returns HTTP headers required to load images from this source.
     * Override this in sources that need custom headers (e.g. Referer).
     * Return null or empty map if no special headers are needed.
     * @return 
     */
    default java.util.Map<String, String> getImageHeaders() {
        return null;
    }
    
    default String getUserAgent(){
        return null;
    }
        
}