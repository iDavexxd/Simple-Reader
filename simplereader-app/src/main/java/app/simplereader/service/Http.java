package app.simplereader.service;

import java.util.Map;
import app.simplereader.repository.MangaSource;

public class Http {

    /**
     * Intenta obtener el InputStream. Si falla (ej. error 400 por bloqueo del servidor de imagenes),
     * reintenta usando el User-Agent por defecto (Chrome).
     */
    public static java.io.InputStream getInputStreamWithRetry(String urlStr, MangaSource source) throws Exception {
        if (urlStr.startsWith("file:")) {
            return new java.net.URL(urlStr).openStream();
        }
        
        okhttp3.Response response = getOkHttpResponse(urlStr, source);
        if (!response.isSuccessful()) {
            response.close();
            app.simplereader.service.Logger.info("Error en la primera conexión. Reintentando con User-Agent por defecto de Chrome...");
            
            // Reintento: Forzamos el uso del User-Agent de AppConfig
            okhttp3.OkHttpClient client = (source != null) ? source.getClient() : app.simplereader.repository.GlobalNetwork.getInstance().getClient();
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(urlStr);
            builder.header("User-Agent", app.simplereader.model.AppConfig.get().USER_AGENT);
            
            if (source != null && source.getImageHeaders() != null) {
                for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                    if (!entry.getKey().equalsIgnoreCase("User-Agent")) {
                        builder.header(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            okhttp3.Response response2 = client.newCall(builder.build()).execute();
            if (!response2.isSuccessful()) {
                response2.close();
                throw new Exception("HTTP " + response2.code());
            }
            return response2.body().byteStream();
        }
        return response.body().byteStream();
    }

    /**
     * Obtiene una respuesta de OkHttp usando el cliente de la extensión (si lo hay) o el global.
     */
    public static okhttp3.Response getOkHttpResponse(String urlStr, MangaSource source) throws Exception {
        okhttp3.OkHttpClient client = (source != null) ? source.getClient() : app.simplereader.repository.GlobalNetwork.getInstance().getClient();
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(urlStr);
        
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
        
        String uaToUse = (customUA != null && !customUA.trim().isEmpty()) ? customUA : app.simplereader.model.AppConfig.get().USER_AGENT;
        builder.header("User-Agent", uaToUse);
        
        if (source != null && source.getImageHeaders() != null) {
            for (Map.Entry<String, String> entry : source.getImageHeaders().entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("User-Agent")) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return client.newCall(builder.build()).execute();
    }
}
