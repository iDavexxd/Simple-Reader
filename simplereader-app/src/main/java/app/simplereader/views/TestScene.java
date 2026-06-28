package app.simplereader.views;

import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import app.simplereader.model.Manga;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import app.simplereader.repository.AppScene;

public class TestScene implements AppScene{
    private final SceneController nav;
    public TestScene(SceneController nav){
        this.nav = nav;
    }

    @Override
    public javafx.scene.Parent getScene() {
        BorderPane root = new BorderPane();
        Label lbl = new Label("Test Scene - MangaDex source pending");
        root.setCenter(lbl);
        
        Button boton = new Button("Volver");
        boton.setOnAction(e -> nav.backScene());
        root.setBottom(boton);
        
        return root;
    }

    @Override
    public String getName() {
        return "Test";
    }
    @Override
    public String getParentName(){
        return "Test";
    }
}
