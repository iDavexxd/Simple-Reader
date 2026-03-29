package app.simplereader;

import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author iDavexX
 */
public class SimpleReader extends Application{
    
    @Override
    public void start(Stage stage) {
        
        Image image = new Image("file:/home/david/Imágenes/16b345f8-09e0-4160-ad0b-7a5b511e5501.jpeg");
        ImageView visor = new ImageView(image);
        
        Button btnNext = new Button("->");
        Button btnBack = new Button("<-");
        btnNext.setOnAction(e -> {
            Logger.Log("Cliqueaste siguiente!");
        });
        btnBack.setOnAction(e -> Logger.Log("Cliqueaste volver!"));
        
        visor.setFitHeight(400);
        visor.setFitWidth(600);
        visor.setPreserveRatio(true);
        
        BorderPane layout = new BorderPane();
        layout.setCenter(visor);
        layout.setLeft(btnBack);
        layout.setRight(btnNext);
        
        
        Scene escena = new Scene(layout,1280,720);
               
        stage.setTitle("Simple Reader");
        stage.setScene(escena);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
        Logger.Log("XD");
    }
}
