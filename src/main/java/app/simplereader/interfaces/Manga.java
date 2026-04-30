package app.simplereader.interfaces;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author david
 */
public interface Manga {
    String getTitle();
    String getAuthor();
    String getDescription();
    List<String> getTags();
    List<Chapter> getChapters();
    String getCover();
    
    Set<String> getReadedChapters();
    HashMap<String,Integer> getChapterLastPage();
    
    List<Chapter> getReaded();
    List<Chapter> getUnreaded();
    
    void saveData();
}
