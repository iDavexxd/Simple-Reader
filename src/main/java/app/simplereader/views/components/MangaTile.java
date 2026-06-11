package app.simplereader.views.components;

import app.simplereader.controller.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Manga;
import app.simplereader.views.ScnMangaMenu;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
    
    
    private final Manga manga;
    private final String title;
    private final String coverURL;
    
    private ImageView coverView;
    private Rectangle placeholder;
    
    private VBox tile;
    private Image cover;
    private Runnable updateImageFit;
    private boolean imageLoaded = false;
      
    
    private static final Cache<String, Image> IMAGE_CACHE = Caffeine.newBuilder()
        .maximumSize(50)
        .build();
    
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

        iconManga.setOnMouseClicked(e -> {
            MainMenuController.getInstance().unloadAllCovers();
            ScnMangaMenu menu = ScnMangaMenu.getInstance();
            menu.updateManga(manga);
            nav.goTo(menu);
        });

        iconManga.getStyleClass().add("manga-icon");
        
        return iconManga;
    }
    
    public VBox getTile() {
        return this.tile;
    }
    
    public void loadImage(){
        if(imageLoaded) return;
        
        Image cached = IMAGE_CACHE.getIfPresent(this.coverURL);
        if(cached != null){
            this.coverView.setImage(cached);
            this.coverView.setCache(true);
            this.coverView.setCacheHint(javafx.scene.CacheHint.SPEED);
            this.updateImageFit.run();
            this.coverView.setOpacity(1.0);
            this.placeholder.setVisible(false);
            this.imageLoaded = true;
            return;
        }
        loadScaledCoverAsync();
    }

    public void unloadImage() {
        this.coverView.setImage(null);      // libera la imagen del ImageView
        this.coverView.setOpacity(0.0);     // la deja invisible para el próximo loadImage()
        this.placeholder.setVisible(true);  // muestra el placeholder gris
        this.imageLoaded = false;           // permite que loadImage() vuelva a cargar
    }
    
    private void loadScaledCoverAsync() {
        if (this.coverURL != null && this.coverURL.toLowerCase().contains(".webp")) {
            loadWithImageIO();
        } else {
            // JavaFX nativo: escalado en carga, no bloquea, bajo consumo de RAM
            Image originalImage = new Image(this.coverURL, COVER_MAX_SIZE, COVER_MAX_SIZE, true, true, true);

            if (originalImage.isError()) {
                loadWithImageIO();
            } else if (originalImage.getProgress() >= 1.0) {
                Platform.runLater(() -> processAndSetImage(originalImage, false));
            } else {
                ChangeListener<Number> progressListener = new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> obs, Number old, Number progress) {
                        if (progress.doubleValue() >= 1.0) {
                            originalImage.progressProperty().removeListener(this);
                            if (!originalImage.isError()) {
                                Platform.runLater(() -> processAndSetImage(originalImage, true));
                            }
                        }
                    }
                };
                
                ChangeListener<Boolean> errorListener = new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> obs, Boolean old, Boolean isError) {
                        if (isError) {
                            originalImage.errorProperty().removeListener(this);
                            loadWithImageIO();
                        }
                    }
                };

                originalImage.progressProperty().addListener(progressListener);
                originalImage.errorProperty().addListener(errorListener);
            }
        }
    }

    private void loadWithImageIO() {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(this.coverURL);
                java.net.URLConnection conn = urlObj.openConnection();
                if (this.coverURL.startsWith("http")) {
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                }
                java.awt.image.BufferedImage bimg;
                try (java.io.InputStream in = conn.getInputStream()) {
                    bimg = javax.imageio.ImageIO.read(in);
                }
                if (bimg != null) {
                    if (bimg.getWidth() > COVER_MAX_SIZE || bimg.getHeight() > COVER_MAX_SIZE) {
                        int newWidth = bimg.getWidth();
                        int newHeight = bimg.getHeight();
                        if (newWidth > newHeight) {
                            newHeight = (int) (newHeight * ((double) COVER_MAX_SIZE / newWidth));
                            newWidth = COVER_MAX_SIZE;
                        } else {
                            newWidth = (int) (newWidth * ((double) COVER_MAX_SIZE / newHeight));
                            newHeight = COVER_MAX_SIZE;
                        }
                        
                        java.awt.Image tmp = bimg.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                        java.awt.image.BufferedImage scaledBimg = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = scaledBimg.createGraphics();
                        g2d.drawImage(tmp, 0, 0, null);
                        g2d.dispose();
                        bimg = scaledBimg;
                    }
                    return javafx.embed.swing.SwingFXUtils.toFXImage(bimg, null);
                }
            } catch (Exception e) {
                Logger.error("Error decodificando cover con ImageIO: " + e.getMessage());
            }
            return null;
        }).thenAccept(originalImage -> {
            if (originalImage != null) {
                Platform.runLater(() -> processAndSetImage(originalImage, true));
            }
        });
    }

    private void processAndSetImage(Image originalImage,boolean animate) {
        Image finalImage = originalImage;

        if (originalImage.getWidth() > COVER_MAX_SIZE || originalImage.getHeight() > COVER_MAX_SIZE) {
            ImageView tempView = new ImageView(originalImage);
            tempView.setPreserveRatio(true);

            if (originalImage.getWidth() > originalImage.getHeight()) {
                tempView.setFitWidth(COVER_MAX_SIZE);
            } else {
                tempView.setFitHeight(COVER_MAX_SIZE);
            }

            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            finalImage = tempView.snapshot(params, null);
        }

        coverView.setImage(finalImage);
        coverView.setCache(true);
        coverView.setCacheHint(javafx.scene.CacheHint.SPEED);

        updateImageFit.run();
        IMAGE_CACHE.put(this.coverURL, finalImage);   // guarda en la caché
        this.imageLoaded = true;	
        coverView.setOpacity(1.0);
        placeholder.setVisible(false);
    }
}