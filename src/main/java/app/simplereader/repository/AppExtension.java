package app.simplereader.repository;

import java.net.http.HttpClient;
import java.util.List;

/**
 *
 * @author david
 */
public abstract class AppExtension {
    
    protected final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    
    public abstract String getName();

    public abstract List<MangaSource> getSources();
    
}
