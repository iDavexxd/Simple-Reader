package app.simplereader.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import app.simplereader.repository.MangaSource;

public class Http {
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    
    /**
     * Creates and configures an HttpURLConnection for a given URL and source.
     * Applies the default User-Agent and any source-specific headers (like Referer).
     */
    public static java.net.URLConnection getConnection(String urlStr, MangaSource source) throws Exception {
        java.net.URLConnection conn = new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        if (conn instanceof HttpURLConnection) {
            // Default User-Agent
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
            
            // Apply custom headers from the source (can override User-Agent if needed)
            if (source != null && source.getImageHeaders() != null) {
                for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return conn;
    }
}
