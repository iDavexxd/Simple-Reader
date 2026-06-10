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
import javafx.scene.effect.GaussianBlur;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

// Importaciones para las animaciones
import javafx.animation.FadeTransition;
import javafx.util.Duration;

/**
 *
 * @author david
 */
public class ScnMangaMenu implements AppScene{
    
    // Color de sombra solicitado (#1A1D23)
    private static final String SHADOW_COLOR = "#1A1D23";
    
    private final Manga manga;
    private final SceneController nav = SceneController.getInstance();
    private final MangaMenuController controller;
    private final LibraryController lib = LibraryController.getInstance();
    
    private boolean isdown = true;
    private boolean menuVisible = false;
    private boolean covermenuvisible = false;
    
    private SVGPath icnArrow;
    private SVGPath icnLibrary;
    private SVGPath icnDownload;
    
    private Label title;
    private Label author;
    private Label description;
    private Label tags;
    
    private Button btnAddToLibrary;    
    private Button btnDownloadChapter;
    
    private ImageView coverView;
    private StackPane coverContainer;
    private Rectangle placeholder;
    private ImageView bgView; // <-- Añadido para controlar el fondo desde loadCover
    
    private ListView<Chapter> listaCaps;
    private StackPane categoryMenu;
    private BorderPane categoryPane;
    private BorderPane coverPane;
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
        
        /*
         * COVER
         */
        coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        coverContainer = new StackPane();
        coverContainer.setMinWidth(300);
        coverContainer.setMaxWidth(300);
        coverContainer.setMinHeight(450); // 300 * 1.5
        coverContainer.setMaxHeight(450);

