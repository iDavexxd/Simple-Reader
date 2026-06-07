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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/**
 *
 * @author david
 */
public class ScnMainMenu implements AppScene{
    private static ScnMainMenu instance;
    
    private final SceneController nav = SceneController.getInstance();    
    private final LibraryController lib = LibraryController.getInstance();
    
    private static TilePane activePane;
    private String currentCategory = "Default";
    private BorderPane sourceBorderPane;
    private VBox sourceButtons;
    private TabPane categoryTabPane;
            
    private boolean sourceMenuVisible = false;
    
    private final MainMenuController controller;
    
    private static final Map<String, TilePane> categoryPanes = new HashMap<>();
    
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
        
        // Crear TilePanes para cada categoría
        controller.doCreateCategoryPanes();
        
        // Cargar mangas en los tiles
        controller.loadAllTiles();
        
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
        
        SVGPath icnAdd = new SVGPath();
        icnAdd.setContent("M352-120H200q-33 0-56.5-23.5T120-200v-152q48 0 84-30.5t36-77.5q0-47-36-77.5T120-568v-152q0-33 23.5-56.5T200-800h160q0-42 29-71t71-29q42 0 71 29t29 71h160q33 0 56.5 23.5T800-720v160q42 0 71 29t29 71q0 42-29 71t-71 29v160q0 33-23.5 56.5T720-120H568q0-50-31.5-85T460-240q-45 0-76.5 35T352-120Zm-152-80h85q24-66 77-93t98-27q45 0 98 27t77 93h85v-240h80q8 0 14-6t6-14q0-8-6-14t-14-6h-80v-240H480v-80q0-8-6-14t-14-6q-8 0-14 6t-6 14v80H200v88q54 20 87 67t33 105q0 57-33 104t-87 68v88Zm260-260Z");
        icnAdd.getStyleClass().add("icon");
        icnAdd.setScaleX(scale);
        icnAdd.setScaleY(scale);
        
        Group icon_add_group = new Group(icnAdd);
        StackPane icon_container_add = new StackPane(icon_add_group);
        
        SVGPath icnConfig = new SVGPath();
        icnConfig.setContent("m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-13.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l119-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24 12L590-80H370Zm70-80h79l14-106q31-8 57.5-23.5T639-327l99 41 39-68-86-65q5-14 7-29.5t2-31.5q0-16-2-31.5t-7-29.5l86-65-39-68-99 42q-22-23-48.5-38.5T533-694l-13-106h-79l-14 106q-31 8-57.5 23.5T321-633l-99-41-39 68 86 64q-5 15-7 30t-2 32q0 16 2 31t7 30l-86 65 39 68 99-42q22 23 48.5 38.5T427-266l13 106Zm42-180q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Zm-2-140Z");
        icnConfig.getStyleClass().add("icon");
        icnConfig.setScaleX(scale);
        icnConfig.setScaleY(scale);
        
        Group icnConfig_group = new Group(icnConfig);
        StackPane icnConfig_container = new StackPane(icnConfig_group);
        
        // Botones
        Button btnAdd = new Button("",icon_container_add);
        
        Button btnReload = new Button("",icon_container);
        btnReload.setMinSize(24, 24);
        btnReload.setMaxSize(24,24);
        
        Button btnConfig = new Button("",icnConfig_container);
        btnConfig.setMinSize(24, 24);
        btnConfig.setMaxSize(24,24);
        btnConfig.setOnAction(e -> { 
            nav.goTo(new ScnConfig());
        });
        
        btnAdd.setMinSize(24, 24);
        btnAdd.setMaxSize(24, 24);
        btnReload.setOnAction(e -> {        
            Logger.info("- Starting mangas reload.");
            controller.reloadMangas(); 
        });

        // Configuración del tabpane
        categoryTabPane = new TabPane();
        categoryTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        categoryTabPane.getStyleClass().add("category-tabpane");
        
        categoryTabPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            int totalTabs = categoryTabPane.getTabs().size();
            if (totalTabs > 0) {
                // Restamos 25 píxeles para compensar el padding interno de JavaFX
                double tabWidth = (newVal.doubleValue() / totalTabs) - 25;

                categoryTabPane.setTabMinWidth(tabWidth);
                categoryTabPane.setTabMaxWidth(tabWidth);
            }
        });
        doCreateCategoryTabs();

        // Menu lateral
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(btnReload);
        lateralmenu.addBottom(btnAdd);
        lateralmenu.addBottom(btnConfig);

        BorderPane panel = new BorderPane();
        panel.setCenter(categoryTabPane); // El TabPane toma todo el centro
        panel.setLeft(lateralmenu.getPane());
        
        doCreateSourcePane();
        sourceBorderPane.setVisible(sourceMenuVisible);
        
        StackPane root = new StackPane();
        root.getChildren().add(panel);
        root.getChildren().add(sourceBorderPane);
        
        btnAdd.setOnAction(e -> {
            doShowSourceMenu();
        });
        
        Scene rootCache = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        rootCache.getStylesheets().add(nav.getCss());
        rootCache.setOnKeyPressed( e -> {
            KeyCode key = e.getCode();
            switch (key){
                // case DIGIT1 -> controller.showCategory("Default"); // <- Puedes borrar esto también en tu MainMenuController si ya no usas atajos para cambiar de categoría
                case F5 -> {
                    Logger.info("F5");
                    controller.reloadMangas();
                }
                case F12 -> importFolder();
            }
        });
        
        return rootCache;
    }
    
    public void doCreateCategoryTabs(){
        // Recorremos todas las categorías de la librería
        for (Category cat : lib.getAllCategories()) {
            if (!cat.isHide()) {
                Tab tab = new Tab(cat.getName());

                TilePane tempPane = getCategoriesPanes().get(cat.getName());
                if (tempPane == null) {
                    tempPane = controller.createTilePane(cat.getName(), 15, 15, 15, 5);
                    getCategoriesPanes().put(cat.getName(), tempPane);
                }

                final TilePane finalPane = tempPane;

                ScrollPane tabScroll = new ScrollPane(finalPane); 
                tabScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                tabScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                tabScroll.setFitToWidth(true);
                tabScroll.setFitToHeight(true); 

                tabScroll.widthProperty().addListener((obs, oldVal, newVal) -> {
                    if (activePane == finalPane) {
                        controller.resizeTiles(newVal.doubleValue());
                    }
                });

                tab.setContent(tabScroll);
                categoryTabPane.getTabs().add(tab);
            }
        }

        // Listener para cuando cambias de pestaña manualmente
        categoryTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                String tabName = newTab.getText();
                activePane = getCategoriesPanes().get(tabName);
                currentCategory = tabName;

                ScrollPane activeScroll = (ScrollPane) newTab.getContent();
                controller.resizeTiles(activeScroll.getWidth());
            }
        });

        if (!categoryTabPane.getTabs().isEmpty()) {
            Tab firstTab = categoryTabPane.getTabs().get(0);
            activePane = getCategoriesPanes().get(firstTab.getText());
            currentCategory = firstTab.getText();

            javafx.application.Platform.runLater(() -> {
                ScrollPane activeScroll = (ScrollPane) firstTab.getContent();
                if (activeScroll != null && activeScroll.getWidth() > 0) {
                    controller.resizeTiles(activeScroll.getWidth());
                }
            });
        }
    }
    
    public void doCreateSourceButtons(){
        for(MangaSource source : SourceManager.getInstance().getAllSources()){
            Button btnSource = new Button(source.getName());
            btnSource.setOnAction(e -> {
                nav.goTo(new ScnSourceSearch(source));
                doHideSourceMenu();
            });
            btnSource.setMaxSize(Double.MAX_VALUE, 30);
            
            sourceButtons.getChildren().add(btnSource);
        }
    }
    
    private void doShowSourceMenu(){
        sourceMenuVisible = true;
        sourceBorderPane.setVisible(sourceMenuVisible);
    }
    
    private void doHideSourceMenu(){
        sourceMenuVisible = false;
        sourceBorderPane.setVisible(sourceMenuVisible);
    }
    
    public void doCreateSourcePane(){
        //top
        Label menuTitle = new Label();
        menuTitle.setText("Sources");
        
        Button btnClose = new Button("x");
        btnClose.setMinSize(30, 30);
        btnClose.setMaxSize(30, 30);
        btnClose.setOnAction(e -> doHideSourceMenu());
        
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        
        HBox topcontent = new HBox(menuTitle,topSpacer,btnClose);
        

        // center
        sourceButtons = new VBox();
        doCreateSourceButtons();
        
        ScrollPane centercontent = new ScrollPane(sourceButtons);
        centercontent.setFitToWidth(true);
        
        //Bottom
        Button btnImport = new Button("+");
        btnImport.setMinSize(30, 30);
        btnImport.setMaxSize(Double.MAX_VALUE, 30);
        
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox allContent = new VBox(10,topcontent,centercontent,bottomSpacer,btnImport);
        
        StackPane sourceMenu = new StackPane(allContent);
        sourceMenu.getStyleClass().add("source-menu");
        sourceMenu.setPadding(new Insets(15));

        sourceMenu.setMaxSize(300, 450);
        sourceMenu.setMinHeight(450);

        sourceBorderPane = new BorderPane();
        sourceBorderPane.getStyleClass().add("menu-background");
        sourceBorderPane.setCenter(sourceMenu);
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
    
    @Override
    public String getName(){
        return "Menu";
    }
    @Override
    public String getParentName(){
        return "Main";
    }
    
    /*
    Getters:
    */
    public TabPane getCategoryTabPane(){
        return this.categoryTabPane;
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