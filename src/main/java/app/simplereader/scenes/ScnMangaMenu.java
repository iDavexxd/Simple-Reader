/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Chapter;
import app.simplereader.manga.Manga;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class ScnMangaMenu implements Navigable{
    private Manga manga;
    private Navegador nav;
    public ScnMangaMenu(Navegador nav,Manga manga){
        this.manga = manga;
        this.nav = nav;
    }
    
    @Override
    public Scene getScene() {
        List<Chapter> chapter = manga.getChapters();
        ImageView cover = new ImageView();
        cover.setFitWidth(150);
        cover.setFitHeight(200);
        cover.setPreserveRatio(true);

        if (manga.getCover() != null) {
            Image img = new Image(manga.getCover().toURI().toString());
            cover.setImage(img);
        }        
        
        VBox datos = new VBox(10,
            new Label(manga.getTitle()),
            new Label(manga.getAuthor()),
            new Label(manga.getDescription()),
            new Label("Tags: "+getTags())
        ); 
        HBox top = new HBox(20,cover,datos);
        
        ListView<String> listaCaps = new ListView<>();
        for (Chapter cap : manga.getChapters()) {
            listaCaps.getItems().add(cap.getChName());
        }
        
        BorderPane panel = new BorderPane();
        panel.setTop(top);
        panel.setBottom(listaCaps);
        return new Scene(panel,AppConfig.WIDTH,AppConfig.HEIGHT);
    }
    
    private String getTags() {
        if (manga.getTags().isEmpty()) return "";
        return String.join(", ", manga.getTags()); // "accion, aventura, comedia"
    }

    @Override
    public String getName() {
        return manga.getTitle();
    }
    
}
