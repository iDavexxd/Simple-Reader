package app.simplereader.views;

import app.simplereader.controller.SceneController;
import app.simplereader.repository.AppScene;
import app.simplereader.service.Logger;
import app.simplereader.views.components.Buttons;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

public class ScnLoading implements AppScene {

    private static ScnLoading instance;
    private Parent myScene;
    private javafx.beans.value.ChangeListener<Boolean> fullScreenListener;
    private boolean aborted = false;

    private ScnLoading() {}

    public static ScnLoading getInstance() {
        if (instance == null) {
            instance = new ScnLoading();
        }
        return instance;
    }
    
    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }
    
    public boolean isAborted() {
        return aborted;
    }

    @Override
    public Parent getScene() {
        if (fullScreenListener == null) {
            fullScreenListener = (obs, oldVal, isFull) -> {
                if (myScene != null) {
                    if (isFull) {
                        myScene.getStyleClass().add("fullscreen");
                    } else {
                        myScene.getStyleClass().remove("fullscreen");
                    }
                }
            };
        }
        SceneController.getInstance().getStage().fullScreenProperty().removeListener(fullScreenListener);
        SceneController.getInstance().getStage().fullScreenProperty().addListener(fullScreenListener);

        if (myScene != null) {
            if (SceneController.getInstance().getStage().isFullScreen() && !myScene.getStyleClass().contains("fullscreen")) {
                myScene.getStyleClass().add("fullscreen");
            } else if (!SceneController.getInstance().getStage().isFullScreen()) {
                myScene.getStyleClass().remove("fullscreen");
            }
            return myScene;
        }

        Image loadingGif = null;
        try {
            loadingGif = new Image(getClass().getResource("/icons/koruko.gif").toExternalForm());
        } catch (Exception e) {
            Logger.error("No se pudo cargar el gif de carga: " + e.getMessage());
        }

        ImageView gifView = new ImageView(loadingGif);
        gifView.setFitWidth(150);
        gifView.setFitHeight(150);
        gifView.setPreserveRatio(true);

        Label loadingLabel = new Label("Cargando...");
        loadingLabel.getStyleClass().add("reader-loading-label");

        VBox loadingOverlay = new VBox(15, gifView, loadingLabel);
        loadingOverlay.setAlignment(Pos.CENTER);

        BorderPane layout = new BorderPane();
        layout.setCenter(loadingOverlay);

        Button btnBack = Buttons.getBackButton();
        btnBack.addEventHandler(javafx.event.ActionEvent.ACTION, e -> {
            aborted = true;
            SceneController.getInstance().getStage().fullScreenProperty().removeListener(fullScreenListener);
        });
        
        StackPane backContainer = new StackPane(btnBack);
        backContainer.setAlignment(Pos.TOP_LEFT);
        backContainer.setStyle("-fx-padding: 15;");

        StackPane root = new StackPane(layout, backContainer);
        root.getStyleClass().add("reader");
        
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                aborted = true;
                SceneController.getInstance().getStage().fullScreenProperty().removeListener(fullScreenListener);
                SceneController.getInstance().backScene();
            }
        });
        
        myScene = root;

        if (SceneController.getInstance().getStage().isFullScreen()) {
            myScene.getStyleClass().add("fullscreen");
        }

        return myScene;
    }

    @Override
    public String getName() {
        return "Loading";
    }

    @Override
    public String getParentName() {
        return "MangaMenu"; 
    }
}
