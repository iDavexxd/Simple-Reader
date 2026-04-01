package app.simplereader.scenes;

import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Chapter;
import app.simplereader.manga.Manga;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final Chapter chapter;
    private final int chapternum;
    private final Manga manga;

    private Label lblPage;
    private ImageView visor;
    private ScrollPane scrollVisor;
    private List<File> imagenes;
    private int indiceactual = 0;

    public ScnReader(Navegador nav, Manga manga, Chapter chapter, int indice) {
        this.nav = nav;
        this.chapter = chapter;
        this.chapternum = indice;
        this.manga = manga;
        Logger.info("Loaded " + chapter.getChName() + " " + chapternum);
    }

    @Override
    public Parent getParent() {
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
            if (indiceactual < imagenes.size() - 1) {
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
            imagenes = chapter.reloadPages();
            LoadImage();
            relbl();
        });
        btnBackToMenu.setOnAction(e -> {
            clearImages();
            nav.goTo(new ScnMangaMenu(nav, manga));
        });

        btnNextCh.setOnAction(e -> {
            if (this.chapternum < this.manga.getChapters().size() - 1) {
                Chapter next = this.manga.getChapters().get(this.chapternum + 1);
                if(!next.getPages().isEmpty()){
                    nav.goTo(new ScnReader(nav, this.manga, next, chapternum + 1));
                }else{
                    Logger.noPagesAlert(next);
                }
            }
        });

        btnBackCh.setOnAction(e -> {
            if (this.chapternum > 0) {
                Chapter last = this.manga.getChapters().get(this.chapternum - 1);
                if(!last.getPages().isEmpty()){
                    nav.goTo(new ScnReader(nav, this.manga, last, chapternum - 1));
                }else{
                    Logger.noPagesAlert(last);
                }
                
            }
        });
        
        btnBackCh.setDisable(chapternum == 0);
        btnNextCh.setDisable(chapternum >= manga.getChapters().size() - 1);

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

        imagenes = loadImages();
        if (!imagenes.isEmpty()) {
            LoadImage();
            relbl();
        }

        return layout;
    }

    public void resetZoom() {
        if (visor != null) {
            visor.setScaleX(1.0);
            visor.setScaleY(1.0);
        }
    }

    public void LoadImage() {
        File archivo = imagenes.get(indiceactual);
        Image img = new Image(archivo.toURI().toString());
        visor.setImage(img);
        resetZoom();
        
        // Usamos Platform.runLater para asegurar que el ScrollPane ya sabe su tamaño
        Platform.runLater(this::fitImageToScreen);
        Logger.info(archivo.getName() + " - Loaded.");
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
        if (lblPage != null && imagenes != null) {
            lblPage.setText((indiceactual + 1) + " / " + imagenes.size());
        }
    }

    public List<File> loadImages() {
        return chapter.getPages();
    }
    
    private void clearImages(){
        if(imagenes != null) imagenes.clear();
        if(visor != null) visor = null;
    }

    @Override
    public String getName() {
        return manga.getTitle() +" - "+ chapter.getChName();
    }
    
}
