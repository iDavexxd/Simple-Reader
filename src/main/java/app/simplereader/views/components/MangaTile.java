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

/**
 *
 * @author david
 */
public class MangaTile {
    
    private static final SceneController nav = SceneController.getInstance();
    
    public static VBox create(Manga manga){
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        // 1. Instanciamos el contenedor vacío
        StackPane coverContainer = new StackPane();
        coverContainer.setMaxSize(250, Double.MAX_VALUE);
        
        // 2. Creamos el Placeholder (rectángulo semitransparente)
        Rectangle placeholder = new Rectangle();
        placeholder.setFill(Color.rgb(255, 255, 255, 0.1)); 
        placeholder.widthProperty().bind(coverContainer.widthProperty());
        placeholder.heightProperty().bind(coverContainer.heightProperty());

        // Añadimos el placeholder al fondo y la imagen por encima
        coverContainer.getChildren().addAll(placeholder, coverView);

        if(manga.getCoverURL() != null){

            Image icon = new Image(manga.getCoverURL(), true);
            coverView.setImage(icon);

            // 3. Lógica de recorte dinámico
            Runnable updateImageFit = () -> {
                if (icon.getProgress() < 1.0 || icon.isError()) return;

                double imgW = icon.getWidth();
                double imgH = icon.getHeight();
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

            // Recalcular al cambiar el tamaño del contenedor
            coverContainer.widthProperty().addListener((obs, oldV, newV) -> updateImageFit.run());
            coverContainer.heightProperty().addListener((obs, oldV, newV) -> updateImageFit.run());

            // Recalcular al terminar de descargar la imagen
            icon.progressProperty().addListener((obs, old, progress) -> {
                if(progress.doubleValue() >= 1.0){
                    updateImageFit.run();
                    placeholder.setVisible(false); // Ocultamos el placeholder
                }
            });

            // 4. Centrado de la imagen
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

            Logger.info(manga.getTitle()+" - "+manga.getCoverURL()+" --> Loaded");
        }

        // 5. Clip redondeado para el contenedor (y el placeholder)
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);
        coverContainer.getStyleClass().add("menu-mangatile");
        // 6. Solución al crasheo: Mantener ratio 2:3 usando un listener sin bloquear la propiedad
        coverContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            coverContainer.setPrefHeight(newVal.doubleValue() * 1.5);
        });

        // 7. Título
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

        iconManga.setOnMouseClicked( e -> nav.goTo(new ScnMangaMenu(manga)));

        iconManga.getStyleClass().add("manga-icon");

        return iconManga;
    }
    
}
