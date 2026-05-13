package app.simplereader;


import app.simplereader.controller.SceneController;
import app.simplereader.controller.Logger;
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
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoSlab-Bold.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Lato-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Lato-Bold.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/RobotoMono-Bold.ttf"), 14);
        

        
        SceneController nav = new SceneController(stage);
        nav.goTo(new ScnMainMenu(nav));
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }
        

    public static void main(String[] args) {
        launch(args);
        Logger.info("XD");
    }


}
