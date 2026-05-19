package app.simplereader.views;

import app.simplereader.controller.Logger;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceMenuController;
import app.simplereader.model.AppConfig;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.components.Buttons;
import app.simplereader.views.components.SideMenu;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;

/**
 *
 * @author david
 */
public class ScnSourceMenu implements AppScene {

    private final SceneController nav = SceneController.getInstance();
    private SourceMenuController controller;
    
    private ListView<MangaSource> sourceListView;
    
    public ScnSourceMenu(){
        SourceMenuController.doInstance(this);
        this.controller = SourceMenuController.getInstance();
    }
    
    @Override
    public Scene getScene() {
        SideMenu leftMenu = new SideMenu();
        
        //botones
        Button btnBack = Buttons.getBackButton();
        SVGPath icnImportar = new SVGPath();
        icnImportar.setContent("M440-320v-326L336-542l-56-58 200-200 200 200-56 58-104-104v326h-80ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z");
        icnImportar.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        icnImportar.setScaleX(scale);
        icnImportar.setScaleY(scale);
        
        Group icon_importar_group = new Group(icnImportar);
        StackPane icon_container_importar = new StackPane(icon_importar_group);
        
        Button btnImportar = new Button("",icon_container_importar);
        btnImportar.setMinSize(24, 24);
        btnImportar.setMaxSize(24, 24);
        
        btnImportar.setOnAction(event -> {
            Window currentWindow = btnImportar.getScene().getWindow();
            controller.importPlugin(currentWindow);
        });
        
        leftMenu.addTop(btnBack);
        leftMenu.addBottom(btnImportar);
        
        //Lista
        sourceListView = new ListView<>();
        sourceListView.getStyleClass().add("source-list");
        List<MangaSource> sources = controller.getAllSources();
        sourceListView.setCellFactory(param -> new ListCell<MangaSource>() {
            @Override
            protected void updateItem(MangaSource item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        // Escuchar la selección del ListView
        sourceListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                nav.goTo(new ScnSourceSearch(newValue));
            }
        });
        
        
        if (sources != null) {
            ObservableList<MangaSource> observableSources = FXCollections.observableArrayList(sources);
            sourceListView.setItems(observableSources);
        }
        
        Label title = new Label("Extensions");
        title.getStyleClass().add("source-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        VBox vboxconto = new VBox(title,sourceListView);
        VBox.setVgrow(sourceListView, Priority.ALWAYS);
        BorderPane root = new BorderPane();
        root.setLeft(leftMenu.getPane());
        root.setCenter(vboxconto);
        
        Scene scene = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        return scene;
    }

    public ListView<MangaSource> getListView(){
        return this.sourceListView;
    }
    
    @Override
    public String getName() {
        return "Sources";
    }

    @Override
    public String getParentName() {
        return "Sources";
    }
    
}
