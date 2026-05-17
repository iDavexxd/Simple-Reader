package app.simplereader.views;

import app.simplereader.model.AppConfig;
import app.simplereader.controller.CategoryController;
import app.simplereader.controller.Logger;
import app.simplereader.controller.MainMenuController;
import app.simplereader.controller.SceneController;
import app.simplereader.model.mdManga;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F7;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaInterface;

/**
 *
 * @author david
 */
public class ScnMainMenu implements AppScene{
    private final SceneController nav;    
    private final MainMenuController controller;
    
    private static Boolean mangasLoaded = false;
    
    private static ScrollPane scroll;
    
    public static CategoryController manager = new CategoryController();
    private static TilePane DefaultPane = manager.getCategories().get("Default").getPane().getPane();
    
    
    public ScnMainMenu(SceneController nav){
        this.nav = nav;
        this.controller = new MainMenuController(nav, manager, this);
        nav.getStage().setResizable(false);
    }
    
    public SceneController getSceneController(){
        return this.nav;
    }
 
    @Override
    @SuppressWarnings("empty-statement")
    public Scene getScene(){      
        
        
        
        if(!mangasLoaded){
            controller.loadMangas();
            mangasLoaded = true;
            // Cargar los mangas solo si no se han cargao
        }

        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;        
        
        
        // Panel con las tiles
        DefaultPane.setHgap(hgap);
        DefaultPane.setVgap(vgap);
        DefaultPane.setPadding(new Insets(padding));
        DefaultPane.setPrefColumns(columns);
        
        
        
        // Scroll
        scroll = new ScrollPane(DefaultPane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        
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
            controller.importFolder();
        });
        btnAdd.setOnAction(e -> {
            String id = showInputDialog("Ingresar ID del manga");
            if (!id.isBlank()) {
                MangaInterface manga = new mdManga(id);
                manga.saveData();
                controller.reloadMangas();
            }
        });
                   
        
        scroll.setFitToWidth(true);
        
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

        for(String name:manager.getNameList()){
            Button btnCategory = new Button(name);
            HBox.setHgrow(btnCategory, Priority.ALWAYS);

            btnCategory.setMaxWidth(Double.MAX_VALUE);
            btnCategory.setOnAction(e -> {
                controller.showCategory(name);
            });
            categoryButtons.getChildren().add(btnCategory);
        }
        
        
        BorderPane categoriesPane = new BorderPane();
        categoriesPane.setCenter(categoryButtons);
        categoriesPane.setMinHeight(30);
        categoriesPane.setMaxHeight(30);
        
        VBox scrlandpane = new VBox(categoriesPane,scroll);
        
        
        // Menu lateral
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(btnReload).addBottom(btnImportar);
        lateralmenu.addBottom(btnAdd);
        lateralmenu.addBottom(btnConfig);

        BorderPane panel = new BorderPane();
        panel.setCenter(scrlandpane);
        panel.setLeft(lateralmenu.getPane());
        scroll.widthProperty().addListener((obs, oldVal, newVal) -> {
            controller.resizeTiles(newVal.doubleValue());
        });
        
        // Panel con todo
        
        
        Scene rootCache = new Scene(panel,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        rootCache.getStylesheets().add(nav.getCss());
        rootCache.setOnKeyPressed( e -> {
            KeyCode key = e.getCode();
            switch (key){
                case DIGIT1 -> controller.showCategory("Default"); // siempre Default
                case F5 -> {
                    Logger.info("F5");
                    controller.reloadMangas();
                }
                case F12 ->{
                    controller.importFolder();
                }    
                case F7 -> {
                    nav.goTo(new TestScene(nav));
                }
            
            }
        });
        if (!controller.equals("Default")){
            controller.showCategory(controller.getActualCategory());
        }
        return rootCache;
    }

    public static String showInputDialog(String titulo) {
        Stage dialog = new Stage();
        dialog.setTitle(titulo);
        dialog.initModality(Modality.APPLICATION_MODAL); // bloquea la ventana principal

        TextField textField = new TextField();
        Button btnOk = new Button("OK");

        final String[] resultado = {""};

        btnOk.setOnAction(e -> {
            resultado[0] = textField.getText();
            dialog.close();
        });

        VBox layout = new VBox(10, textField, btnOk);
        layout.setPadding(new Insets(20));

        dialog.setScene(new Scene(layout, 300, 100));
        dialog.showAndWait(); // espera a que se cierre

        return resultado[0];
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
}