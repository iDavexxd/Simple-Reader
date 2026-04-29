package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Manga;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.mdManga;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;


public class TestScene implements Navigable{
    private final Navegador nav;
    
    
    public TestScene(Navegador nav){
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
