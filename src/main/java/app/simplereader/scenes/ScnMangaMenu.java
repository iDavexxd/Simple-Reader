package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Manga;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.LocalManga;
import app.simplereader.scenes.others.SideMenu;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F5;
import static javafx.scene.input.KeyCode.F7;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane; 
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

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
            Image img = new Image(manga.getCover(),true);
            cover.setImage(img);
        }        
        
        SVGPath icnBack = new SVGPath();
        icnBack.setContent("M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z");
        icnBack.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        icnBack.setScaleX(scale);
        icnBack.setScaleY(scale);
        
        Group icon_back_group = new Group(icnBack);
        StackPane icon_back = new StackPane(icon_back_group);
        icon_back.setPrefSize(24, 24);
        icon_back.setMaxSize(24, 24);
        
        
        Button btnBack = new Button("",icon_back);
        btnBack.setOnAction(e -> {
            nav.goTo(new ScnMainMenu(nav));
        });
        btnBack.setMinSize(24, 24);
        btnBack.setMaxSize(24,24);
        
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
        //panel con to'
        ListView<String> listaCaps = new ListView<>();
        for (Chapter cap : manga.getChapters()) {
            listaCaps.getItems().add(cap.getName());
        }
        listaCaps.setOnMouseClicked(e -> {
            int indice = listaCaps.getSelectionModel().getSelectedIndex();
            if(indice >= 0) {
                Chapter selChapter = manga.getChapters().get(indice);
                if(selChapter.hasPages()){
                    Logger.info("Selected: "+selChapter.getName());
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
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(buttons);
        
        BorderPane panel = new BorderPane();
        panel.setLeft(lateralmenu.getPane());
        
        panel.setCenter(coverlista);
       
        Scene scene = new Scene(panel,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    Logger.info("F5");
                    nav.goTo(new ScnMangaMenu(nav, this.manga));
                    e.consume();
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