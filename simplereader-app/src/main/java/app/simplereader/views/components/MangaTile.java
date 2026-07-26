package app.simplereader.views.components;

import app.simplereader.service.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Manga;
import app.simplereader.views.ScnMangaMenu;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class MangaTile {

    private static final SceneController nav = SceneController.getInstance();
    private static final int COVER_MAX_SIZE = 400;
    private static final int COVER_LOAD_SIZE = 200;
    
    
    private final Manga manga;
    private final String title;
    private final String coverURL;
    
    private ImageView coverView;
    private Rectangle placeholder;
    
    private VBox tile;
    private Image cover;
    private Runnable updateImageFit;
    private boolean imageLoaded = false;
    private Image currentNativeLoad = null;
    private java.util.concurrent.CompletableFuture<?> currentIOLoad = null;
      
    

    public MangaTile(Manga manga){
        this.manga = manga;
        this.title = manga.getTitle();
        this.coverURL = manga.getCoverURL();
        this.tile = buildTile();
    }
    
    public void setCover(Image image){
        this.cover = image;
    }
    
    private VBox buildTile(){
        this.coverView = new ImageView();
        this.placeholder = new Rectangle();
        
        this.coverView.setPreserveRatio(true);
        this.coverView.setManaged(false);

        StackPane coverContainer = new StackPane();
        coverContainer.setMaxSize(COVER_MAX_SIZE, Double.MAX_VALUE);

        this.placeholder.setFill(Color.rgb(255, 255, 255, 0.1));
        this.placeholder.widthProperty().bind(coverContainer.widthProperty());
        this.placeholder.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.getChildren().addAll(placeholder, coverView);

        if (this.coverURL != null) {
            this.coverView.setOpacity(0.0);

            this.updateImageFit = () -> {
                Image img = this.coverView.getImage();
                if (img == null || img.isError()) return;

                double imgW = img.getWidth();
                double imgH = img.getHeight();
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

            Logger.info(this.title + " - " + this.coverURL + " --> Tile created.");
        }

        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);
        coverContainer.getStyleClass().add("menu-mangatile");

        coverContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            coverContainer.setPrefHeight(newVal.doubleValue() * 1.5);
        });

        Label title = new Label(this.title);

        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setAlignment(Pos.TOP_CENTER);

        title.setMinHeight(50);
        title.setMaxHeight(50);

        title.setMaxWidth(Double.MAX_VALUE);

        title.getStyleClass().add("manga-title");

        VBox iconManga = new VBox(5, coverContainer, title);

        iconManga.setFillWidth(true);

        title.prefWidthProperty().bind(iconManga.widthProperty());

        iconManga.setAlignment(Pos.TOP_CENTER);
        
        coverContainer.setMinSize(0, 0);
        iconManga.setMinSize(0, 0);

        iconManga.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                MainMenuController.getInstance().unloadAllCovers();
                ScnMangaMenu menu = ScnMangaMenu.getInstance();
                menu.updateManga(manga);
                nav.goTo(menu);
            }
        });

        iconManga.getStyleClass().add("manga-icon");
        
        return iconManga;
    }
    
    public VBox getTile() {
        return this.tile;
    }
    
    public void loadImage(){
        if(imageLoaded) return;
        
        app.simplereader.service.ImageManager.getInstance().loadCoverAsync(this.coverURL, false, finalImage -> {
            Platform.runLater(() -> {
                coverView.setImage(finalImage);
                coverView.setCache(true);
                coverView.setCacheHint(javafx.scene.CacheHint.SPEED);

                updateImageFit.run();
                this.imageLoaded = true;	
                coverView.setOpacity(1.0);
                placeholder.setVisible(false);
            });
        });
    }

    public void unloadImage() {
        this.coverView.setImage(null);      // libera la imagen del ImageView
        this.coverView.setOpacity(0.0);     // la deja invisible para el próximo loadImage()
        this.placeholder.setVisible(true);  // muestra el placeholder gris
        this.imageLoaded = false;           // permite que loadImage() vuelva a cargar
    }
}