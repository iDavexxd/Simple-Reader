package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Manga;
import app.simplereader.manga.MangaLoader;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class ScnMainMenu implements Navigable{
    private final Navegador nav;    
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
    }
    
    public VBox crearIcon(Manga manga){
        ImageView coverView = new ImageView();
        coverView.setFitWidth(150);
        coverView.setFitHeight(200);
        coverView.setPreserveRatio(true);
        
        if(manga.getCover() != null){
            Image icon = new Image(manga.getCover().toURI().toString());
            coverView.setImage(icon);
            Logger.info(manga.getTitle()+" - "+manga.getCover().getName()+" --> Loaded");
        }else{
            coverView.setStyle("-fx-background-color: #cccccc;");
        }
        
        
        Label title = new Label(manga.getTitle());
        VBox iconManga = new VBox(5, coverView,title);
        iconManga.setOnMouseClicked(e -> {
            nav.goTo(new ScnMangaMenu(nav,manga));
            Logger.info("Cliqueaste: "+manga.getTitle());
        });
        return iconManga;
    }
    
    @Override
    public Parent getParent(){
        List<Manga> mangas = MangaLoader.loadMangas();
        FlowPane flwpane = new FlowPane();
        flwpane.setHgap(10);
        flwpane.setVgap(10);
        flwpane.setPrefWrapLength(AppConfig.get().WIDTH);
        flwpane.setPadding(new Insets(15));
        
        for(Manga manga : mangas){
            flwpane.getChildren().add(crearIcon(manga));
        }
        ScrollPane scroll = new ScrollPane(flwpane); 
        BorderPane panel = new BorderPane(scroll);
        
        return panel;
    }
    @Override
    public String getName(){
        return "Menu";
    }
}