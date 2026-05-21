package app.simplereader.views;

import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.controller.MangaMenuController;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.controller.Logger;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Category;
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
import app.simplereader.views.components.Buttons;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

/**
 *
 * @author david
 */
public class ScnMangaMenu implements AppScene{
    private final Manga manga;
    private final SceneController nav = SceneController.getInstance();
    private final MangaMenuController controller;
    private final LibraryController lib = LibraryController.getInstance();
    
    private boolean isdown = true;
    private boolean menuVisible = false;
    
    
    private SVGPath icnArrow;
    private SVGPath icnLibrary;
    
    private Label title;
    private Label author;
    private Label description;
    private Label tags;
    
    private ImageView coverView;
    private StackPane coverContainer;
    private Rectangle placeholder;
    
    private ListView<Chapter> listaCaps;
    private StackPane categoryMenu;
    private VBox categoryButtons;
    
    public ScnMangaMenu(Manga manga){
        this.manga = manga;
        nav.getStage().setResizable(false);
        
        MangaMenuController.doInstance(this);
        this.controller = MangaMenuController.getInstance();
        controller.init(manga);
    }
    
    @Override
    public Scene getScene() {
        coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        // 1. Contenedor con tamaño mayor (300px en lugar de 250px)
        coverContainer = new StackPane();
        coverContainer.setMinWidth(300);
        coverContainer.setMaxWidth(300);
        coverContainer.setMinHeight(450); // 300 * 1.5
        coverContainer.setMaxHeight(450);

        // 2. Placeholder semitransparente
        placeholder = new Rectangle();
        placeholder.setFill(Color.rgb(255, 255, 255, 0.1)); 
        placeholder.widthProperty().bind(coverContainer.widthProperty());
        placeholder.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.getChildren().addAll(placeholder, coverView);

        if (manga.getCoverURL() != null) {
            loadCover(manga.getCoverURL());
        }

        // 5. Clip redondeado para el contenedor general
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);
        
