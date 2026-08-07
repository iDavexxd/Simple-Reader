package app.simplereader.views.components;

import app.simplereader.repository.GlobalNetwork;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public class WebViewWindow extends Stage {

    private final WebView webView;
    private final String urlToLoad;
    private static CookieManager javaNetCookieManager;

    public WebViewWindow(String url) {
        this.urlToLoad = url;

        // Configurar el manejador global de cookies de JavaFX si no existe
        if (CookieHandler.getDefault() == null || !(CookieHandler.getDefault() instanceof CookieManager)) {
            javaNetCookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(javaNetCookieManager);
        } else {
            javaNetCookieManager = (CookieManager) CookieHandler.getDefault();
        }

        webView = new WebView();
        WebEngine engine = webView.getEngine();
        
        // Mismo User-Agent que OkHttp para evitar sospechas de Cloudflare
        engine.setUserAgent("SimpleReader/beta-1.8");

        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 900, 700);
        this.setScene(scene);
        this.setTitle("Resolviendo Cloudflare - SimpleReader");
        
        // Bloquea la app por debajo para que el usuario se enfoque en resolver el captcha
        this.initModality(Modality.APPLICATION_MODAL); 

        // Escuchar cada vez que la página cambia o termina de cargar
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                checkCookiesAndClose();
            }
        });

        // Forzar limpieza de WebKit al cerrar la ventana para liberar RAM
        this.setOnCloseRequest(e -> {
            engine.load(null);                // Descargar la página
            engine.loadContent("");           // Vaciar el DOM
            webView.getEngine().load(null);   // Doble limpieza
            System.gc();                      // Sugerir al Garbage Collector que limpie ahora
        });

        engine.load(url);
    }

    private void checkCookiesAndClose() {
        HttpUrl parsedUrl = HttpUrl.parse(urlToLoad);
        if (parsedUrl == null) return;

        String targetDomain = parsedUrl.host();
        boolean foundClearance = false;

        // Extraer cookies del navegador interno de JavaFX
        List<HttpCookie> javafxCookies = javaNetCookieManager.getCookieStore().getCookies();
        List<Cookie> okHttpCookies = new ArrayList<>();

        for (HttpCookie jc : javafxCookies) {
            String cookieDomain = jc.getDomain();
            if (cookieDomain != null) {
                // Limpiar el punto inicial que JavaFX a veces añade (ej. ".mangadex.org")
                if (cookieDomain.startsWith(".")) {
                    cookieDomain = cookieDomain.substring(1);
                }

                // Verificar si la cookie pertenece a la página que estamos cargando
                if (targetDomain.endsWith(cookieDomain)) {
                    if ("cf_clearance".equals(jc.getName())) {
                        foundClearance = true;
                    }
                    
                    // Traducir la cookie de JavaFX al formato de OkHttp
                    Cookie.Builder builder = new Cookie.Builder()
                            .name(jc.getName())
                            .value(jc.getValue())
                            .domain(cookieDomain);
                    
                    if (jc.getPath() != null) builder.path(jc.getPath());
                    if (jc.getSecure()) builder.secure();
                    if (jc.isHttpOnly()) builder.httpOnly();
                    // Construimos la cookie y la guardamos en la lista
                    okHttpCookies.add(builder.build());
                }
            }
        }

        // Si encontró la cookie de Cloudflare, la guardamos y cerramos la ventana
        if (foundClearance) {
            // Guardar en nuestro DiskCookieManager mágicamente
            GlobalNetwork.getInstance().getClient().cookieJar().saveFromResponse(parsedUrl, okHttpCookies);
            System.out.println("¡Cookie cf_clearance capturada exitosamente para " + targetDomain + "!");
            
            // Cerrar la ventana emergente automáticamente
            Platform.runLater(this::close);
        }
    }
}
