package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.chapter.LocalChapter;
import app.simplereader.manga.ChapterType;
import app.simplereader.manga.LocalManga;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class ScnReader implements Navigable {

    private final Navegador nav;
    private Chapter chapter;
    private int chapternum;
    private final LocalManga manga;

    private Label lblPage;
    private ImageView visor;
    private ScrollPane scrollVisor;
    private int indiceactual = 0;
    private BorderPane layout;
    
    private final Map<Integer, Image> cache = java.util.Collections.synchronizedMap(new HashMap<>());
    private final ExecutorService preloader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "preloader");
        t.setDaemon(true); // Se cierra solo cuando cierra la app
        return t;
    });
    
    public ScnReader(Navegador nav, LocalManga manga, Chapter chapter, int indice) {
        this.nav = nav;
        this.chapter = chapter;
        this.chapternum = indice;
        this.manga = manga;
        Logger.info("Loaded " + chapter.getName() + " " + chapternum);
    }

    @Override
    public Scene getScene() {
        // Scene del lector, con sus cosas.
        if(layout == null) layout = getPane();
        
        
        nav.getStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);  
        Scene scene = new Scene(layout,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        // Listener de las teclas
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
        KeyCode key = e.getCode();

            switch(key){
                case F11 -> {
                    boolean isFull = nav.getStage().isFullScreen();
                    nav.getStage().setFullScreen(!isFull);
                }
                case ESCAPE -> {
                    cache.clear();
                    preloader.shutdownNow();
                    nav.goTo(new ScnMangaMenu(nav,manga));
                }
                case RIGHT -> {
                    if(e.isShiftDown()){
                        if (this.chapternum < this.manga.getChapters().size() - 1) {
                            Chapter next = this.manga.getChapters().get(this.chapternum + 1);
                            if(next.hasPages()){
                                loadChapter(next, chapternum + 1);
                            }else{
                                Logger.noPagesAlert(next);
                            }
                        }
                    }else {
                        if (indiceactual < totalPages() - 1) {
                            indiceactual++;
                            LoadImage();
                            relbl();
                        }    
                    }
                    
                    e.consume();
                }
                case LEFT -> {
                    if(e.isShiftDown())
                    {
                       if (this.chapternum > 0) {
                            Chapter last = this.manga.getChapters().get(this.chapternum - 1);
                            if(last.hasPages()){
                                loadChapter(last, chapternum - 1);
                            }else{
                                Logger.noPagesAlert(last);
                            }

                        }   
                    }
                    else
                    {
                        if (indiceactual > 0) {
                        indiceactual--;
                        LoadImage();
                        relbl();
                        }
                        
                    }
                 e.consume();    
                }

            }
        });
        
        
        return scene;
    }
    
    private void loadChapter(Chapter chapter, int index){
        this.chapter = chapter;
        this.chapternum = index;
        this.indiceactual = 0;
        
        this.lblPage.setText("0/0");
        
        if (totalPages() > 0) {
            LoadImage();
            relbl();
        }
        cache.clear();
        nav.getStage().setTitle(this.getName());
    }
    
    private BorderPane getPane(){
        this.lblPage = new Label("0/0");
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

                // Ajustar scroll para que el zoom sea hacia el cursor (no funciona)
                double viewW = scrollVisor.getViewportBounds().getWidth();
                double viewH = scrollVisor.getViewportBounds().getHeight();

                double newScrollH = (scrollH * (contentW - viewW) + mouseX * (zoomFactor - 1))
                                    / (newContentW - viewW);
                double newScrollV = (scrollV * (contentH - viewH) + mouseY * (zoomFactor - 1))
                                    / (newContentH - viewH);

                scrollVisor.setHvalue(Math.max(0, Math.min(1, newScrollH)));
                scrollVisor.setVvalue(Math.max(0, Math.min(1, newScrollV)));

                event.consume();
            }
        });
                
        Button btnNext = new Button("->");
        Button btnBack = new Button("<-");
        Button btnNextCh = new Button("Next");
        Button btnBackCh = new Button("Back");
        Button btnReload = new Button("Reload");
        Button btnBackToMenu = new Button("Menu");

        btnNext.setOnAction(e -> {
            if (indiceactual < totalPages() - 1) {
                indiceactual++;
                LoadImage();
                relbl();
            }
        });
        btnBack.setOnAction(e -> {
            if (indiceactual > 0) {
                indiceactual--;
                LoadImage();
                relbl();
            }
        });
        btnReload.setOnAction(e -> {
            LoadImage();
            relbl();
        });
        btnBackToMenu.setOnAction(e -> {
            cache.clear();
            preloader.shutdownNow();
            nav.goTo(new ScnMangaMenu(nav, manga));
        });

        btnNextCh.setOnAction(e -> {
            if (this.chapternum < this.manga.getChapters().size() - 1) {
                Chapter next = this.manga.getChapters().get(this.chapternum + 1);
                if(next.hasPages()){
                    loadChapter(next, chapternum + 1);
                }else{
                    Logger.noPagesAlert(next);
                }
            }
        });

        btnBackCh.setOnAction(e -> {
            if (this.chapternum > 0) {
                Chapter last = this.manga.getChapters().get(this.chapternum - 1);
                if(last.hasPages()){
                    loadChapter(last, chapternum - 1);
                }else{
                    Logger.noPagesAlert(last);
                }
                
            }
        });

        VBox leftpanel = new VBox(10, btnBack, btnBackCh);
        VBox rightpanel = new VBox(10, btnNext, btnNextCh);
        HBox toppanel = new HBox(20, btnReload, lblPage, btnBackToMenu);
        leftpanel.setAlignment(Pos.CENTER);
        rightpanel.setAlignment(Pos.CENTER);
        toppanel.setAlignment(Pos.CENTER);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollVisor);
        layout.setLeft(leftpanel);
        layout.setRight(rightpanel);
        layout.setTop(toppanel);

        //esto cambia el tamaño de la imagen si se cambia el tamaño de la ventana
        scrollVisor.widthProperty().addListener((obs, oldVal, newVal) -> fitImageToScreen());
        scrollVisor.heightProperty().addListener((obs, oldVal, newVal) -> fitImageToScreen());

        scrollVisor.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.getWidth() > 0 && newVal.getHeight() > 0) {
                fitImageToScreen();
            }
        });

        if (chapter.hasPages()) {
            LoadImage();
            relbl();
        }
        
        layout.getStyleClass().add("reader");
        return layout;
    }

    public void resetZoom() {
        if (visor != null) {
            visor.setScaleX(1.0);
            visor.setScaleY(1.0);
        }
    }
    

    public void LoadImage() {
        try {
            visor.setImage(null);
            Image img = getPage(indiceactual);

            // Si todavía está cargando (vino de preload), esperar con listener
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
    
    private Image getPage(int index){
        if(cache.containsKey(index)) return cache.get(index);
        
        Image img = chapter.getPage(index);
        cache.put(index, img);
        return img;
    }
    
    private void cleanCache(){
        int range = 1;
        
        System.gc();
        cache.keySet().removeIf(i -> Math.abs(i - indiceactual) > range);
    }
    
    private void preload(int index) {
        if (index < 0 || index >= totalPages()) return;
        if (cache.containsKey(index)) return;

        preloader.submit(() -> {
            try {
                Image img = chapter.getPage(index);
                // Esperar a que la imagen termine de cargar
                if (img.isBackgroundLoading()) {
                    // Bloquear el hilo del preloader hasta que cargue
                    while (img.getProgress() < 1.0 && !img.isError()) {
                        Thread.sleep(10);
                    }
                }
                if (!img.isError()) {
                    cache.put(index, img);
                    Logger.info("Preloaded page: " + index);
                }
            } catch (Exception e) {
                Logger.error("Error precargando página " + index + ": " + e.getMessage());
            }
        });
    }
    
    private void preloadAroundCurrent() {
        preload(indiceactual + 1);
        preload(indiceactual - 1);
    }
    
    private void fitImageToScreen() {
        if (visor.getImage() == null || scrollVisor == null) return;

        // Obtenemos el ancho y alto real del área visible (Viewport)
        // Restamos 2 píxeles de margen de seguridad para evitar scrolls accidentales
        double containerW = scrollVisor.getViewportBounds().getWidth() - 2;
        double containerH = scrollVisor.getViewportBounds().getHeight() - 2;

        if (containerW <= 0 || containerH <= 0) return;

        double imgW = visor.getImage().getWidth();
        double imgH = visor.getImage().getHeight();

        double ratioX = containerW / imgW;
        double ratioY = containerH / imgH;

        // El truco: elegimos el ratio más pequeño para que quepa en AMBOS ejes
        double scale = Math.min(ratioX, ratioY);

        // Aplicamos el tamaño ajustado
        visor.setFitWidth(imgW * scale);
        visor.setFitHeight(imgH * scale);
    }    
    

    private void relbl() {
        if (lblPage != null) {
            lblPage.setText((indiceactual + 1) + " / " + chapter.getPageCount());
        }
    }
    
    private int totalPages() {
        return chapter.getPageCount();
    }

    
    

    

    @Override
    public String getName() {
        return manga.getTitle() +" - "+ chapter.getName();
    }
    @Override
    public String getParentName(){
        return "Reader";
    }
    
}