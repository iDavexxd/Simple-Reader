package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.Category;
import app.simplereader.model.Manga;
import app.simplereader.repository.AppExtension;
import app.simplereader.repository.AppScene;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.components.MangaTile;
import app.simplereader.views.ScnMainMenu;
import app.simplereader.views.ScnSourceSearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.application.Platform;

/**
 *
 * @author david
 */
public class MainMenuController {
    
    public static final int MAX_COLUMNS = 7;
    public static final double MIN_TILE_WIDTH = 150.0;

    private ScnMainMenu scene;
    private Boolean tilesLoaded = false;
    private final Map<String, List<MangaTile>> allTiles = new HashMap<>();
    
    public static MainMenuController instance;
    public SceneController nav = SceneController.getInstance();
    private final LibraryController lib = LibraryController.getInstance();
    
    private String currentCategory = null;
    private ScrollPane activeScroll;
    
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
    
    public void doGoToSource(AppExtension extension){
        nav.goTo(new ScnSourceSearch(extension));
    }
    
    
    
    public void doCreateCategoryPanes(){
        int columns = MAX_COLUMNS;
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
    
    public void reloadCategoryTabs(){
        scene.getCategoryTabPane().getTabs().clear();
        scene.doCreateCategoryTabs();
    }
    
    public void showCategory(String name) {
  	// 1. Unload la anterior
        if (currentCategory != null) {
            List<MangaTile> previousTiles = allTiles.get(currentCategory);
            if (previousTiles != null) {
                previousTiles.forEach(MangaTile::unloadImage);
            }
        }

        // 2. La carga se hace por visibilidad (lazy loading)
        currentCategory = name;
        Platform.runLater(this::updateVisibleTiles);
    }
    
    public void setActiveScroll(ScrollPane scroll) {
        this.activeScroll = scroll;
    }
    
    public void updateVisibleTiles() {
        if (currentCategory == null || activeScroll == null) return;
        List<MangaTile> tiles = allTiles.get(currentCategory);
        if (tiles == null || tiles.isEmpty()) return;
        
        TilePane pane = scene.getCategoriesPanes().get(currentCategory);
        if (pane == null) return;
        
        javafx.geometry.Bounds viewportBounds = activeScroll.getViewportBounds();
        double viewportHeight = viewportBounds.getHeight();
        double contentHeight = pane.getHeight();
        
        if (viewportHeight <= 0 || contentHeight <= 0) {
            // Layout no listo, cargar todo como fallback
            tiles.forEach(MangaTile::loadImage);
            return;
        }
        
        int columns = pane.getPrefColumns();
        if (columns <= 0) columns = 1;
        
        double tileHeight = pane.getPrefTileHeight();
        double vgap = pane.getVgap();
        double paddingTop = pane.getPadding().getTop();
        
        if (tileHeight <= 0) {
            tiles.forEach(MangaTile::loadImage);
            return;
        }
        
        double scrollableHeight = Math.max(0, contentHeight - viewportHeight);
        double topY = activeScroll.getVvalue() * scrollableHeight;
        double bottomY = topY + viewportHeight;
        
        double rowHeight = tileHeight + vgap;
        
        // Buffer de 1 fila arriba y abajo para scroll suave
        int firstVisibleRow = Math.max(0, (int) Math.floor((topY - paddingTop) / rowHeight) - 1);
        int lastVisibleRow = (int) Math.ceil((bottomY - paddingTop) / rowHeight) + 1;
        
        int firstIndex = firstVisibleRow * columns;
        int lastIndex = Math.min(tiles.size() - 1, (lastVisibleRow + 1) * columns - 1);
        
        for (int i = 0; i < tiles.size(); i++) {
            if (i >= firstIndex && i <= lastIndex) {
                tiles.get(i).loadImage();
            } else {
                tiles.get(i).unloadImage();
            }
        }
    }
    
    public void loadAllTiles() {
        if(tilesLoaded) return;
        
        for (Category cat : lib.getAllCategories()) {
            TilePane pane = scene.getCategoriesPanes().get(cat.getName());
            if (pane == null) {
                pane = createTilePane(cat.getName(), 15, 15, 15, MAX_COLUMNS);
                scene.getCategoriesPanes().put(cat.getName(), pane);
            }
            allTiles.put(cat.getName(), new ArrayList<>());

            
            for (Manga manga : lib.getMangasByCategory(cat.getName()).values()) {
                if (manga.getCoverURL() != null) {
                    MangaTile mangaTile = new MangaTile(manga);      
                    allTiles.get(cat.getName()).add(mangaTile);          
                    pane.getChildren().add(mangaTile.getTile());
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
    
    
    
    public void resizeTiles(double totalWidth) {
        if (totalWidth <= 50) return;
        TilePane pane = scene.getActivePane();
        if (pane == null) return;

        double hgap = 15, vgap = 15, padding = 15;
        double minTileWidth = MIN_TILE_WIDTH; 

        int columns = (int) Math.floor((totalWidth - padding * 2 + hgap) / (minTileWidth + hgap));
        columns = Math.max(1, Math.min(MAX_COLUMNS, columns));

        pane.setPrefColumns(columns);

        double tileWidth = Math.floor((totalWidth - padding * 2 - hgap * (columns - 1)) / columns);
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
    public void unloadAllCovers() {
        allTiles.values().forEach(list -> list.forEach(MangaTile::unloadImage));
    }
    public void reloadMangas() {
        allTiles.values().forEach(list -> list.forEach(MangaTile::unloadImage));
        allTiles.clear();
        currentCategory = null; 
        for (TilePane pane : scene.getCategoriesPanes().values()) {
            pane.getChildren().clear();
        }
        tilesLoaded = false;

        loadAllTiles();

        TilePane currentPane = scene.getActivePane();
        if (currentPane != null && currentPane.getParent() instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane activeScroll = (javafx.scene.control.ScrollPane) currentPane.getParent();
            if (activeScroll.getViewportBounds().getWidth() > 0) {
                resizeTiles(activeScroll.getViewportBounds().getWidth());
            }
        }
        
        String activeCat = scene.getCurrentCategory();
        if (activeCat != null) {
            showCategory(activeCat);
        }
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
