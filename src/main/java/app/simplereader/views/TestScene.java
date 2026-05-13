package app.simplereader.views;

import app.simplereader.views.ScnMangaMenu;
import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import app.simplereader.repository.Manga;
import app.simplereader.model.mdManga;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import app.simplereader.repository.AppScene;


public class TestScene implements AppScene{
    private final SceneController nav;
    
    
    public TestScene(SceneController nav){
        this.nav = nav;
    }

    @Override
    public Scene getScene() {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        TextField texto = new TextField("si");
        root.setCenter(texto);
        
        Button boton = new Button("Leer");
        boton.setOnAction(e -> {
            nav.goTo(new ScnMangaMenu(nav, new mdManga(texto.getText())));
        });
        root.setBottom(boton);
        
        
        return scene;
    }

    @Override
    public String getName() {
        return "asd";
    }
    @Override
    public String getParentName(){
        return "Reader";
    }
    
    
}