        // 6. Listener para mantener ratio 2:3
        coverContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            coverContainer.setPrefHeight(newVal.doubleValue() * 1.5);
        });
        
        // 7. Bindings de centrado (siempre activos)
        coverView.layoutXProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> (coverContainer.getWidth() - coverView.getBoundsInLocal().getWidth()) / 2.0,
                coverContainer.widthProperty(),
                coverView.boundsInLocalProperty()
            )
        );

        coverView.layoutYProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> (coverContainer.getHeight() - coverView.getBoundsInLocal().getHeight()) / 2.0,
                coverContainer.heightProperty(),
                coverView.boundsInLocalProperty()
            )
        );
        
        double scale = 24.0 / 960.0;
        Button btnBack = Buttons.getBackButton();
        
        VBox buttons = new VBox(btnBack);
        title = new Label(manga.getTitle());
        title.getStyleClass().add("manga-info-title");
        title.setWrapText(true);
        author = new Label(manga.getAuthor() != null ? manga.getAuthor() : "");
        author.getStyleClass().add("manga-info-author");
        description = new Label(manga.getDescription() != null ? manga.getDescription() : "");
        description.setWrapText(true);
        description.getStyleClass().add("manga-info-description");
        tags = new Label(controller.getTags());
        tags.getStyleClass().add("manga-info-tags");
        
        VBox datosmanga = new VBox(10, title, author, description); 
        VBox tagsmanga = new VBox(tags);
        BorderPane datos = new BorderPane();
        datos.setTop(datosmanga);
        datos.setBottom(tagsmanga);
        HBox top = new HBox(20, coverContainer, datos);
        
        
        
        listaCaps = new ListView<>();
        HBox.setHgrow(listaCaps, javafx.scene.layout.Priority.ALWAYS);
        doAddChapters();

        listaCaps.setOnMouseClicked(e -> {
            Chapter selChapter = listaCaps.getSelectionModel().getSelectedItem();
            if (selChapter != null) {
                controller.openChapter(selChapter);
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
        
        String addLibrary = "M520-400h80v-120h120v-80H600v-120h-80v120H400v80h120v120ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
        String onLibrary = "m508-398 226-226-56-58-170 170-86-84-56 56 142 142ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
        
        icnLibrary = new SVGPath();
        icnLibrary.setContent(addLibrary);
        icnLibrary.getStyleClass().add("icon");
        icnLibrary.setScaleX(scale);
        icnLibrary.setScaleY(scale);
        
        Group icnLibrary_group = new Group(icnLibrary);
        StackPane icnLibrary_pane = new StackPane(icnLibrary_group);
        
        icnArrow = new SVGPath();
        
        icnArrow.setContent(down);
        icnArrow.getStyleClass().add("icon");
        icnArrow.setScaleX(scale);
        icnArrow.setScaleY(scale);
        
        Group icon_arrow_group = new Group(icnArrow);
        StackPane icon_arrow = new StackPane(icon_arrow_group);
        
        
        VBox botones = new VBox(5);
        
        Button btnAddToLibrary = new Button("",icnLibrary_pane);
        btnAddToLibrary.getStyleClass().add("mangamenu-button");
        btnAddToLibrary.setOnAction(e -> {
            doShowMenu();
        });
        btnAddToLibrary.setMinSize(40, 40);
        btnAddToLibrary.setMaxSize(40, 40);

        Button btnKeepReading = new Button("", icon_read);
        btnKeepReading.getStyleClass().add("mangamenu-button");
        btnKeepReading.setOnAction(e -> {
            Chapter selChapter = controller.findFirstUnreadChapter();

            if (selChapter != null) {
                controller.openChapter(selChapter);
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
        
        //Menu
        categoryMenu = new StackPane();
        categoryMenu.getStyleClass().add("category-option");
        categoryMenu.setMaxSize(300, 450);
        categoryMenu.setVisible(menuVisible);
        categoryMenu.setPadding(new Insets(15));
        
        Button btnClose = new Button("x");
        btnClose.setMinSize(24, 24);
        btnClose.setMaxSize(24,24);
        btnClose.setOnAction(e -> doHideMenu());
        Region topSpacer = new Region();
        topSpacer.setMaxWidth(Double.MAX_VALUE);
        Label topLabel = new Label("Categories");
        
        HBox topContent = new HBox(topLabel,topSpacer,btnClose);
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        categoryButtons = new VBox(5);
        categoryButtons.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(categoryButtons, Pos.TOP_CENTER);
        
        
        doCategoryButtons();
        ScrollPane categories = new ScrollPane(categoryButtons);
        
        VBox content = new VBox(10,topContent,categories);
        
        
        categoryMenu.getChildren().add(content);
        StackPane panel_menu = new StackPane(panel,categoryMenu);
        
        BorderPane fullPanel = new BorderPane();
        fullPanel.setCenter(panel_menu);
        Scene scene = new Scene(fullPanel, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    controller.reloadManga();
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
    
    public void doAddChapters(){
        List<Chapter> chapters = controller.getChapters();
        for (Chapter cap : chapters) {
            listaCaps.getItems().add(cap);
        }
    }
    
    public void doReloadChapters(){
        List<Chapter> chapters = controller.getChapters();
        listaCaps.getItems().clear();
        for (Chapter cap : chapters) {
            listaCaps.getItems().add(cap);
        }
    }
    
    public void loadCover(String url){
        if (url == null) return;
        
        Image icon = new Image(url, true);
        coverView.setImage(icon);
        
        placeholder.setVisible(true);
        
        Runnable updateImageFit = () -> {
            if (icon.getProgress() < 1.0 || icon.isError()) return;

            double imgW = icon.getWidth();
            double imgH = icon.getHeight();
            double contW = coverContainer.getWidth();
            double contH = coverContainer.getHeight();

            if (imgW == 0 || imgH == 0 || contW == 0 || contH == 0) return;

            double imageRatio = imgW / imgH;
            double realContainerRatio = contW / contH; 

            if (imageRatio < realContainerRatio) {
                coverView.setFitWidth(contW);
                coverView.setFitHeight(0); 
            } else {
                coverView.setFitHeight(contH);
                coverView.setFitWidth(0); 
            }
        };

        coverContainer.widthProperty().addListener((obs, oldV, newV) -> updateImageFit.run());
        coverContainer.heightProperty().addListener((obs, oldV, newV) -> updateImageFit.run());

        icon.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() >= 1.0) {
                updateImageFit.run();
                placeholder.setVisible(false);
            }
        });
    }
    
    public void setTitle(String title){
        this.title.setText(title);
    }
    
    public void setAuthor(String author){
        this.author.setText(author);
    }
    
    public void setDescrition(String desc){
        this.description.setText(desc);
    }
    
    public void setTags(String tags){
        this.tags.setText(tags);
    }
    
    private void doShowMenu(){
        menuVisible = true;
        categoryMenu.setVisible(menuVisible);
    }
    
    private void doHideMenu(){
        menuVisible = false;
        categoryMenu.setVisible(menuVisible);
    }

    private void doCategoryButtons(){
        categoryButtons.getChildren().clear();
        
        categoryButtons.setSpacing(10); 

        for(Category category : lib.getAllCategories()){
            CheckBox chkCategory = new CheckBox(category.getName());
            
            chkCategory.getStyleClass().add("category-checkbox"); 

            chkCategory.setMaxSize(Double.MAX_VALUE, 30);
            
            // Si el manga está en la categoría, el CheckBox se marca automáticamente
            chkCategory.setSelected(lib.onCategory(category, manga));

            // Evento cuando el usuario hace clic en el cuadrito
            chkCategory.setOnAction(e -> {
                if (chkCategory.isSelected()) {
                    if(!lib.onCategory(category, manga)) {
                        controller.addToLibrary(category.getName());
                    }
                } else {
                    controller.removeManga(this.manga, category);
                }
            });
            
            categoryButtons.getChildren().add(chkCategory);
        }
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
