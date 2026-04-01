package app.simplereader;


import app.simplereader.scenes.ScnMainMenu;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


/**
 *
 * @author iDavexX
 */
public class SimpleReader extends Application{
          
    
    @Override
    public void start(Stage stage) {
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Bold.ttf"), 14);
        Navegador nav = new Navegador(stage);
        nav.goTo(new ScnMainMenu(nav));
        if (AppConfig.get().readerfullscreen && !stage.isFullScreen()) {
        stage.setFullScreen(true);
        }
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }
        

    public static void main(String[] args) {
        launch(args);
        Logger.info("XD");
    }


}
