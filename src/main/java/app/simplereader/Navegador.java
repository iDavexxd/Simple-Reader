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
        stage.setScene(mainScene);
    }
    
    
    public void goTo(Navigable s){
        RootPane.getChildren().setAll(s.getParent());
        stage.setTitle(AppConfig.get().APP_TITLE+ " - "+s.getName());
        if (AppConfig.get().readerfullscreen && !stage.isFullScreen()) {
        stage.setFullScreen(true);
        }
        Logger.info("Scene --> "+s.getName());
    }
}
