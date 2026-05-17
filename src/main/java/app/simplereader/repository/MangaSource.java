package app.simplereader.repository;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import java.util.List;

/**
 *
 * @author david
 */
public interface MangaSource {   
    
    
    String getMangaName();
    
    String getMangaID();
    String getMangaTitle(String id);
    String getMangaAuthor(String id);
    String getMangaDescription(String id);
    
    
    
    List<Chapter> getChapters(String id);
    
    List<String> getPages(ChapterInterface chapter);
    
}
