package app.simplereader.controller;

import app.simplereader.model.AppConfig;
import app.simplereader.controller.Logger;
import javafx.stage.Stage;
import app.simplereader.repository.AppScene;
import java.lang.classfile.Opcode;

/**
 *
 * @author david
 */
public class SceneController {
    
    private static String ActualSceneName;
    private static AppScene ActualScene, LastScene;
    
    private final Stage stage;
    private String css;
    private SceneController instance;

    public SceneController getInstance() {
        return instance;
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
    
    
    public void goTo(AppScene s, AppScene last){
        ActualSceneName = s.getName();
        LastScene = last;
        ActualScene = s;
        stage.setTitle(AppConfig.get().APP_TITLE+ " - "+ActualSceneName);
        stage.setScene(s.getScene());
        Logger.info("Scene --> "+s.getName());
    }
    
    public void backScene(){
        goTo(LastScene, ActualScene);
    }

    public String getCss() {
        return css;
    }
    
    public Stage getStage(){
        return this.stage;
    }
    
}