        placeholder = new Rectangle();
        placeholder.setFill(Color.rgb(255, 255, 255, 0.1)); 
        placeholder.widthProperty().bind(coverContainer.widthProperty());
        placeholder.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.getChildren().addAll(placeholder, coverView);

        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);
        
        coverContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            coverContainer.setPrefHeight(newVal.doubleValue() * 1.5);
        });
        
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
        
        /*
         * INFO DEL MANGA
         */
        
        title = new Label(manga.getTitle());
        title.getStyleClass().add("manga-info-title");
        title.setWrapText(true);
        author = new Label(manga.getAuthor() != null ? manga.getAuthor() : "");
        author.getStyleClass().add("manga-info-author");
        
        description = new Label(manga.getDescription() != null ? manga.getDescription() : "");
        description.setWrapText(true);
        description.getStyleClass().add("manga-info-description");
        
        ScrollPane descScroll = new ScrollPane();
        descScroll.getStyleClass().add("description-scroll");
        descScroll.setContent(description);
        descScroll.setFitToWidth(true);
        VBox.setVgrow(descScroll, Priority.ALWAYS); //No funciona
        
        tags = new Label(controller.getTags());
        tags.getStyleClass().add("manga-info-tags");
        VBox.setVgrow(tags, Priority.ALWAYS);
        VBox datosmanga = new VBox(10, title, author, descScroll); 
        VBox tagsmanga = new VBox(tags);
        BorderPane datos = new BorderPane();
        datos.setMaxHeight(500);
        datos.setCenter(datosmanga);
        if(!controller.getTags().equals("")) datos.setBottom(tagsmanga);
        HBox top = new HBox(20, coverContainer, datos);
        top.setAlignment(Pos.CENTER_LEFT); // Asegurar centrado vertical del contenido
        top.setPadding(new Insets(20)); // Un poco de aire alrededor
        
        /*
         * FONDO DESENFOCADO CON SOMBRA INFERIOR PESADA (FIX WEBP OPTIMIZADO)
         */
        bgView = new ImageView();
        
        GaussianBlur blur = new GaussianBlur(30); // Desenfoque alto
        bgView.setEffect(blur);
        
        bgView.setCache(true);
        bgView.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        bgView.setManaged(false);
        bgView.setPreserveRatio(true); // Mantener proporción para que no se estire
        bgView.setOpacity(0.0); // Ocultar para animación inicial

        // --> AHORA CARGAMOS LA IMAGEN PARA AMBAS VISTAS <--
        if (manga.getCoverURL() != null) {
            loadCover(manga.getCoverURL());
        }

        // CREAR LA CAPA DE SOMBRA (GRADIENTE DE ABAJO HACIA ARRIBA)
        Region shadowOverlay = new Region();
        String gradientStyle = "-fx-background-color: linear-gradient(to top, " +
                               SHADOW_COLOR + " 0%, " +
                               "rgba(26, 29, 35, 0.8) 30%, " +
                               "rgba(26, 29, 35, 0) 100%);";   
        shadowOverlay.setStyle(gradientStyle);

        // ENSAMBLAR EL HEADER: FONDO -> SOMBRA -> CONTENIDO
        StackPane fulltop = new StackPane(bgView, shadowOverlay, top);
        fulltop.setMaxHeight(500);
        fulltop.getStyleClass().add("manga-header-full");

        // Bindings para que la imagen ocupe todo el ancho y se recorte
        bgView.fitWidthProperty().bind(fulltop.widthProperty());
        
        // Binding para centrar la imagen verticalmente
        bgView.layoutYProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> (fulltop.getHeight() - bgView.getBoundsInLocal().getHeight()) / 2.0,
                fulltop.heightProperty(),
                bgView.boundsInLocalProperty()
            )
        );

        // Recortar excesos con bordes redondeados (Clip)
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(fulltop.widthProperty());
        clip.heightProperty().bind(fulltop.heightProperty());
        clip.setArcWidth(25);  
        clip.setArcHeight(25); 
        fulltop.setClip(clip);

        /*
         * LISTA CON LOS CAPITULOS
         */
        listaCaps = new ListView<>();
        HBox.setHgrow(listaCaps, javafx.scene.layout.Priority.ALWAYS);
        listaCaps.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        doAddChapters();
        
        
        listaCaps.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            if (e.getClickCount() < 2) return;
            Chapter selChapter = listaCaps.getSelectionModel().getSelectedItem();
            if (selChapter != null) {
                controller.openChapter(selChapter);
            }
        });

        ContextMenu ctxMenu = new ContextMenu();
        MenuItem markRead = new MenuItem("Mark as read");
        MenuItem markUnread = new MenuItem("Mark as unread");
        
        ctxMenu.getItems().addAll(markRead, markUnread);

        ctxMenu.setOnShowing(e -> {
            var selected = listaCaps.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) {
                e.consume();
                return;
            }
            boolean anyUnread = false;
            boolean anyRead = false;
            for (Chapter ch : selected) {
                if (ch.isReaded()) anyRead = true;
                else anyUnread = true;
            }
            markRead.setVisible(anyUnread);
            markUnread.setVisible(anyRead);
        });

        markRead.setOnAction(e -> {
            for (Chapter sel : listaCaps.getSelectionModel().getSelectedItems()) {
                sel.markAsReaded();
            }
            lib.saveLibrary();
            listaCaps.refresh();
            listaCaps.getSelectionModel().clearSelection();
        });

        markUnread.setOnAction(e -> {
            for (Chapter sel : listaCaps.getSelectionModel().getSelectedItems()) {
                sel.unRead();
            }
            lib.saveLibrary();
            listaCaps.refresh();
            listaCaps.getSelectionModel().clearSelection();
        });

        listaCaps.setContextMenu(ctxMenu);

        listaCaps.getStyleClass().add("chapter-list");
        listaCaps.setCellFactory(lv -> new ListCell<Chapter>() {
            private final Label titleLabel = new Label();
            private final Label subLabel = new Label();
            private final VBox cellBox = new VBox(2, titleLabel, subLabel);

            {
                titleLabel.getStyleClass().add("chapter-title");
                subLabel.getStyleClass().add("chapter-sub");
            }

            @Override
            protected void updateItem(Chapter cap, boolean empty) {
                super.updateItem(cap, empty);
                getStyleClass().removeAll("chapter-read", "chapter-unread");
                if (empty || cap == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    titleLabel.setText(cap.getTitle());
                    String date = cap.getDate();
                    if (date != null && date.length() >= 10) date = date.substring(0, 10);
                    String scan = cap.getScane();
                    subLabel.setText((date != null ? date : "") + (scan != null ? " | " + scan : ""));
                    setGraphic(cellBox);
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
        if(lib.onLibrary(manga)){
            icnLibrary.setContent(onLibrary);
        } 
        else {
            icnLibrary.setContent(addLibrary);
        }
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
        
        icnDownload = new SVGPath();
        doChangeDownloadIcon(0);
        icnDownload.getStyleClass().add("icon");
        icnDownload.setScaleX(scale);
        icnDownload.setScaleY(scale);
        
        Group icnDownload_group = new Group(icnDownload);
        StackPane icnDownload_container = new StackPane(icnDownload_group);
        
        VBox botones = new VBox(5);
        
        btnAddToLibrary = new Button("",icnLibrary_pane);
        btnAddToLibrary.getStyleClass().add("mangamenu-button");
        btnAddToLibrary.setOnAction(e -> {
            doShowMenu();
        });
        btnAddToLibrary.setMinSize(40, 40);
        btnAddToLibrary.setMaxSize(40, 40);

        Button btnKeepReading = new Button("", icon_read);
        btnKeepReading.getStyleClass().add("mangamenu-button");
        btnKeepReading.setOnAction(e -> {
            Chapter sel = listaCaps.getSelectionModel().getSelectedItem();
            if (sel != null) {
                controller.openChapter(sel);
                return;
            }
            Chapter selChapter = controller.findFirstUnreadChapter();
            if (selChapter != null) {
                controller.openChapter(selChapter);
            } else {
                Logger.info("No unread chapters.");
            }
        });
        
        if(!manga.getSourceID().equals("local")){
            btnDownloadChapter = new Button("",icnDownload_container);
            doChangeDownloadIcon(0);
            listaCaps.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    if(newValue.isDownloaded()){
                        doChangeDownloadIcon(1);
                    } else{
                        doChangeDownloadIcon(0);
                    }
                    
                } else {
                    // No hay ningún capítulo seleccionado en la lista.
                    doChangeDownloadIcon(0);
                }
            });
            btnDownloadChapter.getStyleClass().add("mangamenu-button");
            btnDownloadChapter.setMaxSize(40, 40);
            btnDownloadChapter.setMinSize(40, 40);
            btnDownloadChapter.disableProperty().bind(listaCaps.getSelectionModel().selectedItemProperty().isNull());
            btnDownloadChapter.setOnAction(e -> {
                for (Chapter ch : listaCaps.getSelectionModel().getSelectedItems()) {
                    controller.downloadChapter(ch);
                }
            });
        }
        
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
        if(!manga.getSourceID().equals("local")) botones.getChildren().add(btnDownloadChapter);
        botones.getChildren().add(spacer);
        botones.getChildren().add(btnAddToLibrary);
        botones.getChildren().add(btnInvertir);
 
        btnKeepReading.setMaxSize(40, 40);
        btnKeepReading.setMinSize(40, 40);
        btnInvertir.setMaxSize(40, 40);
        btnInvertir.setMinSize(40, 40);
        HBox bottom = new HBox(listaCaps, botones);
        bottom.setSpacing(5);
        
        VBox coverlista = new VBox(10, fulltop, bottom);
        
        coverlista.getStyleClass().add("manga-info");
        coverlista.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            javafx.scene.Node p = target;
            boolean inList = false;
            while (p != null) {
                if (p == listaCaps) { inList = true; break; }
                p = p.getParent();
            }
            if (!inList) listaCaps.getSelectionModel().clearSelection();
        });
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
        
        SVGPath icnClose = new SVGPath();
        icnClose.getStyleClass().add("icon");
        icnClose.setContent("m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z");
        icnClose.setScaleX(scale);
        icnClose.setScaleY(scale);
        
        Group icnClose_group = new Group(icnClose);
        StackPane icnClose_container = new StackPane(icnClose_group);

        Button btnClose = new Button("",icnClose_container);
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
        
        Button btnEdit = new Button("Edit");
        Button btnAccept = new Button("Accept");
        
        btnEdit.setMaxSize(Double.MAX_VALUE, 30);
        btnEdit.setMinHeight(30);
        
        btnAccept.setMaxSize(Double.MAX_VALUE, 30);
        btnAccept.setMinHeight(30);
        
        btnAccept.setOnAction(e -> doHideMenu());
        btnEdit.setOnAction(e -> {
            nav.goTo(new ScnConfig());
        });
        
        HBox bottomContent = new HBox(10, btnEdit, btnAccept);
        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        HBox.setHgrow(btnAccept, Priority.ALWAYS);

        bottomContent.setAlignment(Pos.CENTER_RIGHT); 

        // 2. Le decimos al ScrollPane que ocupe el espacio sobrante (así no empuja los botones fuera)
        VBox.setVgrow(categories, Priority.ALWAYS);

        // 3. Ya no necesitamos el bottomSpacer, armamos el VBox directamente
        VBox content = new VBox(10, topContent, categories, bottomContent);
        
        categoryMenu.getChildren().add(content);
        
        categoryPane = new BorderPane();
        categoryPane.getStyleClass().add("menu-background");

        categoryPane.setCenter(categoryMenu);
        categoryPane.setVisible(menuVisible);
        
        coverPane = new BorderPane();
        doConfigCoverMenu();
        coverPane.getStyleClass().add("menu-background");
        coverPane.setVisible(covermenuvisible);

        StackPane panel_menu = new StackPane(panel,categoryPane,coverPane);
        
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
                    if(covermenuvisible == false && menuVisible == false) nav.backScene();
                    if(covermenuvisible) doHideCoverMenu();
                    e.consume();
                }
            }
        });
        
        return scene;
    }
    
    public void doConfigCoverMenu(){
        Image cover = new Image(manga.getCoverURL(),true);
        
        ImageView coverImageView = new ImageView(cover);
        coverImageView.setPreserveRatio(true);
        coverImageView.fitHeightProperty().bind(coverPane.heightProperty());
        StackPane stackpane = new StackPane(coverImageView);
        
        SVGPath svg = new SVGPath();
        double scale = 24.0 / 960.0;
        svg.setContent("m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        Group icon_group = new Group(svg);
        StackPane icon = new StackPane(icon_group);
        
        Button btnBack = new Button("",icon);
        
        btnBack.setOnAction(e -> doHideCoverMenu());
        btnBack.setMinSize(24, 24);
        btnBack.setMaxSize(24, 24);
        Region spacer = new Region();
        
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox left = new VBox(btnBack,spacer);
        left.setPadding(new Insets(15));
        coverPane.setCenter(stackpane);
        coverPane.setRight(left);
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
        
        // ¡SOLO CREAMOS LA IMAGEN UNA VEZ! Esto reduce el consumo de RAM a la mitad.
        Image sharedImage = new Image(url, true);
        
        coverView.setImage(sharedImage);
        coverView.setOpacity(0.0);
        coverView.setCache(true);
        coverView.setCacheHint(javafx.scene.CacheHint.SPEED); // Agregamos caché a la cover principal también
        
        if (bgView != null) {
            bgView.setImage(sharedImage);
            bgView.setOpacity(0.0);
        }
        
        placeholder.setVisible(true);
        
        Runnable updateImageFit = () -> {
            if (sharedImage.getProgress() < 1.0 || sharedImage.isError()) return;

            double imgW = sharedImage.getWidth();
            double imgH = sharedImage.getHeight();
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
        
        coverContainer.setOnMouseClicked(e -> {
            doShowCoverMenu();
        });
        
        sharedImage.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() >= 1.0 && !sharedImage.isError()) {
                updateImageFit.run();
                
                // Retrasamos la animación un solo 'frame' usando Platform.runLater
                // Esto permite que el desenfoque pesado se calcule sin colisionar con el Fade
                javafx.application.Platform.runLater(() -> {
                    FadeTransition fadeCover = new FadeTransition(Duration.millis(400), coverView);
                    fadeCover.setFromValue(0.0);
                    fadeCover.setToValue(1.0);
                    fadeCover.setOnFinished(e -> placeholder.setVisible(false)); 
                    
                    if (bgView != null) {
                        FadeTransition fadeBg = new FadeTransition(Duration.millis(400), bgView);
                        fadeBg.setFromValue(0.0);
                        fadeBg.setToValue(1.0);
                        fadeBg.play();
                    }
                    
                    fadeCover.play();
                });
            }
        });
        
        // Si la imagen carga de manera instantánea (ya estaba en memoria)
        if (sharedImage.getProgress() >= 1.0) {
            updateImageFit.run();
            coverView.setOpacity(1.0);
            if (bgView != null) bgView.setOpacity(1.0);
            placeholder.setVisible(false);
        }
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
        categoryPane.setVisible(menuVisible);
    }
    
    private void doHideMenu(){
        menuVisible = false;
        categoryMenu.setVisible(menuVisible);
        categoryPane.setVisible(menuVisible);
    }
    
    private void doShowCoverMenu(){
        covermenuvisible = true;
        coverPane.setOpacity(1.0); // Nos aseguramos de que esté visible siempre
        coverPane.setVisible(true);
    }
    
    private void doHideCoverMenu(){
        covermenuvisible = false;
        coverPane.setVisible(false);
    }
    
    private void doChangeDownloadIcon(int op){
        
        // 0 = hide
        // 1 = Downloaded
        // 2 = download
        
        switch (op){
            case 0 -> icnDownload.setContent("M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z");
            case 1 -> icnDownload.setContent("M382-320 155-547l57-57 170 170 366-366 57 57-423 423ZM200-160v-80h560v80H200Z");
            case 2 -> icnDownload.setContent("M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z");
            default -> Logger.info("iccon "+op+" does not exist.");
        }
    }

    public void doCategoryButtons(){
        
        if(categoryButtons == null) return;
        
        String addLibrary = "M520-400h80v-120h120v-80H600v-120h-80v120H400v80h120v120ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
        String onLibrary = "m508-398 226-226-56-58-170 170-86-84-56 56 142 142ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
        
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
                        if(lib.onLibrary(manga)) icnLibrary.setContent(onLibrary);
                    }
                } else {
                    controller.removeManga(this.manga, category);
                    if(!lib.onLibrary(manga)) icnLibrary.setContent(addLibrary);
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