package app.simplereader.network;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

/**
 * Interceptor de OkHttp diseñado para detectar bloqueos de Cloudflare.
 */
public class CloudflareInterceptor implements Interceptor {

    // Esta interfaz servirá para avisarle a tu interfaz gráfica (JavaFX) que debe abrir el WebView
    public interface CloudflareCallback {
        void onCloudflareChallenge(String url);
    }

    private final CloudflareCallback callback;

    public CloudflareInterceptor(CloudflareCallback callback) {
        this.callback = callback;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        
        // Dejamos que OkHttp haga la petición normal
        Response response = chain.proceed(request);

        // Si la página devuelve 403 (Forbidden) o 503 (Service Unavailable), es sospechoso.
        if (response.code() == 403 || response.code() == 503) {
            String serverHeader = response.header("Server");
            
            // Verificamos si la cabecera del servidor confirma que es Cloudflare o DDoS-Guard
            if (serverHeader != null && (serverHeader.equalsIgnoreCase("cloudflare") || serverHeader.equalsIgnoreCase("ddos-guard"))) {
                
                // ¡Alarma! Disparamos el evento para que la App abra el WebView
                if (callback != null) {
                    callback.onCloudflareChallenge(request.url().toString());
                }
            }
        }
        
        return response;
    }
}
