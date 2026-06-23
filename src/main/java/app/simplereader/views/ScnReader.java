package app.simplereader.views;

import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.service.Logger;
import app.simplereader.controller.ReaderController;
import app.simplereader.controller.SceneController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import app.simplereader.repository.AppScene;
import app.simplereader.views.components.SvgIcons;
import java.util.List;

/**
 *
 * @author david
 */
public class ScnReader implements AppScene {

    private final SceneController nav;
    private final ReaderController controller;
    private final SvgIcons icons = SvgIcons.get();
    
    private Manga manga;
    private Chapter chapter;
    private int chapterIndex;

    private static ScnReader instance;
    private javafx.scene.Parent myScene;
    private String pageLabelText = "";
    
    private Label pages;
    private StackPane pagePane;
    private ImageView visor;
    private ScrollPane scrollVisor;
    private Parent layout;
    private ComboBox<Integer> pagina;
    private ComboBox<Chapter> caps;
    private Parent lateralMenu;
    private Label chnameLabel;
    private Label mangaTitleLabel;
    
    private final StackPane FullScreenIcon = icons.getFullScreenIcon();
    private final StackPane ExitFullScreenIcon = icons.getExitFullScreenIcon();
    
    private javafx.beans.value.ChangeListener<Boolean> fullScreenListener;
    private javafx.event.EventHandler<javafx.stage.WindowEvent> originalCloseHandler;
    
    private javafx.scene.image.Image loadingGif;
    private javafx.scene.image.Image errorGif;
    private VBox loadingOverlay;
    private VBox errorOverlay;
    private VBox noPagesOverlay;
    
    private ScnReader() {
            this.nav = SceneController.getInstance();
            ReaderController.doInstance(this);
            this.controller = ReaderController.getInstance();
            
            try {
                loadingGif = new javafx.scene.image.Image(getClass().getResource("/icons/koruko.gif").toExternalForm());
                errorGif = new javafx.scene.image.Image(getClass().getResource("/icons/bochi.gif").toExternalForm());
            } catch (Exception e) {
                Logger.error("No se pudo cargar los gifs: " + e.getMessage());
            }
    }

    public static ScnReader getInstance() {
        if (instance == null) {
            instance = new ScnReader();
        }
        return instance;
    }
    
