package app.simplereader;

import app.simplereader.interfaces.Navigable;
import javafx.stage.Stage;

/**
 *
 * @author david
 */
public class Navegador {
    
    private static String actualScene;
        
    
    private final Stage stage;
    private String css;
    private Navegador instance;

    public Navegador getInstance() {
        return instance;
    }
    
    public Navegador(Stage stage) {
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
    
    
    public void goTo(Navigable s){
        actualScene = s.getName();        
        stage.setTitle(AppConfig.get().APP_TITLE+ " - "+actualScene);
        stage.setScene(s.getScene());
        Logger.info("Scene --> "+s.getName());
    }

    public String getCss() {
        return css;
    }
    
    public Stage getStage(){
        return this.stage;
    }
    
}
