package app.simplereader.views;

import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.controller.MangaMenuController;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.service.Logger;
import app.simplereader.service.Downloader;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Category;
import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
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

// Importaciones para las animaciones y listeners
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.beans.value.ChangeListener;

/**
 *
 * @author david
 */
public class ScnMangaMenu implements AppScene{
    
    // Color de sombra solicitado (#1A1D23)
    private static final String SHADOW_COLOR = "#1A1D23";
    
    private Manga manga;
    private final SceneController nav = SceneController.getInstance();
    private MangaMenuController controller;
    private final LibraryController lib = LibraryController.getInstance();
    
    private boolean isdown = true;
    private boolean menu_visible = false;
    private boolean covermenu_visible = false;
    private boolean loadingPane_visible = false;
    
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
    private ImageView bgView; 
    private Rectangle fadeOverlay;
    
    private ListView<Chapter> listaCaps;
    private StackPane categoryMenu;
    private BorderPane categoryPane;
    private BorderPane coverPane;
    private BorderPane loadingPane;
    private VBox categoryButtons;
    
    private ImageView coverMenuImageView; 
    private Image currentCoverImage; 
    private Image loadingGif;
    private ChangeListener<Number> widthListener;
    private ChangeListener<Number> heightListener;
    
    private static ScnMangaMenu instance;
    private javafx.scene.Parent myScene;
    
    private ScnMangaMenu(){
        MangaMenuController.doInstance(this);
        this.controller = MangaMenuController.getInstance();
        
        try {
                loadingGif = new javafx.scene.image.Image(getClass().getResource("/icons/koruko.gif").toExternalForm());
        } catch (Exception e) {
                Logger.error("No se pudo cargar el gif de carga: " + e.getMessage());
        }
    }
    
    public static ScnMangaMenu getInstance() {
        if (instance == null) {
            instance = new ScnMangaMenu();
        }
        return instance;
    }
    
    public void updateManga(Manga newManga) {
        boolean sameCover = (this.manga != null && this.manga.getCoverURL() != null && this.manga.getCoverURL().equals(newManga.getCoverURL()));
        this.manga = newManga;
        this.controller.init(newManga);
        
        if (myScene != null) {
            title.setText(newManga.getTitle());
            author.setText(newManga.getAuthor() != null ? newManga.getAuthor() : "");
            description.setText(newManga.getDescription() != null ? newManga.getDescription() : "");
            String currentTags = controller.getTags();
            tags.setText(currentTags);
            boolean hasTags = !currentTags.isEmpty();
            tags.setVisible(hasTags);
            tags.setManaged(hasTags);
            
            String addLibrary = "M520-400h80v-120h120v-80H600v-120h-80v120H400v80h120v120ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
            String onLibrary = "m508-398 226-226-56-58-170 170-86-84-56 56 142 142ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z";
            if(lib.onLibrary(newManga)) icnLibrary.setContent(onLibrary);
            else icnLibrary.setContent(addLibrary);
            
            // Actualizar visibilidad del botón de descarga según el source
            boolean isLocal = newManga.getSourceID() == null || newManga.getSourceID().equals("local");
            btnDownloadChapter.setVisible(!isLocal);
            btnDownloadChapter.setManaged(!isLocal);
            
            if (!sameCover) {
                if (currentCoverImage != null) currentCoverImage.cancel();
                coverView.setImage(null);
                coverView.setOpacity(0.0);
                if (bgView != null) {
                    bgView.setImage(null);
                    bgView.setOpacity(1.0); // bgView siempre opaco
                }
                loadCover(newManga.getCoverURL());
            }
            
            doReloadChapters();
            doCategoryButtons();
        }
    }
    
