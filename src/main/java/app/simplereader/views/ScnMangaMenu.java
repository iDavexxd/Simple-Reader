package app.simplereader.views;

import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.controller.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F5;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane; 
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.components.Buttons;

import java.util.List;

/**
 *
 * @author david
 */
public class ScnMangaMenu implements AppScene{
    private final Manga manga;
    private final SceneController nav = SceneController.getInstance();
    private final LibraryController library = LibraryController.getInstance();
    
    private boolean isdown = true;
    
    public ScnMangaMenu(Manga manga){
        this.manga = manga;
        nav.getStage().setResizable(false);
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
        if (manga.getCoverURL() != null) {
            Image img = new Image(manga.getCoverURL(), true);
            cover.setImage(img);
        }        
        double scale = 24.0 / 960.0;
        Button btnBack = Buttons.getBackButton();
        
        VBox buttons = new VBox(btnBack);
        Label title = new Label(manga.getTitle());
        title.getStyleClass().add("manga-info-title");
        title.setWrapText(true);
        Label author = new Label(manga.getAuthor() != null ? manga.getAuthor() : "");
        author.getStyleClass().add("manga-info-author");
        Label description = new Label(manga.getDescription() != null ? manga.getDescription() : "");
        description.setWrapText(true);
        description.getStyleClass().add("manga-info-description");
        Label tags = new Label(getTags());
        tags.getStyleClass().add("manga-info-tags");
        VBox datosmanga = new VBox(10, title, author, description); 
        VBox tagsmanga = new VBox(tags);
        BorderPane datos = new BorderPane();
        datos.setTop(datosmanga);
        datos.setBottom(tagsmanga);
        HBox top = new HBox(20, cover, datos);
        
        List<Chapter> chapters = manga.getChapters();
        final List<Chapter> finalChapters = chapters != null ? chapters : List.of();
        
        ListView<Chapter> listaCaps = new ListView<>();
        HBox.setHgrow(listaCaps, javafx.scene.layout.Priority.ALWAYS);
        for (Chapter cap : finalChapters) {
            listaCaps.getItems().add(cap);
        }

        listaCaps.setOnMouseClicked(e -> {
            Chapter selChapter = listaCaps.getSelectionModel().getSelectedItem();
            if (selChapter != null) {
                
                // 1. Si no tiene páginas cargadas, las pedimos al Source
                if (!selChapter.hasPages()) {
                    Logger.info("Cargando páginas para: " + selChapter.getTitle());
                    
                    // Obtenemos el source correspondiente
                    MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
                    if (src != null) {
                        // Pedimos las páginas y se las asignamos al capítulo
                        List<String> pages = src.getPages(manga.getMangaID(), selChapter.getChapterID());
                        selChapter.setPages(pages);
                    }
                }
                
                // 2. Ahora sí comprobamos si tiene páginas
                if (selChapter.hasPages()) {
                    int indice = finalChapters.indexOf(selChapter);
                    Logger.info("Selected: " + selChapter.getTitle());
                    nav.goTo(new ScnReader(nav, manga, selChapter, indice));
                } else {
                    Logger.noPagesAlert(selChapter.getTitle());
                }
            }
        });

        listaCaps.getStyleClass().add("chapter-list");
        listaCaps.setCellFactory(lv -> new ListCell<Chapter>() {
            @Override
            protected void updateItem(Chapter cap, boolean empty) {
                super.updateItem(cap, empty);
                getStyleClass().removeAll("chapter-read", "chapter-unread");
                if (empty || cap == null) {
                    setText(null);
                    getStyleClass().removeAll("chapter-read", "chapter-unread");
                } else {
                    setText(cap.getTitle());
                    if (cap.isReaded()) {
                        getStyleClass().add("chapter-read");
                        getStyleClass().remove("chapter-unread");
                    } else {
                        getStyleClass().add("chapter-unread");
                        getStyleClass().remove("chapter-read");
                    }
                }
            }
        });
        
        SVGPath icnRead = new SVGPath();
        icnRead.setContent("M320-200v-560l440 280-440 280Zm80-280Zm0 134 210-134-210-134v268Z");
        icnRead.getStyleClass().add("icon");
        icnRead.setScaleX(scale);
        icnRead.setScaleY(scale);
        
        Group icon_read_group = new Group(icnRead);
        StackPane icon_read = new StackPane(icon_read_group);
        
        
        String down = "M480-344 240-584l56-56 184 184 184-184 56 56-240 240Z";
        String up = "M480-528 296-344l-56-56 240-240 240 240-56 56-184-184Z";
        
        SVGPath icnArrow = new SVGPath();
        
        icnArrow.setContent(down);
        icnArrow.getStyleClass().add("icon");
        icnArrow.setScaleX(scale);
        icnArrow.setScaleY(scale);
        
        Group icon_arrow_group = new Group(icnArrow);
        StackPane icon_arrow = new StackPane(icon_arrow_group);
        
        
        VBox botones = new VBox();
        
        Button btnAddToLibrary = new Button("");
        btnAddToLibrary.getStyleClass().add("mangamenu-button");
        btnAddToLibrary.setOnAction(e -> addToLibrary());

        
        Button btnKeepReading = new Button("", icon_read);
        btnKeepReading.getStyleClass().add("mangamenu-button");
        btnKeepReading.setOnAction(e -> {
            Chapter selChapter = finalChapters.stream()
                    .filter(c -> !c.isReaded())
                    .findFirst()
                    .orElse(null);

            if (selChapter != null) {
                
                if (!selChapter.hasPages()) {
                    Logger.info("Cargando páginas para: " + selChapter.getTitle());
                    
                    // Obtenemos el source correspondiente
                    MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
                    if (src != null) {
                        // Pedimos las páginas y se las asignamos al capítulo
                        List<String> pages = src.getPages(manga.getMangaID(), selChapter.getChapterID());
                        selChapter.setPages(pages);
                    }
                }
                
                if (selChapter.hasPages()) {
                    int indice = finalChapters.indexOf(selChapter);
                    Logger.info("Selected: " + selChapter.getTitle());
                    nav.goTo(new ScnReader(nav, manga, selChapter, indice));
                } else {
                    Logger.noPagesAlert(selChapter.getTitle());
                }
            } else {
                Logger.info("No unread chapters.");
            }
        });
        
        Button btnInvertir = new Button("", icon_arrow);
        btnInvertir.getStyleClass().add("mangamenu-button");
        btnInvertir.setOnAction(e -> {
            if (isdown) {
                icnArrow.setContent(up);
                isdown = false;
            } else {
                icnArrow.setContent(down);
                isdown = true;
            }
            FXCollections.reverse(listaCaps.getItems());
        });
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        botones.getChildren().add(btnKeepReading);
        botones.getChildren().add(spacer);
        botones.getChildren().add(btnAddToLibrary);
        botones.getChildren().add(btnInvertir);
 
        btnKeepReading.setMaxSize(40, 40);
        btnKeepReading.setMinSize(40, 40);
        btnInvertir.setMaxSize(40, 40);
        btnInvertir.setMinSize(40, 40);
        HBox bottom = new HBox(listaCaps, botones);
        bottom.setSpacing(5);
        
        VBox coverlista = new VBox(10, top, bottom);
        
        coverlista.getStyleClass().add("manga-info");
        VBox.setVgrow(listaCaps, javafx.scene.layout.Priority.ALWAYS);
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(buttons);
        
        BorderPane panel = new BorderPane();
        panel.setLeft(lateralmenu.getPane());
        
        panel.setCenter(coverlista);
       
        Scene scene = new Scene(panel, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    Logger.info("F5");
                    nav.goTo(new ScnMangaMenu(this.manga));
                    e.consume();
                }
                case ESCAPE -> {
                    nav.backScene();
                    e.consume();
                }
            }
        });
        
        return scene;
    }
    
    private String getTags() {
        if (manga.getTags() == null || manga.getTags().isEmpty()) return "";
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

    private void addToLibrary() {
        SourceManager.getInstance().saveManga(this.manga);
        library.addManga(this.manga,"Default");
        library.saveLibrary();
        MainMenuController.getInstance().reloadMangas();
    }
    
}
