package app.simplereader.controller;

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

        try 
        {
            css = getClass().getResource("/style.css").toExternalForm();
            Logger.info("Loaded syle.css");
        } 
        catch (Exception e) 
        {
                Logger.error("No se pudo cargar el archivo css: "+e.getMessage());
        }
    }
    
    public void goTo(AppScene s){
        ActualSceneName = s.getName();
        
        stage.setTitle(AppConfig.get().APP_TITLE+ " - "+ActualSceneName);
        stage.setScene(s.getScene());
        scnList.add(s);
        Logger.info("Scene --> "+s.getName());
    }
    
    public void backScene(){
        if (scnList.size() < 2) return; // evita crash si no hay dónde volver
        int indice = scnList.size() - 1;
        scnList.remove(indice);                              // elimina la actual primero
        AppScene anterior = scnList.get(scnList.size() - 1);
        ActualSceneName = anterior.getName();
        
        stage.setTitle(AppConfig.get().APP_TITLE + " - " + ActualSceneName);
        stage.setResizable(false);
        stage.setScene(anterior.getScene());
        Logger.info("Scene --> " + anterior.getName());
    }

    public String getCss() {
        return css;
    }
    
    public Stage getStage(){
        return this.stage;
    }
    
}