    @Override
    public javafx.scene.Parent getScene() {
        if (loadingPane != null) doHideLoadingPane();
        if (myScene != null) return myScene;
        
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
        
        String initialTags = controller.getTags();
        tags = new Label(initialTags);
        tags.setWrapText(true);
        tags.getStyleClass().add("manga-info-tags");
        VBox.setVgrow(tags, Priority.ALWAYS);
        boolean hasTagsInit = !initialTags.isEmpty();
        tags.setVisible(hasTagsInit);
        tags.setManaged(hasTagsInit);
        
        VBox datosmanga = new VBox(10, title, author, descScroll); 
        VBox tagsmanga = new VBox(tags);
        BorderPane datos = new BorderPane();
        datos.setMaxHeight(500);
        datos.setCenter(datosmanga);
        datos.setBottom(tagsmanga);
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
                doShowLoadingPane();
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
            private final ProgressBar progressBar = new ProgressBar(0);
            private final VBox cellBox = new VBox(2, titleLabel, subLabel, progressBar);

            {
                titleLabel.getStyleClass().add("chapter-title");
                subLabel.getStyleClass().add("chapter-sub");
                progressBar.getStyleClass().add("chapter-download-progress");
                progressBar.setMaxWidth(Double.MAX_VALUE);
                progressBar.setVisible(false);
                progressBar.setManaged(false);
            }

            private Downloader.DownloadInfo findDownloadInfo(String chapterID) {
                for (Downloader.DownloadInfo info : Downloader.getInstance().getActiveDownloads()) {
                    if (info.getChapterID().equals(chapterID)) {
                        return info;
                    }
                }
                return null;
            }

            @Override
            protected void updateItem(Chapter cap, boolean empty) {
                super.updateItem(cap, empty);
                getStyleClass().removeAll("chapter-read", "chapter-unread");
                progressBar.progressProperty().unbind();

                if (empty || cap == null) {
                    setGraphic(null);
                    setText(null);
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                } else {
                    titleLabel.setText(cap.getTitle());
                    String date = cap.getDate();
                    if (date != null && date.length() >= 10) date = date.substring(0, 10);
                    String scan = cap.getScane();
                    subLabel.setText((date != null ? date : "") + (scan != null ? " | " + scan : ""));

                    // Show progress bar if this chapter is downloading
                    Downloader.DownloadInfo info = findDownloadInfo(cap.getChapterID());
                    if (info != null) {
                        progressBar.progressProperty().bind(info.progressProperty());
                        progressBar.setVisible(true);
                        progressBar.setManaged(true);
                    } else {
                        progressBar.setVisible(false);
                        progressBar.setManaged(false);
                    }

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

        // Refresh cells when downloads start or finish
        Downloader.getInstance().getActiveDownloads().addListener(
            (javafx.collections.ListChangeListener<Downloader.DownloadInfo>) change -> {
                listaCaps.refresh();
            }
        );
        
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
                doShowLoadingPane();
                controller.openChapter(sel);
                return;
            }
            Chapter selChapter = controller.findFirstUnreadChapter();
            if (selChapter != null) {
                doShowLoadingPane();
                controller.openChapter(selChapter);
            } else {
                Logger.info("No unread chapters.");
            }
        });
        
