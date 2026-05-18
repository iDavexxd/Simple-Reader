package app.simplereader.views;

import app.simplereader.controller.LibraryController;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.controller.Logger;
import app.simplereader.controller.ReaderController;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
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
import java.util.HashSet;

import java.util.List;
import java.util.Set;

/**
 *
 * @author david
 */
public class ScnReader implements AppScene {

    private final SceneController nav;
    private final ReaderController controller;
    
    private Chapter chapter;
    private int chapternum;
    private final Manga manga;

    private ImageView visor;
    private ScrollPane scrollVisor;
    private int indiceactual = 0;
    private Parent layout;
    private ComboBox<Integer> pagina;
    private ComboBox<Chapter> caps;
    private Parent lateralMenu;
    private static Label chnameLabel;
    
    private boolean updatingUI;
    private boolean menuVisible = true;
    private boolean inZoom = false;

    private javafx.event.EventHandler<javafx.stage.WindowEvent> originalCloseHandler;
    
    private final Set<Integer> loadingPages = java.util.Collections.synchronizedSet(new HashSet<>());
    
    private final Map<Integer, Image> cache = java.util.Collections.synchronizedMap(new HashMap<>());
    
    private ExecutorService preloader = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "preloader");
        t.setDaemon(true);
        return t;
    });
    
    public ScnReader(SceneController nav, Manga manga, Chapter chapter, int indice) {
        this.nav = nav;
        this.chapter = chapter;
        this.chapternum = indice;
        this.manga = manga;
        this.indiceactual = chapter.isReaded() ? 0 : chapter.getLastRead();
        
        ReaderController.doInstance(this);
        this.controller = ReaderController.getInstance();
        nav.getStage().setResizable(true);
        Logger.info("Loaded " + chapter.getTitle() + " " + chapternum);
    }
    
    @Override
    public Scene getScene() {
        if (layout == null) layout = getPane();
        
        nav.getStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);  
        
        Scene scene = new Scene(layout, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode key = e.getCode();
            switch (key) {
                case F5 -> LoadImage();
                case F11 -> {
                    boolean isFull = nav.getStage().isFullScreen();
                    nav.getStage().setFullScreen(!isFull);
                }
                case ESCAPE -> {
                    cache.clear();
                    preloader.shutdownNow();
                    nav.getStage().setResizable(false);
                    nav.getStage().setOnCloseRequest(null);
                    nav.getStage().setMaximized(false);
                    nav.getStage().setOnCloseRequest(originalCloseHandler); 
                    nav.backScene();
                }
                case RIGHT -> {
                    if (e.isShiftDown()) {
                        NextChapter();
                    } else {
                        NextPage();   
                    }
                    e.consume();
                }
                case LEFT -> {
                    if (e.isShiftDown()) {
                        BackChapter();
                    } else {
                        BackPage();
                    }
                    e.consume();    
                }
            }
        });
        
        nav.getStage().fullScreenProperty().addListener((obs, oldVal, isFull) -> {
            if (isFull) {
                layout.getStyleClass().add("fullscreen");
            } else {
                layout.getStyleClass().remove("fullscreen");
            }
        });
        originalCloseHandler = nav.getStage().getOnCloseRequest();
        
        nav.getStage().setOnCloseRequest(e -> {
            cache.clear();
            preloader.shutdownNow();
            LibraryController.getInstance().saveLibrary();
            // Nota: Si quieres que la app se cierre aquí, añade Platform.exit();
        });
        
        return scene;
    }
    
    private void loadChapter(Chapter chapter, int index){
        // 1. PRIMERO: Limpiar todo lo del capítulo anterior
        cache.clear(); 
        preloader.shutdownNow(); // Detiene las descargas en segundo plano
        // Reinicia el pool de hilos para el nuevo capítulo
        preloader = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "preloader");
            t.setDaemon(true);
            return t;
        });
        // 2. AHORA: Configurar el nuevo capítulo
        this.chapter = chapter;
        this.chapternum = index;
        this.indiceactual = chapter.isReaded() ? 0 : chapter.getLastRead();
        
        nav.getStage().setTitle(this.getName());
        
        // Actualizar el ComboBox de páginas si existe
        if (pagina != null) {
            pagina.getItems().clear();
            for (int i = 1; i <= chapter.getPageCount(); i++) {
                pagina.getItems().add(i);
            }
            pagina.setValue(indiceactual + 1);
        }
        // 3. Cargar la imagen actual y precargar las siguientes
        if (totalPages() > 0) {
            LoadImage();
        }
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
                inZoom = true;
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

            if (menuVisible) {
                if (x > 300) {
                    hideMenu();
                }
                return;
            }

            if (x < leftZone) {
                if (!inZoom) BackPage();
            } else if (x > rightZone) {
                if (!inZoom) NextPage();
            } else {
                if (!inZoom) showMenu();
            }
        });

        if (chapter.hasPages()) {
            LoadImage();
        }
        
        spane.getStyleClass().add("reader");
        return spane;
    }
    
    private Parent getLateralMenu() {
        BorderPane lateralMenu = new BorderPane();
        
        SVGPath icnClose = new SVGPath();
        icnClose.setContent("m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z");
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
        btnCloseMenu.setOnAction(e -> hideMenu());
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
            cache.clear();
            nav.getStage().setOnCloseRequest(originalCloseHandler); 
            preloader.shutdownNow();
            nav.getStage().setResizable(false);
            nav.getStage().setMaximized(false);
            nav.backScene();
        });
        btnBackToMenu.setMinSize(24, 24);
        btnBackToMenu.setMaxSize(24, 24);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox topButtons = new HBox(btnBackToMenu, spacer, btnCloseMenu);
        
        Label title = new Label(manga.getTitle());
        title.getStyleClass().add("reader-menu-title");
        HBox titulo = new HBox(icnBook(), title);
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
        pagina.setValue(indiceactual + 1);
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
            Chapter selected = caps.getValue();
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                // Pedimos las páginas y se las asignamos al capítulo
                List<String> pages = src.getPages(manga.getMangaID(), selected.getChapterID());
                selected.setPages(pages);
            }
            
            if (selected != null) {
                int index = chapters.indexOf(selected);
                if (index >= 0) {
                    loadChapter(selected, index);
                    caps.setValue(selected);
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
        btnNext.setOnAction(e -> NextPage());
        btnBack.setOnAction(e -> BackPage());
        btnNextCh.setOnAction(e -> NextChapter());
        btnBackCh.setOnAction(e -> BackChapter());
        
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
            if (updatingUI) return;
            Integer selected = pagina.getValue();
            if (selected != null) {
                GoToPage(selected - 1);
            }
        });
        
        lateralMenu.getStyleClass().add("reader-menu");
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

    private void hideMenu() {
        menuVisible = false;
        lateralMenu.setVisible(false);
        lateralMenu.setManaged(false);
    }

    private void showMenu() {
        if (inZoom) return;
        menuVisible = true;
        lateralMenu.setVisible(true);
        lateralMenu.setManaged(true);
    }
    
    private void NextPage() {
        if (indiceactual < totalPages() - 1) {
            GoToPage(indiceactual + 1);
        } else {
            NextChapter();
        }   
    }
    private void BackPage() {
        if (indiceactual > 0) {
            GoToPage(indiceactual - 1);
        } else {
            BackChapter();
        }
    }
    private void GoToPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (index == totalPages() - 1){
            this.chapter.markAsReaded();
            LibraryController.getInstance().saveLibrary();
        }
        indiceactual = index;
        if (!chapter.isReaded()) chapter.setLastRead(indiceactual);
        LoadImage();

        if (pagina != null) {
            updatingUI = true;
            pagina.setValue(indiceactual + 1);
            updatingUI = false;
        }
    }
    private void NextChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        if (this.chapternum < chapters.size() - 1) {
            Chapter next = chapters.get(this.chapternum + 1);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                // Pedimos las páginas y se las asignamos al capítulo
                List<String> pages = src.getPages(manga.getMangaID(), next.getChapterID());
                next.setPages(pages);
            }
            
            if (next.hasPages()) {
                this.chapter.markAsReaded();
                chnameLabel.setText(next.getTitle());
                caps.setValue(next);
                loadChapter(next, this.chapternum + 1);
            } else {
                Logger.noPagesAlert(next.getTitle());
            }
        }
    }
    private void BackChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        if (this.chapternum > 0) {
            Chapter last = chapters.get(this.chapternum - 1);
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                // Pedimos las páginas y se las asignamos al capítulo
                List<String> pages = src.getPages(manga.getMangaID(), last.getChapterID());
                last.setPages(pages);
            }
            
            if (last.hasPages()) {
                chnameLabel.setText(last.getTitle());
                caps.setValue(last);
                pagina.setValue(last.getPageCount());
                loadChapter(last, this.chapternum - 1);
            } else {
                Logger.noPagesAlert(last.getTitle());
            }
        }  
    }
            
    private void resetZoom() {
        if (visor != null) {
            visor.setScaleX(1.0);
            visor.setScaleY(1.0);
            inZoom = false;
        }
    }
    
    private void LoadImage() {
        try {
            visor.setImage(null);
            Image img = getPage(indiceactual);

            if (img.isBackgroundLoading() && img.getProgress() < 1.0) {
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            visor.setImage(img);
                            fitImageToScreen();
                            resetZoom();
                        });
                    }
                });
            } else {
                visor.setImage(img);
            }

            preloadAroundCurrent();
            cleanCache();
            fitImageToScreen();
            resetZoom();

            Logger.info("Page " + indiceactual + " loaded.");
        } catch (Exception e) {
            Logger.error("Error cargando imagen: " + e.getMessage());
        }
    }
    
    private Image getPage(int index) {
        if (cache.containsKey(index)) return cache.get(index);
        Image img = new Image(chapter.getPage(index),true);
        cache.put(index, img);
        return img;
    }
    
    private void cleanCache() {
        int range = 3;
        System.gc();
        cache.keySet().removeIf(i -> Math.abs(i - indiceactual) > range);
    }
    
    private void preload(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (cache.containsKey(index)) return;
        if (loadingPages.contains(index)) return; // <--- YA SE ESTÁ DESCARGANDO, SALIR
        loadingPages.add(index); // <--- MARCAR COMO "EN PROCESO"
        preloader.submit(() -> {
            try {
                Image img = new Image(chapter.getPage(index),true);
                if (img.isBackgroundLoading()) {
                    while (img.getProgress() < 1.0 && !img.isError()) {
                        Thread.sleep(10);
                    }
                }
                if (!img.isError()) {
                    cache.put(index, img);
                    Logger.info("Preloaded page: " + index);
                }
            } catch (Exception e) {
                Logger.error("Error precargando página " + index);
            } finally {
                loadingPages.remove(index); // <--- LIBERAR CUANDO TERMINA (ÉXITO O ERROR)
            }
        });
    }
    
    private void preloadAroundCurrent() {
        preload(indiceactual + 1);
        preload(indiceactual + 2);
        preload(indiceactual + 3);
        preload(indiceactual - 1);
        preload(indiceactual - 2);
    }
    
    private void fitImageToScreen() {
        if (visor.getImage() == null || scrollVisor == null) return;
        double containerW = scrollVisor.getViewportBounds().getWidth() - 2;
        double containerH = scrollVisor.getViewportBounds().getHeight() - 2;
        if (containerW <= 0 || containerH <= 0) return;

        double imgW = visor.getImage().getWidth();
        double imgH = visor.getImage().getHeight();
        double ratioX = containerW / imgW;
        double ratioY = containerH / imgH;
        double scale = Math.min(ratioX, ratioY);
        visor.setFitWidth(imgW * scale);
        visor.setFitHeight(imgH * scale);
    }    
        
    private int totalPages() {
        return chapter.getPageCount();
    }
    
    
    /*
    Setters:
    */
    public void setChapter(Chapter chapter){
        this.chapter = chapter;
    }
    
    /*
    Getters:
    */
    
    
    
    @Override
    public String getName() {
        return manga.getTitle() + " - " + chapter.getTitle();
    }
    @Override
    public String getParentName() {
        return "Reader";
    }
    
}
