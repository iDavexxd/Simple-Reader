package app.simplereader;

import app.simplereader.interfaces.Navigable;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author david
 */
public class Navegador {
    
    private final Stage stage;
    private final StackPane RootPane;
    
    public Navegador(Stage stage) {
        this.stage = stage;
        this.RootPane = new StackPane();
        Scene mainScene = new Scene(RootPane, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        try 
        {
            String css = getClass().getResource("/style.css").toExternalForm();
            mainScene.getStylesheets().add(css);
            Logger.info("Loaded syle.css");
        } 
        catch (Exception e) 
        {
                Logger.error("No se pudo cargar el archivo css: "+e.getMessage());
        }
        stage.setScene(mainScene);
    }
    
    
    public void goTo(Navigable s){
        RootPane.getChildren().setAll(s.getParent());
        stage.setTitle(AppConfig.get().APP_TITLE+ " - "+s.getName());        
        Logger.info("Scene --> "+s.getName());
        
    }
}
