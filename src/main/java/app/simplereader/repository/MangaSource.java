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
    String getCoverURL(String MangaID);
    List<Manga> searchManga(String query);
    List<Chapter> getChapters(String mangaID);
    List<String> getPages(String MangaID,String chapterID);
    
    
    void fetchMangaData(Manga manga);
    
    
        
}