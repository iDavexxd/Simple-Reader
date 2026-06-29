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
            
        // Caché estricto LRU para páginas del lector migrado a Caffeine por peso
        com.github.benmanes.caffeine.cache.Cache<String, Image> pagesCache = Caffeine.newBuilder()
            // Límite de peso: por ejemplo, 200 MB en memoria
            .maximumWeight(200)
            .weigher((String key, Image image) -> {
                // Calculamos el peso aproximado de la imagen en MB (ancho * alto * 4 bytes)
                // Aseguramos de que el peso mínimo sea 1 para evitar un peso de 0.
                if (image == null) return 1;
                double width = image.getWidth();
                double height = image.getHeight();
                int weightMB = (int) ((width * height * 4.0) / (1024.0 * 1024.0));
                return Math.max(1, weightMB);
            })
            .executor(Runnable::run)
            .removalListener((String key, Image image, RemovalCause cause) -> {
                if (image != null) image.cancel();
            })
            .build();
            
        // Exponemos el caché de Caffeine como un Map para mantener la compatibilidad 
        // con tu método getPagesLRU() y el resto del código
        pagesLRU = pagesCache.asMap();
            
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
