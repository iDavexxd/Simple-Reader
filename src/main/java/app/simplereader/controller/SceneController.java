package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.AppConfig;
import javafx.stage.Stage;
import app.simplereader.repository.AppScene;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class SceneController {
    
    private static String ActualSceneName;
   
    private List<AppScene> scnList = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 5;
    
    private final Stage stage;
    private String css;
    
    private static SceneController instance;
    
    public static SceneController getInstance() {
        return instance;
    }
    
    public static void doInstance(Stage stage){
        instance = new SceneController(stage);
    }
    
    public SceneController(Stage stage) {
        this.stage = stage;

        try {
            css = getClass().getResource("/style.css").toExternalForm();
            Logger.info("Loaded style.css");
        } catch (Exception e) {
            Logger.error("No se pudo cargar el archivo css: "+e.getMessage());
        }
    }
    
    public void goTo(AppScene s){
        ActualSceneName = s.getName();
        
        double w = stage.getWidth();
        double h = stage.getHeight();
        
        stage.setTitle(AppConfig.get().APP_TITLE + " - " + ActualSceneName);
        stage.setScene(s.getScene());
        
        if (!Double.isNaN(w) && !Double.isNaN(h) && !stage.isFullScreen() && !stage.isMaximized()) {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        
        // --- INICIO DE BÚSQUEDA PROFUNDA EN EL HISTORIAL ---
        int existingIndex = -1;
        for (int i = 0; i < scnList.size(); i++) {
            if (scnList.get(i).getName().equals(s.getName())) {
                existingIndex = i;
                break;
            }
        }
        
        if (existingIndex != -1) {
            scnList.subList(existingIndex, scnList.size()).clear();
        }
        
        scnList.add(s);
        
        if (scnList.size() > MAX_HISTORY_SIZE) {
            scnList.remove(0); 
        }
        // --- FIN DE BÚSQUEDA PROFUNDA ---
        
        Logger.info("Scene --> " + s.getName());
    }
    
    public void clearHistory() { 
        if (scnList.size() > 1) {
            AppScene currentActiveScene = scnList.get(scnList.size() - 1);
            scnList.clear(); 
            scnList.add(currentActiveScene); 
            Logger.info("Historial de escenas limpiado.");
        }
    }
    
    public void backScene(){
        if (scnList.size() < 2) return; 
        
        int indice = scnList.size() - 1;
        scnList.remove(indice);                              
        AppScene anterior = scnList.get(scnList.size() - 1);
        ActualSceneName = anterior.getName();
        
        double w = stage.getWidth();
        double h = stage.getHeight();
        
        stage.setTitle(AppConfig.get().APP_TITLE + " - " + ActualSceneName);
        stage.setScene(anterior.getScene());
        
        if (!Double.isNaN(w) && !Double.isNaN(h) && !stage.isFullScreen() && !stage.isMaximized()) {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        
        Logger.info("Scene --> " + anterior.getName());
    }

    public String getCss() {
        return css;
    }
    
    public Stage getStage(){
        return this.stage;
    }
    
}