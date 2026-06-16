package app.simplereader.views;

import app.simplereader.controller.ConfigSceneController;
import app.simplereader.controller.MainMenuController;
import app.simplereader.views.components.SideMenu;
import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Category;
import app.simplereader.repository.AppScene;
import app.simplereader.views.components.SvgIcons;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
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
    private final SvgIcons icons = SvgIcons.get();
    public ScnConfig(){
        ConfigSceneController.doInstance(this);
        this.controller = ConfigSceneController.getInstance();
        this.nav = SceneController.getInstance();
    }
    
    
    @Override
    public Parent getScene() {
        
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
        
        // El ScrollPane central que cambiará de contenido
        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setFitToWidth(true); 
        
        // Los paneles pre-construidos
        VBox panelCategories = getCategoriesPanel();
        VBox panelGeneral = getGeneralPanel();
        
        // Por defecto mostramos General
        scrollContent.setContent(panelGeneral);
        
        // Coso de configuraciones (Navegación lateral)
        BorderPane coso = new BorderPane();
        coso.getStyleClass().add("config-menu");
        coso.setMaxWidth(300);
        coso.setMinWidth(300);
        coso.setPadding(new Insets(15));
        
        Button btnGeneral = new Button("Lector");
        btnGeneral.getStyleClass().add("config-menu-btn");
        btnGeneral.setMaxWidth(Double.MAX_VALUE);
        btnGeneral.setOnAction(e -> scrollContent.setContent(panelGeneral));
        
        Button btnCategories = new Button("Categorías");
        btnCategories.getStyleClass().add("config-menu-btn");
        btnCategories.setMaxWidth(Double.MAX_VALUE);
        btnCategories.setOnAction(e -> scrollContent.setContent(panelCategories));
        
        VBox botones = new VBox(10, btnGeneral, btnCategories);
        coso.setCenter(botones);
        
        BorderPane content = new BorderPane();
        content.setCenter(scrollContent);
        content.setLeft(coso);
        
        BorderPane root = new BorderPane();
        root.setCenter(content);
        root.setLeft(lateralmenu.getPane());
        root.getStyleClass().add("conf-root");
        
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                nav.backScene();
                e.consume();
            }
        });
        
        return root;
        
    }
    
    private VBox getCategoriesPanel() {                                                                          
        Label categoryLabel = new Label("Categories");                                                           
        categoryLabel.setMaxWidth(Double.MAX_VALUE);                                                             
        categoryLabel.setAlignment(Pos.TOP_CENTER);                                                              

        ListView<Category> categoryListView = new ListView<>();
        categoryListView.getStyleClass().add("config-panel-listview");

        // 1. Obtener la lista base del controlador y crear la ObservableList
        List<Category> categoriasObtenidas = controller.getCategoryList();
        
        ObservableList<Category> datosObservables = FXCollections.observableArrayList();

        if (categoriasObtenidas != null) {
            for (Category c : categoriasObtenidas) {
                if (!c.getName().equalsIgnoreCase("default")) {
                    datosObservables.add(c);
                }
            }
        }
        categoryListView.setItems(datosObservables); // Asignar la lista al ListView

        // 2. CellFactory para mostrar el getName() de cada Categoría y manejar clics en vacío
        categoryListView.setCellFactory(param -> new ListCell<Category>() {
            {
                setOnMouseClicked(e -> {
                    if (isEmpty()) {
                        getListView().getSelectionModel().clearSelection();
                    }
                });
            }
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
        Button btnAdd = new Button("",icons.getAddIcon());
        btnAdd.getStyleClass().add("config-panel-button");

        btnAdd.setMinSize(50, 50);
        btnAdd.setMaxSize(50, 50);
        
        Button btnHide = new Button("",icons.getVisibleIcon());
        btnHide.getStyleClass().add("config-panel-button");

        btnHide.setMinSize(50, 50);
        btnHide.setMaxSize(50, 50);
        
        Button btnRemove = new Button("",icons.getRemoveIcon());
        btnRemove.getStyleClass().add("config-panel-button");
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
                    datosObservables.setAll(controller.getCategoryList().stream()
                        .filter(c -> !c.getName().equalsIgnoreCase("default")).toList());
                }
            });
        });
        
        // Acción del botón Eliminar (Borra lo seleccionado)
        btnRemove.setOnAction(e -> {
            Category selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
            
            if (selectedCategory != null) {
                if(!selectedCategory.getName().equalsIgnoreCase("default")){
                    controller.doRemoveCategory(selectedCategory.getName());
                    datosObservables.setAll(controller.getCategoryList().stream()
                        .filter(c -> !c.getName().equalsIgnoreCase("default")).toList());
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
            
            
            Region buttonSpacer = new Region();                                                                      
            VBox.setVgrow(buttonSpacer, Priority.ALWAYS);                                                            
            VBox categoryButtons = new VBox(5, btnAdd, buttonSpacer, btnHide, btnRemove);                            
                                                                                                                     
            Region spacer = new Region();                                                                            
            HBox.setHgrow(spacer, Priority.ALWAYS);                                                                  
            HBox listAndButtons = new HBox(categoryListView, spacer, categoryButtons);                               
                                                                                                                     
            VBox categoryConfig = new VBox(categoryLabel, listAndButtons);                                           
            VBox.setVgrow(categoryConfig, Priority.ALWAYS);                                                          
            VBox.setVgrow(categoryListView, Priority.ALWAYS);                                                        
            categoryConfig.setPadding(new Insets(5));
            return categoryConfig;                                                                                   
        }
        
    private VBox getGeneralPanel() {
        Label generalLabel = new Label("Ajustes del Lector");
        generalLabel.setMaxWidth(Double.MAX_VALUE);
        generalLabel.setAlignment(Pos.TOP_CENTER);
        
        // ComboBox de Dirección de Lectura
        Label lblDir = new Label("Sentido de Lectura:");
        javafx.scene.control.ComboBox<String> cbDirection = new javafx.scene.control.ComboBox<>();
        cbDirection.getStyleClass().add("reader-combobox");
        cbDirection.getItems().addAll("LTR (Izquierda a Derecha)", "RTL (Derecha a Izquierda)");
        cbDirection.setValue(AppConfig.get().READING_DIR.equals("LTR") ? "LTR (Izquierda a Derecha)" : "RTL (Derecha a Izquierda)");
        
        cbDirection.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                AppConfig.get().READING_DIR = newVal.contains("LTR") ? "LTR" : "RTL";
                AppConfig.get().save();
            }
        });
        
        // ComboBox de Escalado
        Label lblScale = new Label("Modo de Escalado:");
        javafx.scene.control.ComboBox<String> cbScaling = new javafx.scene.control.ComboBox<>();
        cbScaling.getStyleClass().add("reader-combobox");
        cbScaling.getItems().addAll("Ajustar a lo Alto (FIT_HEIGHT)", "Ajustar a lo Ancho (FIT_WIDTH)");
        cbScaling.setValue(AppConfig.get().SCALING_MODE.equals("FIT_HEIGHT") ? "Ajustar a lo Alto (FIT_HEIGHT)" : "Ajustar a lo Ancho (FIT_WIDTH)");
        
        cbScaling.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                AppConfig.get().SCALING_MODE = newVal.contains("Alto") ? "FIT_HEIGHT" : "FIT_WIDTH";
                AppConfig.get().save();
            }
        });
        
        VBox configBox = new VBox(15, lblDir, cbDirection, lblScale, cbScaling);
        configBox.setPadding(new Insets(20));
        
        VBox panel = new VBox(generalLabel, configBox);
        return panel;
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