package app.simplereader.service;

import app.simplereader.repository.MangaSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class PageLoader {
    
    private static PageLoader instance;
    private volatile boolean disposed = false;
    private volatile int currentGeneration = 0;
    
    private String[] imageURL;
    private MangaSource source;
    private final Cache cache = Cache.getInstance();
    private final ImageManager imageManager = ImageManager.getInstance();
    
    private final Set<String> loadingPages = ConcurrentHashMap.newKeySet();
    private final Map<String, Consumer<Image>> pendingCallbacks = new ConcurrentHashMap<>();
    
    private final ExecutorService preloader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "current-loader");
        t.setDaemon(true);
        return t;
    });
        
    private PageLoader(){
        
    }
    
    public static PageLoader getInstance(){
        if(instance == null) instance = new PageLoader();
        return instance;
    }
    
    public void setImageUrlList(List<String> urls){
        this.disposed = false;
        this.currentGeneration++;
        this.imageURL = (urls != null) ? urls.toArray(String[]::new) : new String[0];
    }
    
    public void getImage(int index, Consumer<Image> onLoaded) {
        if (imageURL == null || index < 0 || index >= imageURL.length) return;
        
        loadImage(index, onLoaded);
        preload(index);
    }
    
    public void preload(int index) {
        // padelante
        if (index + 1 < imageURL.length) loadImage(index + 1, null);
        if (index + 2 < imageURL.length) loadImage(index + 2, null);

        //Patras
        if (index - 1 >= 0) loadImage(index - 1, null);
        if (index - 2 >= 0) loadImage(index - 2, null);
    }
    
    private void loadImage(int index, Consumer<Image> onComplete) {
        if (imageURL == null || index < 0 || index >= imageURL.length) {
            return;
        }

        String url = imageURL[index];
        if (url == null) {
            return;
        }

        // 1. Si ya está en caché, entregamos y salimos
        Image cached = cache.getPagesLRU().getIfPresent(url);
        if (cached != null) {
            if (onComplete != null) {
                onComplete.accept(cached);
            }
            return;
        }

        // 2. Si alguien está esperando esta imagen (página actual), registramos su callback
        if (onComplete != null) {
            pendingCallbacks.put(url, onComplete);
        }

        // 3. Si ya se está descargando (ej. por una precarga previa), salimos;
        // en cuanto termine, el hilo tomará el callback que acabamos de registrar
        if (!loadingPages.add(url)) {
            return;
        }

        final int taskGeneration = this.currentGeneration;

        preloader.submit(() -> {
            Image image = null;
            try {
                if (disposed || taskGeneration != currentGeneration) return;

                image = imageManager.loadImage(url, source, null);

                if (disposed || taskGeneration != currentGeneration) return;

                if (image != null && !image.isError()) {
                    cache.getPagesLRU().put(url, image);
                }
            } catch (Exception e) {
                image = null;
            } finally {
                loadingPages.remove(url);

                Consumer<Image> callback = pendingCallbacks.remove(url);
                if (callback != null) {
                    final Image finalImage = image;
                    javafx.application.Platform.runLater(() -> {
                        if (disposed || taskGeneration != currentGeneration) return;
                        callback.accept(finalImage);
                    });
                }
                
                Logger.info("Loaded image: "+url);
            }
        });
    }
    
    public void setSource(MangaSource newSource){
        this.source = newSource;
    }
    
    public void clear(){
        this.disposed = true;
        this.currentGeneration++;
        loadingPages.clear();
        pendingCallbacks.clear();
        cache.getPagesLRU().invalidateAll();
    }
}
