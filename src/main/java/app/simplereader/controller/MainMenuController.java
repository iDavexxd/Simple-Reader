package app.simplereader.controller;

import app.simplereader.model.Category;
import app.simplereader.model.Manga;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.components.MangaTile;
import app.simplereader.views.ScnMainMenu;
import app.simplereader.views.ScnSource;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class MainMenuController {
    
    private ScnMainMenu scene;
    private Boolean tilesLoaded = false;

    public static MainMenuController instance;
    public SceneController nav = SceneController.getInstance();
    private final LibraryController lib = LibraryController.getInstance();
    
    public MainMenuController(ScnMainMenu scene){
        this.scene = scene;
    }
    
    public static void doInstance(ScnMainMenu scene){
        MainMenuController.instance = new MainMenuController(scene);
    }
    
    public static MainMenuController getInstance(){        
        return instance;
    }
    
    public void doBackScene(){
        nav.backScene();
    }
    
    public void doGoToSource(MangaSource src){
        nav.goTo(new ScnSource(src));
    }
    
    public void doCreateCategoryPanes(){
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;  
        
        if(scene.getCategoriesPanes().isEmpty() || scene.getCategoriesPanes() == null){
            for (Category cat : lib.getAllCategories()) {
                TilePane pane = createTilePane(cat.getName(), hgap, vgap, padding, columns);
                scene.getCategoriesPanes().put(cat.getName(), pane);
            }
        }
    }
    
    public void loadAllTiles() {
        if(tilesLoaded) return;
        
        for (Category cat : lib.getAllCategories()) {
            TilePane pane = scene.getCategoriesPanes().get(cat.getName());
            if (pane == null) {
                pane = createTilePane(cat.getName(), 15, 15, 15, 5);
                scene.getCategoriesPanes().put(cat.getName(), pane);
            }
            
            List<Manga> mangas = lib.getMangasByCategory(cat.getName());
            for (Manga manga : mangas) {
                if (manga.getCoverURL() != null) {
                    VBox tile = MangaTile.create(manga);
                    pane.getChildren().add(tile);
                } else {
                    Logger.warning(manga.getTitle() + " - no tiene cover.");
                }
            }
        }
        
        tilesLoaded = true;
    }
    
    public TilePane createTilePane(String name, double hgap, double vgap, double padding, int columns) {
        TilePane pane = new TilePane();
        pane.setHgap(hgap);
        pane.setVgap(vgap);
        pane.setPadding(new Insets(padding));
        pane.setPrefColumns(columns);
        return pane;
    }
    
    public void showCategory(String name) {
        TilePane pane = scene.getCategoriesPanes().get(name);
        if (pane == null) {
            pane = createTilePane(name, 15, 15, 15, 5);
            scene.getCategoriesPanes().put(name, pane);
        }
        
        scene.setActivePane(pane);
        scene.getScroll().setContent(scene.getActivePane());
        scene.setCurrentCategory(name);
        resizeTiles(scene.getScroll().getWidth());
    }
    
    public void resizeTiles(double totalWidth) {
        TilePane pane = scene.getActivePane();
        if (pane == null) return;

        int columns = 5;
        double hgap = 15, vgap = 15, padding = 15;
        double tileWidth = (totalWidth - padding * 2 - hgap * (columns - 1) - 5) / columns;
        double tileHeight = tileWidth * 1.5;

        pane.setPrefTileWidth(tileWidth);
        pane.setPrefTileHeight(tileHeight + 45);

        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof VBox vbox) {
                vbox.setPrefWidth(tileWidth);
                vbox.setPrefHeight(tileHeight + 45);
                if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof StackPane container) {
                    container.setPrefWidth(tileWidth);
                    container.setPrefHeight(tileHeight);
                }
            }
        }
    }
    
    public void reloadMangas() {
        // Limpiar todos los panes
        for (TilePane pane : scene.getCategoriesPanes().values()) {
            pane.getChildren().clear();
        }
        tilesLoaded = false;
        // Recargar desde LibraryController
        loadAllTiles();
        resizeTiles(scene.getScroll().getWidth());
    }
    /*
    Setters:
    */
    
    public void setScene(AppScene scene){
        if(scene instanceof ScnMainMenu){
            this.scene = (ScnMainMenu) scene;
        }
    }
}
