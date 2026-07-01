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
        
        Image cached = app.simplereader.service.Cache.getInstance().getSharedCache().getIfPresent(this.coverURL);
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
        if (currentNativeLoad != null) {
            currentNativeLoad.cancel();
            currentNativeLoad = null;
        }
        if (currentIOLoad != null) {
            currentIOLoad.cancel(true);
            currentIOLoad = null;
        }
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
            currentNativeLoad = new Image(this.coverURL, COVER_LOAD_SIZE, COVER_LOAD_SIZE, true, true, true);
            Image originalImage = currentNativeLoad;

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
        currentIOLoad = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                app.simplereader.repository.MangaSource src = this.manga != null ? app.simplereader.controller.SourceManager.getInstance().getSource(this.manga.getSourceID()) : null;
                java.awt.image.BufferedImage bimg = null;
                try (java.io.InputStream in = app.simplereader.service.Http.getInputStreamWithRetry(this.coverURL, src)) {
                    javax.imageio.stream.ImageInputStream iis = javax.imageio.ImageIO.createImageInputStream(in);
                    if (iis != null) {
                        java.util.Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(iis);
                        if (readers.hasNext()) {
                            javax.imageio.ImageReader reader = readers.next();
                            reader.setInput(iis, true, true);
                            int w = reader.getWidth(0);
                            int h = reader.getHeight(0);
                            javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                            
                            if (w > COVER_LOAD_SIZE || h > COVER_LOAD_SIZE) {
                                int scale = Math.max(w / COVER_LOAD_SIZE, h / COVER_LOAD_SIZE);
                                if (scale < 1) scale = 1;
                                param.setSourceSubsampling(scale, scale, 0, 0);
                            }
                            bimg = reader.read(0, param);
                            reader.dispose();
                        }
                        iis.close();
                    }
                }
                if (bimg != null) {
                    if (bimg.getWidth() > COVER_LOAD_SIZE || bimg.getHeight() > COVER_LOAD_SIZE) {
                        int newWidth = bimg.getWidth();
                        int newHeight = bimg.getHeight();
                        if (newWidth > newHeight) {
                            newHeight = (int) (newHeight * ((double) COVER_LOAD_SIZE / newWidth));
                            newWidth = COVER_LOAD_SIZE;
                        } else {
                            newWidth = (int) (newWidth * ((double) COVER_LOAD_SIZE / newHeight));
                            newHeight = COVER_LOAD_SIZE;
                        }
                        
                        java.awt.image.BufferedImage scaledBimg = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = scaledBimg.createGraphics();
                        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                        g2d.dispose();
                        
                        bimg.flush(); // Liberar memoria nativa de la imagen original gigante
                        bimg = scaledBimg;
                    }
                    Image fxImg = javafx.embed.swing.SwingFXUtils.toFXImage(bimg, null);
                    bimg.flush(); // Liberar BufferedImage ahora que ya es un Image de JavaFX
                    return fxImg;
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

        if (originalImage.getWidth() > COVER_LOAD_SIZE || originalImage.getHeight() > COVER_LOAD_SIZE) {
            ImageView tempView = new ImageView(originalImage);
            tempView.setPreserveRatio(true);

            if (originalImage.getWidth() > originalImage.getHeight()) {
                tempView.setFitWidth(COVER_LOAD_SIZE);
            } else {
                tempView.setFitHeight(COVER_LOAD_SIZE);
            }

            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            finalImage = tempView.snapshot(params, null);
        }

        coverView.setImage(finalImage);
        coverView.setCache(true);
        coverView.setCacheHint(javafx.scene.CacheHint.SPEED);

        updateImageFit.run();
        app.simplereader.service.Cache.getInstance().getSharedCache().put(this.coverURL, finalImage);   // guarda en la caché
        this.imageLoaded = true;	
        coverView.setOpacity(1.0);
        placeholder.setVisible(false);
    }
}