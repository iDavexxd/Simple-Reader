package app.simplereader.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author david
 */
public interface MangaInterface {
    
    String getTitle();
    String getAuthor();
    String getDescription();
    List<String> getTags();
    List<ChapterInterface> getChapters();
    String getCover();
    MangaSource getSource();
    
    Set<String> getReadedChapters();
    HashMap<String,Integer> getChapterLastPage();
    
    List<ChapterInterface> getReaded();
    List<ChapterInterface> getUnreaded();
    
    String getCategory();
    
    
    void setCategory(String c);
    void saveData();
}
