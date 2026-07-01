package app.simplereader.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import app.simplereader.repository.MangaSource;

public class Http {
    /**
     * Creates and configures an HttpURLConnection for a given URL and source.
     * Applies the source's User-Agent (if available), otherwise default User-Agent.
     */
    public static java.net.URLConnection getConnection(String urlStr, MangaSource source) throws Exception {
        java.net.URLConnection conn = new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        if (conn instanceof HttpURLConnection) {
            String customUA = null;
            if (source != null) {
                customUA = source.getUserAgent();
                if (customUA == null && source.getImageHeaders() != null) {
                    for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                        if (entry.getKey().equalsIgnoreCase("User-Agent")) {
                            customUA = entry.getValue();
                            break;
                        }
                    }
                }
            }
            
            // Priorizamos el UA de la extensión. Si no existe, usamos el por defecto.
            String uaToUse = (customUA != null && !customUA.trim().isEmpty()) ? customUA : app.simplereader.model.AppConfig.get().USER_AGENT;
            conn.setRequestProperty("User-Agent", uaToUse);
            
            // Aplicamos los demás headers, ignorando un posible "User-Agent" que ya procesamos
            if (source != null && source.getImageHeaders() != null) {
                for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                    if (!entry.getKey().equalsIgnoreCase("User-Agent")) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        
        return conn;
    }
    
    /**
     * Intenta obtener el InputStream. Si falla (ej. error 400 por bloqueo del servidor de imagenes),
     * reintenta usando el User-Agent por defecto (Chrome).
     */
    public static java.io.InputStream getInputStreamWithRetry(String urlStr, MangaSource source) throws Exception {
        try {
            // El primer intento usará el UA de la extensión si existe
            java.net.URLConnection conn = getConnection(urlStr, source);
            return conn.getInputStream();
        } catch (Exception e) {
            app.simplereader.service.Logger.info("Error en la primera conexión. Reintentando con User-Agent por defecto de Chrome...");
            
            // Reintento: Forzamos el uso del User-Agent de AppConfig
            java.net.URLConnection conn2 = new URL(urlStr).openConnection();
            conn2.setConnectTimeout(5000);
            conn2.setReadTimeout(10000);
            conn2.setRequestProperty("User-Agent", app.simplereader.model.AppConfig.get().USER_AGENT);
            
            if (source != null && source.getImageHeaders() != null) {
                for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                    if (!entry.getKey().equalsIgnoreCase("User-Agent")) {
                        conn2.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            return conn2.getInputStream();
        }
    }
}
