package app.simplereader.views.components;

import app.simplereader.controller.Logger;
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

public class MangaTile {

    private static final SceneController nav = SceneController.getInstance();
    private static final int COVER_MAX_SIZE = 400;

    public static VBox create(Manga manga) {
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        StackPane coverContainer = new StackPane();
        coverContainer.setMaxSize(COVER_MAX_SIZE, Double.MAX_VALUE);

        Rectangle placeholder = new Rectangle();
        placeholder.setFill(Color.rgb(255, 255, 255, 0.1));
        placeholder.widthProperty().bind(coverContainer.widthProperty());
        placeholder.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.getChildren().addAll(placeholder, coverView);

        if (manga.getCoverURL() != null) {
            coverView.setOpacity(0.0);

            Runnable updateImageFit = () -> {
                Image img = coverView.getImage();
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

            loadScaledCoverAsync(coverView, manga.getCoverURL(), updateImageFit, placeholder);

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

            Logger.info(manga.getTitle() + " - " + manga.getCoverURL() + " --> Loaded");
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

        Label title = new Label(manga.getTitle());

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

        iconManga.setOnMouseClicked(e -> nav.goTo(new ScnMangaMenu(manga)));

        iconManga.getStyleClass().add("manga-icon");

        return iconManga;
    }

    private static void loadScaledCoverAsync(ImageView coverView, String url,
            Runnable updateImageFit, Rectangle placeholder) {

        // 1. Cargamos la imagen original de forma asíncrona para que no se rompa el WebP
        Image originalImage = new Image(url, true);

        // 2. Esperamos a que la imagen se descargue y decodifique
        originalImage.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() >= 1.0 && !originalImage.isError()) {
                Platform.runLater(() -> processAndSetImage(originalImage, coverView, updateImageFit, placeholder, true));
            }
        });

        // 3. Por si la imagen ya estaba descargada en la caché local
        if (originalImage.getProgress() >= 1.0) {
            Platform.runLater(() -> processAndSetImage(originalImage, coverView, updateImageFit, placeholder, false));
        }
    }

    private static void processAndSetImage(Image originalImage, ImageView coverView, Runnable updateImageFit, Rectangle placeholder, boolean animate) {
        Image finalImage = originalImage;

        // Si la imagen es más grande que nuestro límite, la achicamos tomando un "snapshot"
        if (originalImage.getWidth() > COVER_MAX_SIZE || originalImage.getHeight() > COVER_MAX_SIZE) {
            ImageView tempView = new ImageView(originalImage);
            tempView.setPreserveRatio(true);

            if (originalImage.getWidth() > originalImage.getHeight()) {
                tempView.setFitWidth(COVER_MAX_SIZE);
            } else {
                tempView.setFitHeight(COVER_MAX_SIZE);
            }

            // Tomamos una "foto" de la vista escalada con fondo transparente
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            finalImage = tempView.snapshot(params, null);
            
            // La variable "originalImage" dejará de usarse después de esto y el GC liberará la RAM
        }

        // Asignamos la miniatura a la vista final
        coverView.setImage(finalImage);
        coverView.setCache(true);
        coverView.setCacheHint(javafx.scene.CacheHint.SPEED);

        updateImageFit.run();

        if (animate) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), coverView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(e -> placeholder.setVisible(false));
            fadeIn.play();
        } else {
            coverView.setOpacity(1.0);
            placeholder.setVisible(false);
        }
    }
}