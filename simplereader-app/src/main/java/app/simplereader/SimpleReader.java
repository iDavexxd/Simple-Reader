package app.simplereader;

import app.simplereader.controller.LibraryController;
import app.simplereader.controller.SceneController;
import app.simplereader.service.Logger;
import app.simplereader.controller.SourceManager;
import app.simplereader.model.LocalSource;
import app.simplereader.views.ScnMainMenu;
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
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Bold.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Lato-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Lato-Bold.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Bold.ttf"), 14);
        
        // Registrar sources
        SourceManager.getInstance().registerSource(new LocalSource());
        SourceManager.getInstance().loadSources();
        SceneController.doInstance(stage);
        SceneController.getInstance().goTo(new ScnMainMenu());
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            LibraryController.getInstance().saveLibrary();
            });
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app_icon.png")));
        stage.show();
        stage.setResizable(true);
    }
        
    public static void main(String[] args) {
        launch(args);
        Logger.info("But c programa misterioso se cierra");
        Logger.info("XD");
    }
}