    public void updateReader(Manga manga, Chapter chapter, int chapterIndex) {                                   
        this.manga = manga;                                                                                      
        this.chapter = chapter;                                                                                  
        this.chapterIndex = chapterIndex;                                                                        

        if (mangaTitleLabel != null) {
            mangaTitleLabel.setText(manga.getTitle());
        }

        controller.init(manga, chapter, chapterIndex);
                                          
        if (myScene != null) {                                                                                   
            controller.loadChapter(chapter, chapterIndex);                                                       
        }

        Logger.info("Loaded " + chapter.getTitle() + " " + chapterIndex);                                        
    }   
        
    
    @Override
    public javafx.scene.Parent getScene() {
        
        if (originalCloseHandler == null) {
            originalCloseHandler = nav.getStage().getOnCloseRequest();
        }
        nav.getStage().setOnCloseRequest(e -> {
            controller.saveAndCleanup();
        });
        
        if (fullScreenListener == null) {
            fullScreenListener = (obs, oldVal, isFull) -> {
                if (layout != null) {
                    if (isFull) {
                        layout.getStyleClass().add("fullscreen");
                    } else {
                        layout.getStyleClass().remove("fullscreen");
                    }
                }
            };
        }
        nav.getStage().fullScreenProperty().removeListener(fullScreenListener);
        nav.getStage().fullScreenProperty().addListener(fullScreenListener);
        
        if (myScene != null) {
            if (nav.getStage().isFullScreen() && !myScene.getStyleClass().contains("fullscreen")) {
                myScene.getStyleClass().add("fullscreen");
            } else if (!nav.getStage().isFullScreen()) {
                myScene.getStyleClass().remove("fullscreen");
            }
            javafx.application.Platform.runLater(() -> myScene.requestFocus());
            return myScene;
        }

        if (layout == null) layout = getPane();
        
        if (nav.getStage().isFullScreen() && !layout.getStyleClass().contains("fullscreen")) {
            layout.getStyleClass().add("fullscreen");
        } else if (!nav.getStage().isFullScreen()) {
            layout.getStyleClass().remove("fullscreen");
        }
        
        // Listeners de teclado (en el root, no en la Scene)
        layout.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    controller.resetZoom();
                    controller.reloadCurrentImage();
                }
                case SPACE -> {
                    controller.nextPage();
                    e.consume();
                }
                case ESCAPE -> {
                    controller.cleanupResources();
                    e.consume();

                    nav.getStage().setOnCloseRequest(originalCloseHandler);
                    nav.getStage().fullScreenProperty().removeListener(fullScreenListener);
                    nav.backScene();
                }
                case RIGHT -> {
                    boolean isLTR = AppConfig.get().READING_DIR.equals("LTR");
                    if (e.isShiftDown()) {
                        if (isLTR) controller.nextChapter(); else controller.previousChapter();
                    } else {
                        if (isLTR) controller.nextPage(); else controller.previousPage();
                    }
                    e.consume();
                }
                case LEFT -> {
                    boolean isLTR = AppConfig.get().READING_DIR.equals("LTR");
                    if (e.isShiftDown()) {
                        if (isLTR) controller.previousChapter(); else controller.nextChapter();
                    } else {
                        if (isLTR) controller.previousPage(); else controller.nextPage();
                    }
                    e.consume();    
                }
            }
        });
        
        myScene = layout;
        javafx.application.Platform.runLater(() -> myScene.requestFocus());
        return myScene;
    }
    
    private Parent getPane() {
        visor = new ImageView();
        visor.setPreserveRatio(true);
        visor.setSmooth(true);

        Group zoomGroup = new Group(visor);
        StackPane visorpanel = new StackPane(zoomGroup);
        visorpanel.setAlignment(Pos.CENTER);

        scrollVisor = new ScrollPane(visorpanel);
        scrollVisor.setPannable(true);
        scrollVisor.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollVisor.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollVisor.setFitToWidth(true);
        scrollVisor.setFitToHeight(true);

        scrollVisor.addEventFilter(ScrollEvent.ANY, event -> {
            if (event.isControlDown()) {
                controller.setInZoom(true);
                double deltaY = event.getDeltaY();
                if (deltaY == 0) return;

                double zoomFactor = (deltaY > 0) ? 1.1 : 1 / 1.1;
                double newScaleX = visor.getScaleX() * zoomFactor;

                if (newScaleX >= 10.0) {
                    event.consume();
                    return;
                }

                if (newScaleX <= 1.0) {
                    controller.resetZoom();
                    event.consume();
                    return;
                }
                
                controller.setInZoom(true);

                double mouseX = event.getX();
                double mouseY = event.getY();
                double scrollH = scrollVisor.getHvalue();
                double scrollV = scrollVisor.getVvalue();
                double contentW = visor.getBoundsInParent().getWidth();
                double contentH = visor.getBoundsInParent().getHeight();

                visor.setScaleX(newScaleX);
                visor.setScaleY(newScaleX);

                double newContentW = visor.getBoundsInParent().getWidth();
                double newContentH = visor.getBoundsInParent().getHeight();
                double viewW = scrollVisor.getViewportBounds().getWidth();
                double viewH = scrollVisor.getViewportBounds().getHeight();

                double newScrollH = (scrollH * (contentW - viewW) + mouseX * (zoomFactor - 1)) / (newContentW - viewW);
                double newScrollV = (scrollV * (contentH - viewH) + mouseY * (zoomFactor - 1)) / (newContentH - viewH);

                scrollVisor.setHvalue(Math.max(0, Math.min(1, newScrollH)));
                scrollVisor.setVvalue(Math.max(0, Math.min(1, newScrollV)));

                event.consume();
            }
        });
        
        ImageView gifView = new ImageView(loadingGif);
        gifView.setFitWidth(150);
        gifView.setFitHeight(150);
        gifView.setPreserveRatio(true);
        
        Label loadingLabel = new Label("Cargando...");
        loadingLabel.getStyleClass().add("reader-loading-label");
        
        loadingOverlay = new VBox(15, gifView, loadingLabel);
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.setMouseTransparent(true);
        loadingOverlay.setVisible(true);
        
        // Error overlay
        ImageView errorGifView = new ImageView(errorGif);
        errorGifView.setFitWidth(150);
        errorGifView.setFitHeight(150);
        errorGifView.setPreserveRatio(true);
        
        Label errorLabel = new Label("Error cargando la imagen");
        errorLabel.getStyleClass().add("reader-error-label");
        
        errorOverlay = new VBox(15, errorGifView, errorLabel);
        errorOverlay.setAlignment(Pos.CENTER);
        errorOverlay.setMouseTransparent(true);
        errorOverlay.setVisible(false);
        
        // No pages overlay
        ImageView noPagesGifView = new ImageView(errorGif);
        noPagesGifView.setFitWidth(150);
        noPagesGifView.setFitHeight(150);
        noPagesGifView.setPreserveRatio(true);
        
        Label noPagesLabel = new Label("No hay páginas...");
        noPagesLabel.getStyleClass().add("reader-error-label");
        
        noPagesOverlay = new VBox(15, noPagesGifView, noPagesLabel);
        noPagesOverlay.setAlignment(Pos.CENTER);
        noPagesOverlay.setMouseTransparent(true);
        noPagesOverlay.setVisible(false);
                
        StackPane spane = new StackPane();
        spane.getChildren().add(scrollVisor);
        spane.getChildren().add(loadingOverlay);
        spane.getChildren().add(errorOverlay);
        spane.getChildren().add(noPagesOverlay);
        doConfigPagePane();
        spane.getChildren().add(pagePane);
        StackPane.setAlignment(pagePane, Pos.BOTTOM_RIGHT);
        lateralMenu = getLateralMenu();
        StackPane.setAlignment(lateralMenu, Pos.CENTER_LEFT);
        spane.getChildren().add(lateralMenu);
        
        scrollVisor.widthProperty().addListener((obs, oldVal, newVal) -> fitImageToScreen());
        scrollVisor.heightProperty().addListener((obs, oldVal, newVal) -> fitImageToScreen());

        scrollVisor.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.getWidth() > 0 && newVal.getHeight() > 0) {
                fitImageToScreen();
            }
        });
        // Detección manual de clic (sin arrastre) sobre el scrollPane
        final double[] pressXY = new double[2];
        
        scrollVisor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                pressXY[0] = e.getScreenX();
                pressXY[1] = e.getScreenY();
            }
        });
        
        scrollVisor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            
            double dx = Math.abs(e.getScreenX() - pressXY[0]);
            double dy = Math.abs(e.getScreenY() - pressXY[1]);
            
            // Si se movió más de 5px, fue un arrastre, no un clic
            if (dx > 5 || dy > 5) return;

            // Si el menú ya está abierto, cerrarlo
            if (controller.isMenuVisible()) {
                controller.hideMenu();
                return;
            }

            // Si estamos en zoom, cualquier clic abre el menú
            if (visor.getScaleX() != 1.0) {
                controller.showMenu();
                return;
            }

            // Sin zoom: zonas izquierda/derecha pasan página, centro abre menú
            double width = scrollVisor.getViewportBounds().getWidth();
            double x = e.getScreenX() - scrollVisor.localToScreen(scrollVisor.getBoundsInLocal()).getMinX();
            double leftZone = width * 0.4;
            double rightZone = width * 0.6;

            if (x < leftZone) {
                if (AppConfig.get().READING_DIR.equals("LTR")) controller.previousPage();
                else controller.nextPage();
            } else if (x > rightZone) {
                if (AppConfig.get().READING_DIR.equals("LTR")) controller.nextPage();
                else controller.previousPage();
            } else {
                controller.showMenu();
            }
        });

        if (chapter.hasPages()) {
            controller.loadCurrentImage();
        }
        doUpdatePageLabel();
        
        spane.getStyleClass().add("reader");
        return spane;
    }
    
    private Parent getLateralMenu() {
        BorderPane lateralMenu = new BorderPane();
        
        SVGPath icnClose = new SVGPath();
        icnClose.setContent("M660-320v-320L500-480l160 160ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm120-80v-560H200v560h120Zm80 0h360v-560H400v560Zm-80 0H200h120Z");
        icnClose.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        icnClose.setScaleX(scale);
        icnClose.setScaleY(scale);
        
        Group icon_close_group = new Group(icnClose);
        StackPane icon_close = new StackPane(icon_close_group);
        icon_close.setPrefSize(24, 24);
        icon_close.setMaxSize(24, 24);
                       
        Button btnCloseMenu = new Button("", icon_close);
        btnCloseMenu.getStyleClass().add("reader-button");
        btnCloseMenu.setOnAction(e -> controller.hideMenu());
        btnCloseMenu.setMinSize(24, 24);
        btnCloseMenu.setMaxSize(24, 24);
        
        SVGPath icnBack = new SVGPath();
        icnBack.setContent("M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z");
        icnBack.getStyleClass().add("icon");
        icnBack.setScaleX(scale);
        icnBack.setScaleY(scale);
        
        Group icon_back_group = new Group(icnBack);
        StackPane icon_back = new StackPane(icon_back_group);
        icon_back.setPrefSize(24, 24);
        icon_back.setMaxSize(24, 24);
        Button btnBackToMenu = new Button("", icon_back);
        btnBackToMenu.getStyleClass().add("reader-button");
        btnBackToMenu.setOnAction(e -> {
            controller.cleanupResources();
            nav.getStage().setOnCloseRequest(originalCloseHandler); 
            nav.getStage().setFullScreen(false);
            nav.getStage().fullScreenProperty().removeListener(fullScreenListener);
            nav.backScene();
        });
        btnBackToMenu.setMinSize(24, 24);
        btnBackToMenu.setMaxSize(24, 24);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox topButtons = new HBox(btnBackToMenu, spacer, btnCloseMenu);
        
        mangaTitleLabel = new Label(manga.getTitle());
        mangaTitleLabel.getStyleClass().add("reader-menu-title");
        HBox titulo = new HBox(icnBook(), mangaTitleLabel);
        titulo.setPadding(new Insets(5));
        titulo.setSpacing(10);
        chnameLabel = new Label(this.chapter.getTitle());
        chnameLabel.getStyleClass().add("reader-menu-chname");
        VBox labels = new VBox(titulo, chnameLabel);
        VBox top = new VBox(topButtons, labels);
        top.getStyleClass().add("vbox-padding");
        
        Button btnBack = new Button("", icn_Back());
        btnBack.setMinSize(40, 50);
        btnBack.setMaxSize(40, 50);
        btnBack.getStyleClass().add("reader-button2");
        pagina = new ComboBox<>();
        pagina.setMinHeight(50);
        for (int i = 1; i <= chapter.getPageCount(); i++) {
            pagina.getItems().add(i);
        }
        pagina.setValue(controller.getCurrentPageIndex() + 1);
        pagina.getStyleClass().add("reader-combobox");
        Button btnNext = new Button("", icn_Next());
        btnNext.getStyleClass().add("reader-button2");
        btnNext.setMinSize(40, 50);
        btnNext.setMaxSize(40, 50);
        HBox paginas = new HBox(btnBack, pagina, btnNext);
        
        Button btnBackCh = new Button("", icn_Back());
        btnBackCh.setMinSize(40, 50);
        btnBackCh.setMaxSize(40, 50);
        btnBackCh.getStyleClass().add("reader-button2");
        caps = new ComboBox<>();
        caps.setMinHeight(50);
        List<Chapter> chapters = manga.getChapters();
        if (chapters != null) {
            for (Chapter cap : chapters) {
                caps.getItems().add(cap);
            }
        }
        caps.setValue(this.chapter);
        caps.setOnAction(e -> {
            if (controller.isUpdatingUI()) return;
            
            Chapter selected = caps.getValue();
            if (selected != null) {
                int index = caps.getItems().indexOf(selected);
                if (index >= 0) {
                    controller.changeToChapter(selected, index);
                }
            }
        });
        caps.setCellFactory(list -> new javafx.scene.control.ListCell<Chapter>() {
            @Override
            protected void updateItem(Chapter item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle());
            }
        });
        caps.setButtonCell(new javafx.scene.control.ListCell<Chapter>() {
            @Override
            protected void updateItem(Chapter item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle());
            }
        });
        caps.getStyleClass().add("reader-combobox");
        
        Button btnNextCh = new Button("", icn_Next());
        btnNextCh.getStyleClass().add("reader-button2");
        btnNextCh.setMinSize(40, 50);
        btnNextCh.setMaxSize(40, 50);
        btnNext.setOnAction(e -> {
            if (AppConfig.get().READING_DIR.equals("LTR")) controller.nextPage();
            else controller.previousPage();
        });
        btnBack.setOnAction(e -> {
            if (AppConfig.get().READING_DIR.equals("LTR")) controller.previousPage();
            else controller.nextPage();
        });
        btnNextCh.setOnAction(e -> {
            if (AppConfig.get().READING_DIR.equals("LTR")) controller.nextChapter();
            else controller.previousChapter();
        });
        btnBackCh.setOnAction(e -> {
            if (AppConfig.get().READING_DIR.equals("LTR")) controller.previousChapter();
            else controller.nextChapter();
        });
        
        HBox capitulo = new HBox(btnBackCh, caps, btnNextCh);
        
        //Botones de config
        Button btnFullScreen = new Button("", nav.getStage().isFullScreen() ? icons.getExitFullScreenIcon() : icons.getFullScreenIcon());
        btnFullScreen.getStyleClass().add("reader-button2");
        btnFullScreen.setMinHeight(48);
        btnFullScreen.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnFullScreen, Priority.ALWAYS);
        
        btnFullScreen.setOnAction(e -> {
            boolean isFull = nav.getStage().isFullScreen();
            nav.getStage().setFullScreen(!isFull);
        });
        
        nav.getStage().fullScreenProperty().addListener((obs, oldV, isFull) -> {
            btnFullScreen.setGraphic(isFull ? icons.getExitFullScreenIcon() : icons.getFullScreenIcon());
        });

        Button btnZoomIn = new Button("",icons.getZoomInIcon());
        btnZoomIn.getStyleClass().add("reader-button2");
        btnZoomIn.setMinHeight(48);
        btnZoomIn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnZoomIn, Priority.ALWAYS);
        btnZoomIn.setOnAction(e -> {
            controller.setInZoom(true);
            double newScale = visor.getScaleX() * 1.1;
            if(newScale <= 10.0) {
                visor.setScaleX(newScale);
                visor.setScaleY(newScale);
            }
        });
        
        Button btnZoomOut = new Button("",icons.getZoomOutIcon());
        btnZoomOut.getStyleClass().add("reader-button2");
        btnZoomOut.setMinHeight(48);
        btnZoomOut.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnZoomOut, Priority.ALWAYS);
        btnZoomOut.setOnAction(e -> {
            double newScale = visor.getScaleX() / 1.1;
            if (newScale <= 1.0) {
                controller.resetZoom();
            } else {
                visor.setScaleX(newScale);
                visor.setScaleY(newScale);
            }
        });
        
        Button btnConfig = new Button("",icons.getConfigIcon());
        btnConfig.getStyleClass().add("reader-button2");
        btnConfig.setMinHeight(48);
        btnConfig.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfig, Priority.ALWAYS);
        
        btnConfig.setOnAction(e -> {
            nav.goTo(new ScnConfig());
        });
        
        
        
        HBox zoomBox = new HBox(btnZoomIn,btnZoomOut,btnFullScreen);
        VBox bottom = new VBox(paginas, capitulo,zoomBox,btnConfig);
        paginas.setSpacing(5);
        capitulo.setSpacing(5);
        zoomBox.setSpacing(5);
        bottom.setSpacing(5);
        
        HBox.setHgrow(pagina, Priority.ALWAYS);
        pagina.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caps, Priority.ALWAYS);
        caps.setMaxWidth(Double.MAX_VALUE);
        
        lateralMenu.setPrefWidth(300);
        lateralMenu.setMaxWidth(300);
        lateralMenu.setTop(top);
        lateralMenu.setBottom(bottom);
        
        pagina.setOnAction(e -> {
            if (controller.isUpdatingUI()) return;
            Integer selected = pagina.getValue();
            if (selected != null) {
                controller.goToPage(selected - 1);
            }
        });
        
        lateralMenu.getStyleClass().add("reader-menu");
        
        controller.setUIComponents(pagina, caps, chnameLabel);
        
        return lateralMenu;
    }
    
    private void doConfigPagePane(){
        if(pagePane == null) pagePane = new StackPane();
        if(pages == null) pages = new Label();
        
        pagePane.setMouseTransparent(true);
        
        HBox box = new HBox(5);
        box.setPadding(new Insets(5)); // Un poco de margen para que no toque el borde
        pageLabelText = "0"+"/"+String.valueOf(chapter.getPageCount());
        pages = new Label(pageLabelText);
        
        // Aplica la clase CSS y el modo de fusión
        pages.getStyleClass().add("reader-page-label");
        pages.setBlendMode(javafx.scene.effect.BlendMode.DIFFERENCE);
        
        box.getChildren().add(pages);
        box.setAlignment(Pos.BOTTOM_RIGHT);
        pagePane.getChildren().add(box);
    }
    
    public void doUpdatePageLabel(){
        pageLabelText = String.valueOf(controller.getCurrentPageIndex()+1)+"/"+String.valueOf(chapter.getPageCount());
        pages.setText(pageLabelText);
        if (pagePane != null) {
            pagePane.setVisible(chapter.getPageCount() > 1);
        }
    }
    
    private StackPane icn_Back() {
        double scale = 24.0 / 960.0;
        SVGPath icnBackChapter = new SVGPath();
        icnBackChapter.setContent("m313-440 224 224-57 56-320-320 320-320 57 56-224 224h487v80H313Z");
        icnBackChapter.getStyleClass().add("icon");
        icnBackChapter.setScaleX(scale);
        icnBackChapter.setScaleY(scale);
        Group icon_backch_group = new Group(icnBackChapter);
        StackPane icon_backCh = new StackPane(icon_backch_group);
        icon_backCh.setPrefSize(24, 24);
        icon_backCh.setMaxSize(24, 24);
        return icon_backCh;
    }
    
    private StackPane icnBook() {
        double scale = 24.0 / 960.0;
        SVGPath icnBooksvg = new SVGPath();
        icnBooksvg.setContent("M240-80q-33 0-56.5-23.5T160-160v-640q0-33 23.5-56.5T240-880h480q33 0 56.5 23.5T800-800v640q0 33-23.5 56.5T720-80H240Zm0-80h480v-640h-80v280l-100-60-100 60v-280H240v640Zm0 0v-640 640Zm200-360 100-60 100 60-100-60-100 60Z");
        icnBooksvg.getStyleClass().add("icon");
        icnBooksvg.setScaleX(scale);
        icnBooksvg.setScaleY(scale);
        Group icnbook = new Group(icnBooksvg);
        return new StackPane(icnbook);
    }
    
    private StackPane icn_Next() {
        double scale = 24.0 / 960.0;
        SVGPath icnNext = new SVGPath();
        icnNext.setContent("M647-440H160v-80h487L423-744l57-56 320 320-320 320-57-56 224-224Z");
        icnNext.getStyleClass().add("icon");
        icnNext.setScaleX(scale);
        icnNext.setScaleY(scale);
        Group icon_next = new Group(icnNext);
        StackPane icon_nextpane = new StackPane(icon_next);
        icon_nextpane.setPrefSize(24, 24);
        icon_nextpane.setMaxSize(24, 24);
        return icon_nextpane;
    }
    
    public void setImageViewImage(javafx.scene.image.Image img) {
        if (visor != null) {
            visor.setImage(img);
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(img == null);
            }
            if (errorOverlay != null) {
                errorOverlay.setVisible(false);
            }
            if (noPagesOverlay != null) {
                noPagesOverlay.setVisible(false);
            }
        }
    }
    
    public void showErrorOverlay() {
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
        if (noPagesOverlay != null) noPagesOverlay.setVisible(false);
        if (errorOverlay != null) errorOverlay.setVisible(true);
    }
    
    public void showNoPagesOverlay() {
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
        if (errorOverlay != null) errorOverlay.setVisible(false);
        if (noPagesOverlay != null) noPagesOverlay.setVisible(true);
    }
    
    public void setMenuVisible(boolean visible) {
        if (lateralMenu != null) {
            lateralMenu.setVisible(visible);
            lateralMenu.setManaged(visible);
        }
    }
    
    public void fitImageToScreen() {
        if (visor == null || visor.getImage() == null || scrollVisor == null) return;
        
        double containerW = scrollVisor.getViewportBounds().getWidth() - 2;
        double containerH = scrollVisor.getViewportBounds().getHeight() - 2;
        if (containerW <= 0 || containerH <= 0) return;

        double imgW = visor.getImage().getWidth();
        double imgH = visor.getImage().getHeight();
        double ratioX = containerW / imgW;
        double ratioY = containerH / imgH;
        double scale = Math.min(ratioX, ratioY);
        
        // Si el usuario prefiere "Ajustar al Ancho" (ideal para Webtoons)
        if ("FIT_WIDTH".equals(AppConfig.get().SCALING_MODE)) {
            scale = ratioX;
        }
        
        visor.setFitWidth(imgW * scale);
        visor.setFitHeight(imgH * scale);
    }    
        
    public void setChapter(Chapter chapter){
        this.chapter = chapter;
    }
    
    public ImageView getVisor(){
        return this.visor;
    }
    
    @Override
    public String getName() {
        return manga.getTitle() + " - " + chapter.getTitle();
    }
    @Override
    public String getParentName() {
        return "Reader";
    }
    
}