        btnDownloadChapter = new Button("",icnDownload_container);
        doChangeDownloadIcon(0);
        listaCaps.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener.Change<? extends Chapter> c) -> {
            var selected = listaCaps.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) {
                doChangeDownloadIcon(0);
                btnDownloadChapter.setDisable(true);
            } else {
                boolean allDownloaded = true;
                for (Chapter ch : selected) {
                    if (!ch.isDownloaded()) {
                        allDownloaded = false;
                        break;
                    }
                }
                if (allDownloaded) {
                    doChangeDownloadIcon(1);
                    btnDownloadChapter.setDisable(true);
                } else {
                    doChangeDownloadIcon(0);
                    btnDownloadChapter.setDisable(false);
                }
            }
        });
        btnDownloadChapter.getStyleClass().add("mangamenu-button");
        btnDownloadChapter.setMaxSize(40, 40);
        btnDownloadChapter.setMinSize(40, 40);
        btnDownloadChapter.setDisable(true);
        btnDownloadChapter.setOnAction(e -> {
            for (Chapter ch : listaCaps.getSelectionModel().getSelectedItems()) {
                controller.downloadChapter(ch);
            }
        });
        
        boolean isLocal = manga.getSourceID() == null || manga.getSourceID().equals("local");
        btnDownloadChapter.setVisible(!isLocal);
        btnDownloadChapter.setManaged(!isLocal);
        
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
        botones.getChildren().add(btnDownloadChapter);
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
        VBox.setVgrow(bottom, javafx.scene.layout.Priority.ALWAYS);
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(buttons);
        
        BorderPane panel = new BorderPane();
        panel.setLeft(lateralmenu.getPane());
        panel.setCenter(coverlista);
        
        //Menu
        categoryMenu = new StackPane();
        categoryMenu.getStyleClass().add("category-option");
        categoryMenu.setMaxSize(300, 450);
        categoryMenu.setVisible(menu_visible);
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
        
        btnAccept.setOnAction(e -> {
            java.util.List<app.simplereader.model.Category> toAdd = new java.util.ArrayList<>();
            java.util.List<app.simplereader.model.Category> toRemove = new java.util.ArrayList<>();
            
            // 1. Leer la interfaz gráfica en el hilo principal
            for (javafx.scene.Node node : categoryButtons.getChildren()) {
                if (node instanceof CheckBox chk) {
                    app.simplereader.model.Category category = (app.simplereader.model.Category) chk.getUserData();
                    if (chk.isSelected()) {
                        if (!lib.onCategory(category, manga)) toAdd.add(category);
                    } else {
                        if (lib.onCategory(category, manga)) toRemove.add(category);
                    }
                }
            }
            
            if (toAdd.isEmpty() && toRemove.isEmpty()) {
                doHideMenu();
                return;
            }
            
            // 2. Bloquear botones para evitar doble clic
            btnAccept.setDisable(true);
            btnEdit.setDisable(true);
            btnAccept.setText("Saving...");
            
            // 3. Ejecutar descargas y guardado en segundo plano
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                for (app.simplereader.model.Category cat : toAdd) {
                    controller.addToLibrary(cat.getName());
                }
                for (app.simplereader.model.Category cat : toRemove) {
                    controller.removeManga(manga, cat);
                }
                
                // 4. Volver al hilo principal para actualizar íconos y ocultar
                javafx.application.Platform.runLater(() -> {
                    if (lib.onLibrary(manga)) icnLibrary.setContent(onLibrary);
                    else icnLibrary.setContent(addLibrary);
                    
                    btnAccept.setDisable(false);
                    btnEdit.setDisable(false);
                    btnAccept.setText("Accept");
                    doHideMenu();
                });
            });
        });
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
        categoryPane.setVisible(menu_visible);
        
        coverPane = new BorderPane();
        doConfigCoverMenu();
        coverPane.getStyleClass().add("menu-background");
        coverPane.setVisible(covermenu_visible);
        
        // AHORA CARGAMOS LA IMAGEN PARA TODAS LAS VISTAS
        if (manga.getCoverURL() != null) {
            loadCover(manga.getCoverURL());
        }

        doConfigLoadingPane();
        loadingPane.getStyleClass().add("menu-background");
        loadingPane.setVisible(loadingPane_visible);
        
        StackPane panel_menu = new StackPane(panel,categoryPane,coverPane,loadingPane);
        panel_menu.setMinSize(0, 0);
        
        BorderPane fullPanel = new BorderPane();
        fullPanel.setCenter(panel_menu);
        
        // Listeners de teclado (ahora en el root, no en la Scene)
        fullPanel.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    controller.reloadManga();
                    e.consume();
                }
                case ESCAPE -> {
                    if(covermenu_visible == false && menu_visible == false && loadingPane_visible == false) nav.backScene();
                    if(covermenu_visible) doHideCoverMenu();
                    e.consume();
                }
            }
        });
        
        myScene = fullPanel;
        return myScene;
    }
    
    public void doConfigCoverMenu(){
        coverMenuImageView = new ImageView();
        coverMenuImageView.setPreserveRatio(true);
        coverMenuImageView.fitHeightProperty().bind(coverPane.heightProperty());
        StackPane stackpane = new StackPane(coverMenuImageView);
        stackpane.setMinSize(0, 0);
        
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
    
    public void doConfigLoadingPane(){
        ImageView gifView = new ImageView(loadingGif);
        gifView.setFitWidth(150);
        gifView.setFitHeight(150);
        gifView.setPreserveRatio(true);
        
        Label loadingLabel = new Label("Cargando...");
        loadingLabel.getStyleClass().add("reader-loading-label");
        
        VBox loadingOverlay = new VBox(15, gifView, loadingLabel);
        loadingOverlay.setAlignment(Pos.CENTER);
        
        loadingPane = new BorderPane();
        loadingPane.setCenter(loadingOverlay);
    }
    
    private void doShowLoadingPane(){
        loadingPane_visible = true;
        loadingPane.setVisible(loadingPane_visible);
    }
    private void doHideLoadingPane(){
        loadingPane_visible = false;
        loadingPane.setVisible(loadingPane_visible);
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
        
        // Limpiamos listeners si la escena se recargó con F5
        if (widthListener != null) coverContainer.widthProperty().removeListener(widthListener);
        if (heightListener != null) coverContainer.heightProperty().removeListener(heightListener);
        
        currentCoverImage = app.simplereader.service.Cache.getInstance().getCoverMenuCache().get(url, k -> new Image(k, true));
        
        coverView.setImage(currentCoverImage);
        if (bgView != null) bgView.setImage(currentCoverImage);
        
        if (coverMenuImageView != null) {
            coverMenuImageView.setImage(currentCoverImage);
        }
        
        coverView.setOpacity(0.0);
        coverView.setCache(true);
        coverView.setCacheHint(javafx.scene.CacheHint.SPEED); 
        
        if (bgView != null) {
            bgView.setOpacity(1.0);
        }
        
        placeholder.setVisible(true);
        
        Runnable updateImageFit = () -> {
            if (currentCoverImage.getProgress() < 1.0 || currentCoverImage.isError()) return;

            double imgW = currentCoverImage.getWidth();
            double imgH = currentCoverImage.getHeight();
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

        widthListener = (obs, oldV, newV) -> updateImageFit.run();
        heightListener = (obs, oldV, newV) -> updateImageFit.run();
        
        coverContainer.widthProperty().addListener(widthListener);
        coverContainer.heightProperty().addListener(heightListener);
        
        coverContainer.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                doShowCoverMenu();
            }
        });
         currentCoverImage.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() >= 1.0 && !currentCoverImage.isError()) {
                updateImageFit.run();
                
                javafx.application.Platform.runLater(() -> {
                    coverView.setOpacity(1.0);
                    placeholder.setVisible(false);
                    
                    if (bgView != null) {
                        bgView.setOpacity(1.0);
                    }
                });
            }
        });
        
        if (currentCoverImage.getProgress() >= 1.0) {
            updateImageFit.run();
            coverView.setOpacity(1.0);
            placeholder.setVisible(false);
            if (bgView != null) {
                bgView.setOpacity(1.0);
            }
        }       }
    
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
        menu_visible = true;
        categoryMenu.setVisible(menu_visible);
        categoryPane.setVisible(menu_visible);
    }
    
    private void doHideMenu(){
        menu_visible = false;
        categoryMenu.setVisible(menu_visible);
        categoryPane.setVisible(menu_visible);
    }
    
    private void doShowCoverMenu(){
        covermenu_visible = true;
        coverPane.setOpacity(1.0); 
        coverPane.setVisible(true);
    }
    
    private void doHideCoverMenu(){
        covermenu_visible = false;
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
        
        categoryButtons.getChildren().clear();
        categoryButtons.setSpacing(10); 

        for(Category category : lib.getAllCategories()){
            CheckBox chkCategory = new CheckBox(category.getName());
            chkCategory.getStyleClass().add("category-checkbox"); 
            chkCategory.setMaxSize(Double.MAX_VALUE, 30);
            
            // Si el manga está en la categoría, el CheckBox se marca automáticamente
            chkCategory.setSelected(lib.onCategory(category, manga));
            chkCategory.setUserData(category); // Guardamos la categoría para usarla luego en el botón Aceptar

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