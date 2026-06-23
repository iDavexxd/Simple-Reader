package app.simplereader.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class Cache {
    
    private static Cache instance;
    private final com.github.benmanes.caffeine.cache.Cache<String, Image> sharedCache;
    private final java.util.Map<String, Image> pagesLRU;
    private final com.github.benmanes.caffeine.cache.Cache<String, Image> coverMenuCache;
    
    private Cache(){
        // Configuramos el caché compartido de Caffeine.
        sharedCache = Caffeine.newBuilder()
            .maximumSize(30) 
            // Esto obliga a Caffeine a procesar las limpiezas en el mismo hilo (síncrono), 
            // haciéndolo reaccionar un poco más rápido.
            .executor(Runnable::run)
            // Cuando Caffeine saca una imagen de la memoria, nos aseguramos de cancelar su carga si estaba pendiente
            .removalListener((String key, Image image, RemovalCause cause) -> {
                if (image != null) {
                    image.cancel(); 
                }
            })
            .build();
            
        // Caché estricto LRU para páginas del lector
        pagesLRU = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, Image>(10, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, Image> eldest) {
                if (size() > 10) {
                    if (eldest.getValue() != null) eldest.getValue().cancel();
                    return true;
                }
                return false;
            }
        });
            
        // Caché para la imagen de portada en máxima calidad (ScnMangaMenu CoverMenu)
        coverMenuCache = Caffeine.newBuilder()
            .maximumSize(3)
            .executor(Runnable::run)
            .removalListener((String key, Image image, RemovalCause cause) -> {
                if (image != null) {
                    image.cancel(); 
                }
            })
            .build();
    }
    
    public static Cache getInstance(){
        if (instance == null) instance = new Cache();
        return instance;
    }
    
    public com.github.benmanes.caffeine.cache.Cache<String, Image> getSharedCache() {
        return sharedCache;
    }
    
    public java.util.Map<String, Image> getPagesLRU() {
        return pagesLRU;
    }
    
    public com.github.benmanes.caffeine.cache.Cache<String, Image> getCoverMenuCache() {
        return coverMenuCache;
    }
}
