package app.simplereader.views;

import app.simplereader.controller.ConfigSceneController;
import app.simplereader.controller.MainMenuController;
import app.simplereader.views.components.SideMenu;
import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import app.simplereader.model.Category;
import app.simplereader.repository.AppScene;
import app.simplereader.service.Downloader;
import app.simplereader.service.Logger;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F5;
import javafx.scene.input.KeyEvent;
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
            AppConfig.get().save();
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
        VBox panelDownloads = getDownloadsPanel();
        VBox panelAboutApp = getAboutBuildPanel();
        VBox panelPerformance = getPerformancePanel();
        VBox panelWeb = getWebPanel();
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
        
        Button btnDownloads = new Button("Descargas");
        btnDownloads.getStyleClass().add("config-menu-btn");
        btnDownloads.setMaxWidth(Double.MAX_VALUE);
        btnDownloads.setOnAction(e -> scrollContent.setContent(panelDownloads));
        
        Button btnAboutApp = new Button("Sobre la App");
        btnAboutApp.getStyleClass().add("config-menu-btn");
        btnAboutApp.setMaxWidth(Double.MAX_VALUE);
        btnAboutApp.setOnAction(e -> scrollContent.setContent(panelAboutApp));
        
        
        Button btnPerformance = new Button("Performance");
        btnPerformance.getStyleClass().add("config-menu-btn");
        btnPerformance.setMaxWidth(Double.MAX_VALUE);
        btnPerformance.setOnAction(e -> scrollContent.setContent(panelPerformance));
        
        Button btnWeb = new Button("Web");
        btnWeb.getStyleClass().add("config-menu-btn");
        btnWeb.setMaxWidth(Double.MAX_VALUE);
        btnWeb.setOnAction(e -> scrollContent.setContent(panelWeb));
        
        VBox botones = new VBox(10, btnGeneral, btnCategories, btnDownloads,btnWeb,btnAboutApp);
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
                AppConfig.get().save();
                nav.backScene();
                e.consume();
            }
        });
        
        return root;
        
    }
    
    private VBox getCategoriesPanel() {                                                                          
        Label categoryLabel = new Label("Categories");  
        categoryLabel.getStyleClass().add("downloads-title");
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
            categoryConfig.setPadding(new Insets(15));
            return categoryConfig;                                                                                   
        }
        
    private VBox getGeneralPanel() {
        Label generalLabel = new Label("Ajustes del Lector");
        generalLabel.getStyleClass().add("downloads-title");
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
        
        VBox panel = new VBox(generalLabel, configBox);
        panel.setPadding(new Insets(15));
        return panel;
    }
    
    private VBox getDownloadsPanel(){
        Downloader downloader = Downloader.getInstance();
        
        // Título
        Label titleLabel = new Label("Descargas Activas");
        titleLabel.getStyleClass().add("downloads-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.TOP_CENTER);
        
        // ListView de descargas activas
        ListView<Downloader.DownloadInfo> downloadListView = new ListView<>();
        downloadListView.getStyleClass().add("downloads-listview");
        downloadListView.setItems(downloader.getActiveDownloads());
        
        // Placeholder cuando no hay descargas
        Label placeholderLabel = new Label("No hay descargas activas");
        placeholderLabel.getStyleClass().add("downloads-placeholder");
        downloadListView.setPlaceholder(placeholderLabel);
        
        // CellFactory con barra de progreso
        downloadListView.setCellFactory(lv -> new ListCell<Downloader.DownloadInfo>() {
            private final Label chapterLabel = new Label();
            private final Label mangaLabel = new Label();
            private final Label statusLabel = new Label();
            private final ProgressBar progressBar = new ProgressBar(0);
            private final VBox cellContent;
            
            {
                chapterLabel.getStyleClass().add("download-chapter-title");
                mangaLabel.getStyleClass().add("download-manga-title");
                statusLabel.getStyleClass().add("download-status");
                progressBar.getStyleClass().add("download-progress");
                progressBar.setMaxWidth(Double.MAX_VALUE);
                
                HBox infoRow = new HBox(10, mangaLabel, statusLabel);
                infoRow.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(mangaLabel, Priority.ALWAYS);
                
                cellContent = new VBox(4, chapterLabel, infoRow, progressBar);
                cellContent.setPadding(new Insets(8, 12, 8, 12));
            }
            
            @Override
            protected void updateItem(Downloader.DownloadInfo item, boolean empty) {
                super.updateItem(item, empty);
                
                // Unbind previous
                progressBar.progressProperty().unbind();
                statusLabel.textProperty().unbind();
                
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    chapterLabel.setText(item.getChapterTitle());
                    mangaLabel.setText(item.getMangaTitle());
                    progressBar.progressProperty().bind(item.progressProperty());
                    statusLabel.textProperty().bind(item.statusProperty());
                    setGraphic(cellContent);
                }
            }
        });
        
        // Botón cancelar todo
        Button btnCancelAll = new Button("Cancelar Todo");
        btnCancelAll.getStyleClass().add("download-cancel-btn");
        btnCancelAll.setMaxWidth(Double.MAX_VALUE);
        btnCancelAll.setOnAction(e -> {
            downloader.cancelAll();
        });
        
        // Contador de descargas
        Label countLabel = new Label();
        countLabel.getStyleClass().add("downloads-count");
        downloader.getActiveDownloads().addListener((javafx.collections.ListChangeListener<Downloader.DownloadInfo>) change -> {
            int size = downloader.getActiveDownloads().size();
            if (size == 0) {
                countLabel.setText("");
            } else {
                countLabel.setText(size + (size == 1 ? " descarga activa" : " descargas activas"));
            }
        });
        // Set initial value
        int initialSize = downloader.getActiveDownloads().size();
        if (initialSize > 0) {
            countLabel.setText(initialSize + (initialSize == 1 ? " descarga activa" : " descargas activas"));
        }
        
        HBox headerRow = new HBox(10, titleLabel, countLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        VBox panel = new VBox(10, headerRow, downloadListView, btnCancelAll);
        panel.setPadding(new Insets(15));
        VBox.setVgrow(downloadListView, Priority.ALWAYS);
        
        return panel;
    }

    private VBox getAboutBuildPanel(){
        Label titleLabel = new Label("Sobre la app");
        titleLabel.getStyleClass().add("downloads-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.TOP_CENTER);
        
        Label appTitle = new Label(AppConfig.APP_TITLE+" by iDavexX");
        appTitle.getStyleClass().add("conf-label");
        Label appVersion = new Label("Build: "+AppConfig.VERSION);
        appVersion.getStyleClass().add("conf-label");

        VBox menu = new VBox(15,appTitle,appVersion);
        menu.setPadding(new Insets(5));
        VBox todo = new VBox(10,titleLabel,menu);
        todo.setPadding(new Insets(15));
        return todo;
    }
    
    private VBox getPerformancePanel(){
        Label titleLabel = new Label("Performance");  
        titleLabel.getStyleClass().add("downloads-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);                                                             
        titleLabel.setAlignment(Pos.TOP_CENTER); 
        
        Label limitLabel = new Label("Limit image quality:");
        
        javafx.scene.control.ComboBox<String> cbLimitQuality = new javafx.scene.control.ComboBox<>();
        cbLimitQuality.getStyleClass().add("reader-combobox");
        cbLimitQuality.getItems().addAll("Activado", "Desactivado");
        cbLimitQuality.setValue(AppConfig.get().limitPageQuality ? "Activado" : "Desactivado");
        
        cbLimitQuality.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                AppConfig.get().limitPageQuality = newVal.equals("Activado");
                AppConfig.get().save();
            }
        });
        
        VBox configBox = new VBox(15, limitLabel, cbLimitQuality);
        VBox panel = new VBox(15,titleLabel,configBox);
        panel.setPadding(new Insets(15));
        return panel; 
        
    }
    
    private VBox getWebPanel(){
        Label titleLabel = new Label("Web");  
        titleLabel.getStyleClass().add("downloads-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);                                                             
        titleLabel.setAlignment(Pos.TOP_CENTER); 
        
        Label limitLabel = new Label("User-Agent");
        TextField useragentField = new TextField();
        useragentField.setText(AppConfig.get().USER_AGENT);
        useragentField.textProperty().addListener((obs, oldVal, newVal) -> {
            AppConfig.get().USER_AGENT = newVal;
        });
        useragentField.setMaxWidth(Double.MAX_VALUE);
        useragentField.setMaxHeight(40);
        VBox configBox = new VBox(15,limitLabel,useragentField);
        
        VBox panel = new VBox(15,titleLabel,configBox);
        panel.setPadding(new Insets(15));

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