package app.simplereader.views;

import app.simplereader.controller.ConfigSceneController;
import app.simplereader.controller.MainMenuController;
import app.simplereader.views.components.SideMenu;
import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Category;
import app.simplereader.repository.AppScene;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author david
 */
public class ScnConfig implements AppScene {
    
    private final SceneController nav;
    private final ConfigSceneController controller;
    
    public ScnConfig(){
        ConfigSceneController.doInstance(this);
        this.controller = ConfigSceneController.getInstance();
        this.nav = SceneController.getInstance();
    }
    
//    private void saveConfig(){
//        // Sincronizar UI -> Objeto Config
//            AppConfig.get().READING_DIR = cbDirection.getValue().contains("LTR") ? "LTR" : "RTL";
//            AppConfig.get().SCALING_MODE = cbScaling.getValue().contains("ancho") ? "FIT_WIDTH" : "FIT_HEIGHT";
//            
//            AppConfig.get().save(); // Guardar a JSON
//            
//    }
    
    @Override
    public Scene getScene() {
        
        // Menu lateral
        SideMenu lateralmenu = new SideMenu();        
        
        // botones
        SVGPath svgBack = new SVGPath();
        svgBack.setContent("M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z");
        svgBack.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        svgBack.setScaleX(scale);
        svgBack.setScaleY(scale);
        
        Group gpBack = new Group(svgBack);
        StackPane back_container = new StackPane(gpBack);
        back_container.setPrefSize(24, 24);
        back_container.setMinSize(24, 24);

        Button btnBackToMenu = new Button("",back_container);
        btnBackToMenu.setOnAction(e ->{
            nav.backScene();
        });
        btnBackToMenu.setPrefSize(24, 24);
        btnBackToMenu.setMinSize(24, 24);
        
        lateralmenu.addTop(btnBackToMenu);
        
        // Título de Categorías
        Label categoryLabel = new Label("Categories");
        categoryLabel.setMaxWidth(Double.MAX_VALUE); // Corregido: MaxWidth en lugar de MinWidth
        categoryLabel.setAlignment(Pos.TOP_CENTER);
        
        // Instancia del ListView
        ListView<Category> categoryListView = new ListView<>();
        
        // 1. Obtener la lista base del controlador y crear la ObservableList
        List<Category> categoriasObtenidas = controller.getCategoryList();
        ObservableList<Category> datosObservables = FXCollections.observableArrayList();
        
        if (categoriasObtenidas != null) {
            datosObservables.addAll(categoriasObtenidas);
        }
        categoryListView.setItems(datosObservables); // Asignar la lista al ListView
        
        // 2. CellFactory para mostrar el getName() de cada Categoría
        categoryListView.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getName() == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        // Buttons
        Button btnAdd = new Button("Add");
        btnAdd.setMinSize(50, 50);
        btnAdd.setMaxSize(50, 50);
        
        Button btnHide = new Button("Hide");
        btnHide.setMinSize(50, 50);
        btnHide.setMaxSize(50, 50);
        
        Button btnRemove = new Button("Remove");
        btnRemove.setMinSize(50, 50);
        btnRemove.setMaxSize(50, 50);
        
        // === ACCIONES ===
        
        // Acción del botón Añadir (Abre ventana emergente)
        btnAdd.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Category");
            dialog.setHeaderText("Create a new category");
            dialog.setContentText("Category name:");

            Optional<String> result = dialog.showAndWait();
            
            result.ifPresent(name -> {
                if (!name.trim().isEmpty() && !name.trim().equalsIgnoreCase("default")) {
                    controller.doCreateCategory(name.trim());
                    // Refrescar la lista de la interfaz
                    datosObservables.setAll(controller.getCategoryList());
                }
            });
        });
        
        // Acción del botón Eliminar (Borra lo seleccionado)
        btnRemove.setOnAction(e -> {
            Category selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
            
            if (selectedCategory != null) {
                if(!selectedCategory.getName().equalsIgnoreCase("default")){
                    controller.doRemoveCategory(selectedCategory.getName());
                    datosObservables.setAll(controller.getCategoryList());
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No selection");
                alert.setHeaderText(null);
                alert.setContentText("Please select a category from the list to remove.");
                alert.showAndWait();
            }
        });
        
        btnHide.setOnAction(e->{ 
            Category selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
            if (selectedCategory != null) {
                if(!selectedCategory.getName().equalsIgnoreCase("default")){
                    if(selectedCategory.isHide()){
                        selectedCategory.setHide(false);
                    }else{
                        selectedCategory.setHide(true);
                    }
                    MainMenuController.getInstance().reloadCategoryTabs();

                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No selection");
                alert.setHeaderText(null);
                alert.setContentText("Please select a category from the list to hide.");
                alert.showAndWait();
            }
        });
        
        // Coso de configuraciones
        BorderPane coso = new BorderPane();
        coso.setMaxWidth(300);
        coso.setMinWidth(300);
        coso.setPadding(new Insets(15));
        
        Button btnCategories = new Button("prueba");
        btnCategories.setMaxWidth(Double.MAX_VALUE);
        VBox botones = new VBox(5,btnCategories);
        
        coso.setCenter(botones);
        
        // Disposición visual de los botones de la lista
        Region buttonSpacer = new Region();
        VBox.setVgrow(buttonSpacer, Priority.ALWAYS);
        
        VBox categoryButtons = new VBox(5, btnAdd, buttonSpacer, btnHide,btnRemove);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Este Hgrow funciona correctamente ahora gracias al fitToWidth del ScrollPane
        HBox listAndButtons = new HBox(categoryListView, spacer, categoryButtons);
        
        VBox categoryConfig = new VBox(categoryLabel, listAndButtons);
        VBox.setVgrow(categoryConfig, Priority.ALWAYS);
        VBox.setVgrow(categoryListView, Priority.ALWAYS);
        
        VBox allConfig = new VBox(categoryConfig);
        ScrollPane Scroll = new ScrollPane();
        Scroll.setContent(allConfig);
        
        Scroll.setFitToWidth(true); 
        BorderPane content = new BorderPane();
        content.setCenter(Scroll);
        content.setLeft(coso);
        
        BorderPane root = new BorderPane();
        root.setCenter(content);
        root.setLeft(lateralmenu.getPane());
        root.getStyleClass().add("conf-root");
        
        Scene scene = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        return scene;
        
    }

    @Override
    public String getName() {
        return "Configuration";
    }

    @Override
    public String getParentName() {
        return "Configuration";  
    }
    
}