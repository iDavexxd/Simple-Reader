package app.simplereader;

import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author iDavexX
 */
public class SimpleReader extends Application{
    
    @Override
    public void start(Stage stage) {
        Button boton = new Button();
        Label lb = new Label("XD");
        boton.setText("Sexoo");
        StackPane panel = new StackPane(boton,lb);
        Scene escena = new Scene(panel,1280,720);
        
        
        stage.setTitle("Simple Reader");
        stage.setScene(escena);
        stage.show();
        stage.setResizable(false);
        
    }

    public static void main(String[] args) {
        launch(args);
        Logger.Log("XD");
    }
}
