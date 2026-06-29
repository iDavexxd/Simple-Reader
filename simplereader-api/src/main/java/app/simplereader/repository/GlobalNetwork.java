package app.simplereader.repository;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

/**
 * Cliente HTTP global (Singleton) para todas las extensiones.
 */
public class GlobalNetwork {
    
    private static GlobalNetwork instance;
    private OkHttpClient client;

    private GlobalNetwork() {
        // Configuración base de OkHttp (sin cookies por ahora, eso será la Fase 2)
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                // Aquí en el futuro tu App inyectará el cookieJar() y los interceptors()
                .build();
    }

    public static GlobalNetwork getInstance() {
        if (instance == null) {
            instance = new GlobalNetwork();
        }
        return instance;
    }

    public OkHttpClient getClient() {
        return client;
    }

    // Método crucial para que la App principal (simplereader-app) pueda reemplazar 
    // el cliente básico con uno que tenga el CookieJar configurado.
    public void setClient(OkHttpClient customClient) {
        this.client = customClient;
    }
}
