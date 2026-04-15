package app.simplereader.interfaces;

import java.util.List;

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
}
