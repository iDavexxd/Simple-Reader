package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Chapter;
import app.simplereader.manga.Manga;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F5;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane; 
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

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
    public Scene getScene() {
        ImageView cover = new ImageView();
        cover.setFitWidth(250);
        cover.setFitHeight(400);
        cover.setPreserveRatio(true);
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);
        
        recorte.widthProperty().bind(cover.fitWidthProperty());
        recorte.heightProperty().bind(cover.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
        
        cover.setClip(recorte);
        if (manga.getCover() != null) {
            Image img = new Image(manga.getCover().toURI().toString(),true);
            cover.setImage(img);
        }        
        Button btnBack = new Button("Back");
        btnBack.setOnAction(e -> {
            nav.goTo(new ScnMainMenu(nav));
        });
        VBox buttons = new VBox(btnBack);
        Label title = new Label(manga.getTitle());
        title.getStyleClass().add("manga-info-title");
        title.setWrapText(true);
        Label author = new Label(manga.getAuthor());
        author.getStyleClass().add("manga-info-author");
        Label description = new Label(manga.getDescription());
        description.setWrapText(true);
        description.getStyleClass().add("manga-info-description");
        Label tags = new Label(getTags());
        tags.getStyleClass().add("manga-info-tags");
        VBox datosmanga = new VBox(10,title,author,description); 
        VBox tagsmanga = new VBox(tags);
        BorderPane datos = new BorderPane();
        datos.setTop(datosmanga);
        datos.setBottom(tagsmanga);
        HBox top = new HBox(20,cover,datos);
        //panel con to
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
        listaCaps.getStyleClass().add("chapter-list");
        VBox coverlista = new VBox(10,top,listaCaps);
        coverlista.getStyleClass().add("manga-info");
        VBox.setVgrow(listaCaps, javafx.scene.layout.Priority.ALWAYS);
        
        AnchorPane lateralmenu = new AnchorPane();
        lateralmenu.getStyleClass().add("side-menu");
        lateralmenu.setPrefWidth(45);
        lateralmenu.setMinWidth(45);
        lateralmenu.setMaxWidth(45);
        lateralmenu.getChildren().add(buttons);
        
        AnchorPane.setTopAnchor(buttons, 10.0);
        
        BorderPane panel = new BorderPane();
        panel.setLeft(lateralmenu);
        
        panel.setCenter(coverlista);
       
        Scene scene = new Scene(panel,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    Logger.info("F5");
                    nav.goTo(new ScnMangaMenu(nav, this.manga));
                    e.consume(); // Evita que el evento siga propagándose
                }
                case ESCAPE -> {
                    nav.goTo(new ScnMainMenu(nav));
                    e.consume();
                }
            }
        });
        
        return scene;
    }
    
    
    
    private String getTags() {
        if (manga.getTags().isEmpty()) return "";
        return String.join(", ", manga.getTags());
    }

    @Override
    public String getName() {
        return manga.getTitle();
    }
    @Override
    public String getParentName(){
        return "MangaMenu";
    }
    
}