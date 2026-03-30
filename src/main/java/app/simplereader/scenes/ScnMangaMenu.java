package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Chapter;
import app.simplereader.manga.Manga;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
    private final Manga manga;
    private final Navegador nav;
    
    public ScnMangaMenu(Navegador nav,Manga manga){
        this.manga = manga;
        this.nav = nav;
    }
    
    @Override
    public Parent getParent() {
        ImageView cover = new ImageView();
        cover.setFitWidth(250);
        cover.setFitHeight(400);
        cover.setPreserveRatio(true);
        if (manga.getCover() != null) {
            Image img = new Image(manga.getCover().toURI().toString());
            cover.setImage(img);
        }        
        Button btnBack = new Button("Back");
        btnBack.setOnAction(e -> {
            nav.goTo(new ScnMainMenu(nav));
        });
        VBox buttons = new VBox(btnBack);
        VBox datos = new VBox(10,
            new Label(manga.getTitle()),
            new Label(manga.getAuthor()),
            new Label(manga.getDescription()),
            new Label("Tags: "+getTags())
        ); 
        HBox top = new HBox(20,cover,datos);
        //panel con to
        VBox toppanel = new VBox(5,buttons,top);
        ListView<String> listaCaps = new ListView<>();
        for (Chapter cap : manga.getChapters()) {
            listaCaps.getItems().add(cap.getChName());
        }
        listaCaps.setOnMouseClicked(e -> {
            int indice = listaCaps.getSelectionModel().getSelectedIndex();
            if(indice >= 0) {
                Chapter selChapter = manga.getChapters().get(indice);
                if(selChapter.hasPages()){
                    Logger.info("Selected: "+selChapter.getChName());
                    nav.goTo(new ScnReader(nav,manga,selChapter,indice));
                } else {
                    Logger.noPagesAlert(selChapter);
                }
            }
        });
        
        BorderPane panel = new BorderPane();
        panel.setTop(toppanel);
        panel.setCenter(listaCaps);
        return panel;
    }
    
    private String getTags() {
        if (manga.getTags().isEmpty()) return "";
        return String.join(", ", manga.getTags());
    }

    @Override
    public String getName() {
        return manga.getTitle();
    }
    
}
