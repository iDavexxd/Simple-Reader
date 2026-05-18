package app.simplereader.views;

import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.controller.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaSource;

import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

/**
 *
 * @author david
 */
public class ScnMainMenu implements AppScene{
    private static ScnMainMenu instance;
    
    private final SceneController nav = SceneController.getInstance();    
    private final LibraryController lib = LibraryController.getInstance();
    
    private ScrollPane scroll;
    private TilePane activePane;
    private String currentCategory = "Default";
    
    private MainMenuController controller;
    
    private static Map<String, TilePane> categoryPanes = new HashMap<>();
    
    public ScnMainMenu(){
        MainMenuController.doInstance(this);
        this.controller = MainMenuController.getInstance();
        nav.getStage().setResizable(false);
    }
    
    public static ScnMainMenu getInstance(){
        return instance;
    }
    
    public SceneController getSceneController(){
        return this.nav;
    }
 
    @Override
    public Scene getScene(){      
        
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;        
        
        // Crear TilePanes para cada categoría
        controller.doCreateCategoryPanes();
        
        // Pane activo por defecto
        activePane = categoryPanes.getOrDefault("Default", controller.createTilePane("Default", hgap, vgap, padding, columns));
        categoryPanes.putIfAbsent("Default", activePane);
        
        // Cargar mangas en los tiles
        controller.loadAllTiles();
        
        // Scroll
        scroll = new ScrollPane(activePane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        
        // Iconos
        
        SVGPath icnReload = new SVGPath();
        icnReload.setContent("M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-110h80v280H520v-80h168q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q77 0 139-44t87-116h84q-28 106-114 173t-196 67Z");
        icnReload.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        icnReload.setScaleX(scale);
        icnReload.setScaleY(scale);
        
        Group icon_reload_group = new Group(icnReload);
        StackPane icon_container = new StackPane(icon_reload_group);
        icon_container.setPrefSize(24, 24);
        icon_container.setMaxSize(24, 24);
        
        SVGPath icnImportar = new SVGPath();
        icnImportar.setContent("M440-320v-326L336-542l-56-58 200-200 200 200-56 58-104-104v326h-80ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z");
        icnImportar.getStyleClass().add("icon");
        icnImportar.setScaleX(scale);
        icnImportar.setScaleY(scale);
        
        Group icon_importar_group = new Group(icnImportar);
        StackPane icon_container_importar = new StackPane(icon_importar_group);
        
        SVGPath icnAdd = new SVGPath();
        icnAdd.setContent("M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z");
        icnAdd.getStyleClass().add("icon");
        icnAdd.setScaleX(scale);
        icnAdd.setScaleY(scale);
        
        Group icon_add_group = new Group(icnAdd);
        StackPane icon_container_add = new StackPane(icon_add_group);
        
        // Botones
        
        Button btnAdd = new Button("",icon_container_add);
        Button btnImportar = new Button("",icon_container_importar);
        btnImportar.setMinSize(24, 24);
        btnImportar.setMaxSize(24, 24);
        Button btnReload = new Button("",icon_container);
        btnReload.setMinSize(24, 24);
        btnReload.setMaxSize(24,24);
        
        Button btnConfig = new Button("");
        btnConfig.setOnAction(e -> { 
            nav.goTo(new ScnConfig(this.nav));
        });
        
        btnReload.setOnAction(e -> {       
            Logger.info("- Starting mangas reload.");
            controller.reloadMangas(); 
        });
        btnImportar.setOnAction(e -> {
            importFolder();
        });
        
                   
        scroll.widthProperty().addListener((obs, oldVal, newVal) -> {
            controller.resizeTiles(newVal.doubleValue());
        });
        
        // Menu con las categorias
        
        HBox categoryButtons = new HBox(hgap);
        Button btnDefCat = new Button("Default");
        btnDefCat.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDefCat, Priority.ALWAYS);
        btnDefCat.setOnAction(e -> {
            controller.showCategory("Default");
        });
        categoryButtons.getChildren().add(btnDefCat);
        categoryButtons.setPadding(new Insets(15, 15, 0, 15));
        categoryButtons.setSpacing(2);

        for (Category cat : lib.getAllCategories()) {
            if (!cat.getName().equals("Default")) {
                Button btnCategory = new Button(cat.getName());
                HBox.setHgrow(btnCategory, Priority.ALWAYS);
                btnCategory.setMaxWidth(Double.MAX_VALUE);
                btnCategory.setOnAction(e -> {
                    controller.showCategory(cat.getName());
                });
                categoryButtons.getChildren().add(btnCategory);
            }
        }
        
        BorderPane categoriesPane = new BorderPane();
        categoriesPane.setCenter(categoryButtons);
        categoriesPane.setMinHeight(30);
        categoriesPane.setMaxHeight(30);
        
        VBox scrlandpane = new VBox(categoriesPane, scroll);
        
        // Menu lateral
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(btnReload).addBottom(btnImportar);
        lateralmenu.addBottom(btnAdd);
        lateralmenu.addBottom(btnConfig);

        BorderPane panel = new BorderPane();
        panel.setCenter(scrlandpane);
        panel.setLeft(lateralmenu.getPane());
        
        StackPane root = new StackPane();
        root.getChildren().add(panel);
        StackPane overlay = createSourcePickerOverlay();
        root.getChildren().add(overlay);
        btnAdd.setOnAction(e -> {
            overlay.setVisible(true);
            overlay.setManaged(true);
        });
        
        Scene rootCache = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        rootCache.getStylesheets().add(nav.getCss());
        rootCache.setOnKeyPressed( e -> {
            KeyCode key = e.getCode();
            switch (key){
                case DIGIT1 -> controller.showCategory("Default");
                case F5 -> {
                    Logger.info("F5");
                    controller.reloadMangas();
                }
                case F12 -> importFolder();
            }
        });
        
        return rootCache;
    }
    
