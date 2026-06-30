package app.simplereader.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class Cache {
    
    private static Cache instance;
    private final com.github.benmanes.caffeine.cache.Cache<String, Image> sharedCache;
    private final SimpleLRUCache pagesLRU;
    private final com.github.benmanes.caffeine.cache.Cache<String, Image> coverMenuCache;
    
    private Cache(){
        // Configuramos el caché compartido de Caffeine.
        sharedCache = Caffeine.newBuilder()
            .maximumSize(100) 
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .scheduler(Scheduler.systemScheduler())
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
            
        // Caché estricto LRU para páginas del lector (Implementación propia para 100% Strict LRU)
        pagesLRU = new SimpleLRUCache(10);
            
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
    
    public SimpleLRUCache getPagesLRU() {
        return pagesLRU;
    }
    
    public static class SimpleLRUCache {
        private final int maxSize;
        private final java.util.LinkedHashMap<String, Image> map;

        public SimpleLRUCache(int maxSize) {
            this.maxSize = maxSize;
            // accessOrder = true asegura un LRU estricto basado en accesos recientes
            this.map = new java.util.LinkedHashMap<>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Image> eldest) {
                    if (size() > maxSize) {
                        if (eldest.getValue() != null) eldest.getValue().cancel();
                        return true;
                    }
                    return false;
                }
            };
        }

        public synchronized void invalidateAll() {
            for (Image img : map.values()) {
                if (img != null) img.cancel();
            }
            map.clear();
        }

        public synchronized void invalidate(String key) {
            Image img = map.remove(key);
            if (img != null) img.cancel();
        }

        public synchronized Image getIfPresent(String key) {
            return map.get(key);
        }

        public synchronized void put(String key, Image img) {
            map.put(key, img);
        }

        public synchronized Image get(String key, java.util.function.Function<String, Image> loader) {
            Image img = map.get(key);
            if (img == null) {
                img = loader.apply(key);
                if (img != null) map.put(key, img);
            }
            return img;
        }
    }
    
    public com.github.benmanes.caffeine.cache.Cache<String, Image> getCoverMenuCache() {
        return coverMenuCache;
    }
}
