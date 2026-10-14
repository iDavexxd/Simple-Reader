package app.simplereader.service;

import app.simplereader.repository.MangaSource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class PageLoader {
    
    private PageLoader instance;
    
    private String[] imageURL;
    private MangaSource source;
    private final Cache cache = Cache.getInstance();
    private final ImageManager imageManager = ImageManager.getInstance();
    
    private final Set<String> loadingPages = ConcurrentHashMap.newKeySet();
    
    private final ExecutorService preloader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "current-loader");
        t.setDaemon(true);
        return t;
    });
        
    private PageLoader(){
        
    }
    
    public PageLoader getInstance(){
        if(instance == null) instance = new PageLoader();
        return instance;
    }
    
    
    public void setImageUrlList(List<String> urls){
        imageURL = urls.toArray(String[]::new);
    }
    
    public void preload(int index){
        
        // Hacia adelante                                                                          
        if (index >= 0 && index < imageURL.length) loadImage(index);
        if (index + 1 < imageURL.length) loadImage(index + 1);
        if (index + 2 < imageURL.length) loadImage(index + 2);
        
        // Hacia atrás                                                                             
        if (index - 1 >= 0) loadImage(index - 1);
        if (index - 2 >= 0) loadImage(index - 2);
    }
    
    private void loadImage(int index) {
        if (imageURL == null || index < 0 || index >= imageURL.length) return;
        
        String url = imageURL[index];
        if (url == null) return;
  
        if (cache.getPagesLRU().getIfPresent(url) != null) {
            return;
        }
  
        if (!loadingPages.add(url)) {
            return; 
        }
  
        preloader.submit(() -> {
            try {
                Image image = imageManager.loadImage(url, source, null);
                if (image != null && !image.isError()) {
                    cache.getPagesLRU().put(url, image);
                }
            } finally {
                loadingPages.remove(url);
            }
        });
    }
    
    public void setSource(MangaSource newSource){
        this.source = newSource;
    }
    
    public void clear(){
        loadingPages.clear();
        cache.getPagesLRU().invalidateAll();
    }
}