    private StackPane createSourcePickerOverlay() {
        StackPane overlay = new StackPane();
        overlay.setVisible(false); // Oculto por defecto
        overlay.setManaged(false); // No afecta al layout cuando está oculto
        // Fondo oscuro semitransparente
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle();
        bg.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.6));
        bg.widthProperty().bind(overlay.widthProperty());
        bg.heightProperty().bind(overlay.heightProperty());
        // La "tarjeta" central
        VBox card = new VBox(15);
        card.getStyleClass().add("picker-card"); // Para CSS
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(300);
        Label title = new Label("Selecciona una fuente");
        title.getStyleClass().add("picker-title");
        VBox sourcesList = new VBox(10);
        sourcesList.setAlignment(Pos.CENTER);
        // Botones para cada Source
        for (MangaSource src : SourceManager.getInstance().getAllSources()) {
            Button btn = new Button(src.getName());
            btn.getStyleClass().add("picker-btn");
            btn.setMaxWidth(Double.MAX_VALUE);

            btn.setOnAction(e -> {
                overlay.setVisible(false);
                overlay.setManaged(false);
                controller.doGoToSource(src);
            });

            sourcesList.getChildren().add(btn);
        }
        // Botón cancelar
        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().add("picker-btn-cancel");
        btnCancel.setOnAction(e -> {
            overlay.setVisible(false);
            overlay.setManaged(false);
        });
        card.getChildren().addAll(title, sourcesList, btnCancel);
        overlay.getChildren().addAll(bg, card);
        return overlay;
    }

    
    
    private void importFolder() {
        // TODO: Implementar importación de carpetas
        Logger.info("Import folder - pending implementation");
    }

        
    public Map<String, TilePane> getCategoriesPanes(){
        return ScnMainMenu.categoryPanes;
    }
    
    public TilePane getActivePane(){
        return activePane;
    }
    
    public ScrollPane getScroll(){
        return scroll;
    }
   
    @Override
    public String getName(){
        return "Menu";
    }
    @Override
    public String getParentName(){
        return "Main";
    }
    
    /*
    Setters:
    */
    public void setActivePane(TilePane pane){
        activePane = pane;
    }
    
    public void setCurrentCategory(String name){
        currentCategory = name;
    }
}
