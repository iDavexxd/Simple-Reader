package app.simplereader;

import app.simplereader.interfaces.Navigable;
import javafx.stage.Stage;

/**
 *
 * @author david
 */
public class Navegador {
    
    private final Stage stage;
        
    public Navegador(Stage stage) {
        this.stage = stage;
    }
    
    
    public void goTo(Navigable s){
        stage.setTitle(AppConfig.TITTLE+ " - "+s.getName());
        stage.setScene(s.getScene());
        Logger.info("Scene --> "+s.getName());
    }
}
