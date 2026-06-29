package app.simplereader.repository;

import okhttp3.OkHttpClient;
import java.util.List;

/**
 *
 * @author david
 */
public abstract class AppExtension {
    
    // Todas las extensiones heredarán este cliente global (OkHttp)
    protected final OkHttpClient client = GlobalNetwork.getInstance().getClient();
    
    public abstract String getName();

    public abstract List<MangaSource> getSources();
    
    public String getVersion() {
        return "Unknown";
    }
    
    public String getAuthor() {
        return "Unknown";
    }
}
