package app.simplereader.controller;

import app.simplereader.model.Chapter;
import app.simplereader.views.ScnReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class ReaderController {
    private ScnReader scene;
    private static ReaderController instance;
    private SceneController nav = SceneController.getInstance();
    
    
    private final Map<Integer, Image> cache = java.util.Collections.synchronizedMap(new HashMap<>());
    
    private ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "preloader");
        t.setDaemon(true);
        return t;
    });
    
    public ReaderController(ScnReader scene){
        this.scene = scene;
    }
    
    public static void doInstance(ScnReader scene){
        instance = new ReaderController(scene);
    }
    
    public static ReaderController getInstance(){
        return instance;
    }
    
    public void loadChapter(Chapter chapter, int index){
        // 1. PRIMERO: Limpiar todo lo del capítulo anterior
        cache.clear(); 
        preloader.shutdownNow(); // Detiene las descargas en segundo plano
        // Reinicia el pool de hilos para el nuevo capítulo
        preloader = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "preloader");
            t.setDaemon(true);
            return t;
        });
        // 2. AHORA: Configurar el nuevo capítulo
        scene.setChapter(chapter);
        this.chapternum = index;
        this.indiceactual = chapter.isReaded() ? 0 : chapter.getLastRead();
        
        nav.getStage().setTitle(scene.getName());
        
        // Actualizar el ComboBox de páginas si existe
        if (pagina != null) {
            pagina.getItems().clear();
            for (int i = 1; i <= chapter.getPageCount(); i++) {
                pagina.getItems().add(i);
            }
            pagina.setValue(indiceactual + 1);
        }
        // 3. Cargar la imagen actual y precargar las siguientes
        if (totalPages() > 0) {
            LoadImage();
        }
    }
    
}
