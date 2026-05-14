/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.simplereader.views;

import app.simplereader.controller.Logger;
import app.simplereader.controller.SceneController;
import app.simplereader.repository.Manga;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/**
 *
 * @author david
 */
public class MangaTile {
    
    public static VBox create(Manga manga, SceneController nav){
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        StackPane coverContainer = new StackPane(coverView);
        coverContainer.setMaxSize(250, Double.MAX_VALUE);

        if(manga.getCover() != null){

            Image icon = new Image(manga.getCover(), true);
            coverView.setImage(icon);

            icon.progressProperty().addListener((obs, old, progress) -> {

                if(progress.doubleValue() >= 1.0){

                    double imageRatio = icon.getWidth() / icon.getHeight();

                    // ratio del contenedor (2:3)
                    double containerRatio = 2.0 / 3.0;

                    // Limpiar bindings anteriores
                    coverView.fitWidthProperty().unbind();
                    coverView.fitHeightProperty().unbind();

                    if(imageRatio < containerRatio){

                        // Imagen muy angosta
                        // llenar ancho y recortar arriba/abajo
                        coverView.fitWidthProperty().bind(
                            coverContainer.widthProperty()
                        );

                    } else {

                        // Imagen normal o ancha
                        // llenar alto y recortar lados
                        coverView.fitHeightProperty().bind(
                            coverContainer.heightProperty()
                        );
                    }

                    // Centrar imagen
                    coverView.layoutXProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> (
                                coverContainer.getWidth()
                                - coverView.getBoundsInLocal().getWidth()
                            ) / 2.0,
                            coverContainer.widthProperty(),
                            coverView.boundsInLocalProperty()
                        )
                    );

                    coverView.layoutYProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> (
                                coverContainer.getHeight()
                                - coverView.getBoundsInLocal().getHeight()
                            ) / 2.0,
                            coverContainer.heightProperty(),
                            coverView.boundsInLocalProperty()
                        )
                    );
                }
            });

            Logger.info(manga.getTitle()+" - "+manga.getCover()+" --> Loaded");
        }

        // Clip redondeado
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);

        // Título
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

        iconManga.setAlignment(Pos.TOP_LEFT);

//        iconManga.setOnMouseClicked(
//            e -> nav.goTo(new ScnMangaMenu(nav, manga))
//        );

        iconManga.getStyleClass().add("manga-icon");

        return iconManga;
    }
    
}
