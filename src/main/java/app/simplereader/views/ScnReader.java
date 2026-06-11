package app.simplereader.views;

import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.controller.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.ReaderController;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
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
import app.simplereader.repository.MangaSource;
import java.util.List;
import javafx.application.Platform;

/**
 *
 * @author david
 */
public class ScnReader implements AppScene {

    private final SceneController nav;
    private final ReaderController controller;
    
    private Manga manga;
    private Chapter chapter;
    private int chapterIndex;

    private static ScnReader instance;
    private Scene myScene;
        
    private ImageView visor;
    private ScrollPane scrollVisor;
    private Parent layout;
    private ComboBox<Integer> pagina;
    private ComboBox<Chapter> caps;
    private Parent lateralMenu;
    private Label chnameLabel;
    private Label mangaTitleLabel;
    
    private javafx.beans.value.ChangeListener<Boolean> fullScreenListener;
    private javafx.event.EventHandler<javafx.stage.WindowEvent> originalCloseHandler;
    
    private ScnReader() {
            this.nav = SceneController.getInstance();
            nav.getStage().setResizable(true);
            ReaderController.doInstance(this);
            this.controller = ReaderController.getInstance();
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
        
        // Si la pantalla ya fue construida al menos una vez,                                                    
        // usamos el método loadChapter para forzar la actualización de la UI.                                   
        if (myScene != null) {                                                                                   
            controller.loadChapter(chapter, chapterIndex);                                                       
        }

        Logger.info("Loaded " + chapter.getTitle() + " " + chapterIndex);                                        
    }   
        
    
    @Override
    public Scene getScene() {
        
        if (myScene != null) return myScene;

        if (layout == null) layout = getPane();
        
        nav.getStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);  
        
        myScene = new Scene(layout, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        myScene.getStylesheets().add(nav.getCss());
        
        myScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> {
                    controller.resetZoom();
                }
                case F11 -> {
                    boolean isFull = nav.getStage().isFullScreen();
                    nav.getStage().setFullScreen(!isFull);
                }
                case ESCAPE -> {
                    controller.cleanupResources();
                    e.consume();

                    // 1. Quitamos la pantalla completa inmediatamente
                    nav.getStage().setFullScreen(false);

                    // 2. Damos un margen de tiempo un poco mayor (200ms suele ser infalible)
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                    delay.setOnFinished(event -> {
                        // 3. Restauramos las propiedades de la ventana cuando ya no está en transición
                        nav.getStage().setMaximized(false);
                        nav.getStage().setResizable(false);
                        nav.getStage().setOnCloseRequest(originalCloseHandler);

                        // 4. CAMBIAMOS DE ESCENA al final, cuando la ventana ya es estable
                        nav.getStage().fullScreenProperty().removeListener(fullScreenListener);
                        nav.backScene();
                    });
                    delay.play();
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
        
        fullScreenListener = (obs, oldVal, isFull) -> {
            if (isFull) {
                layout.getStyleClass().add("fullscreen");
            } else {
                layout.getStyleClass().remove("fullscreen");
            }
        };
        nav.getStage().fullScreenProperty().addListener(fullScreenListener);
        originalCloseHandler = nav.getStage().getOnCloseRequest();
        
        nav.getStage().setOnCloseRequest(e -> {
            controller.saveAndCleanup();
        });
        
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

                if (newScaleX <= 0.1 || newScaleX >= 10.0) {
                    event.consume();
                    return;
                }

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
                
        StackPane spane = new StackPane();
        spane.getChildren().add(scrollVisor);
        
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
        spane.setOnMouseClicked(e -> {
            double width = spane.getWidth();
            double x = e.getX();
            double leftZone = width * 0.4;
            double rightZone = width * 0.6;

            if (controller.isMenuVisible()) {
                if (x > 300) {
                    controller.hideMenu();
                }
                return;
            }

            if (x < leftZone) {
                if (!controller.isInZoom()) {
                    if (AppConfig.get().READING_DIR.equals("LTR")) controller.previousPage();
                    else controller.nextPage();
                }
            } else if (x > rightZone) {
                if (!controller.isInZoom()) {
                    if (AppConfig.get().READING_DIR.equals("LTR")) controller.nextPage();
                    else controller.previousPage();
                }
            } else {
                if (!controller.isInZoom()) controller.showMenu();
            }
        });

        if (chapter.hasPages()) {
            controller.loadCurrentImage();
        }
        
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
            nav.getStage().setResizable(false);
            nav.getStage().setMaximized(false);
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
            if (controller.isProgrammaticNav()) return;
            
            Chapter selected = caps.getValue();
            if (selected != null) {
                int index = chapters.indexOf(selected);
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
        
        VBox bottom = new VBox(paginas, capitulo);
        paginas.setSpacing(5);
        capitulo.setSpacing(5);
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
        }
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