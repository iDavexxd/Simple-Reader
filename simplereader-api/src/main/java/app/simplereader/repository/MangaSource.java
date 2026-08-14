package app.simplereader.repository;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import java.util.List;
import java.util.Map;

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
    
    default Manga[] getFilterManga(){
        return null;
    }
    void fetchMangaData(Manga manga);
    
    
    default Map<String, String> getImageHeaders() {
        return null;
    }
    
    default okhttp3.OkHttpClient getClient() {
            return GlobalNetwork.getInstance().getClient();
    }
    
    default String getUserAgent(){
        return null;
    }
        
}