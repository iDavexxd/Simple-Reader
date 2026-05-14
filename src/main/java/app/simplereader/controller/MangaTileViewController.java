package app.simplereader.controller;

import app.simplereader.repository.Manga;
import app.simplereader.views.MangaTile;
import app.simplereader.views.MangaTileView;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class MangaTileViewController {
    
    private TilePane pane;
    private final SceneController nav;
    private List<Manga> mangas;
    private MangaTileView view;
    
    public MangaTileViewController(TilePane pane, SceneController nav, MangaTileView view){
        this.pane = pane;
        this.nav = nav;
        this.view = view;
    }

    
    public void addManga(Manga manga){
        pane.getChildren().add(MangaTile.create(manga, nav));
        
    }
    
    public void reloadMangas(){
        pane.getChildren().clear();
    }

        
    private void createTiles(){
        for(Manga manga: mangas){
            addManga(manga);
        }
    }
    
    public void resizeTiles() {
        double totalWidth = view.getPane().getWidth();
         // Obtener el pane activo en lugar de siempre DefaultPane
        TilePane activePane = pane;
        if (activePane == null) return;

        int columns = 5;
        double hgap = 15, vgap = 15, padding = 15;
        double tileWidth = (totalWidth - padding * 2 - hgap * (columns - 1) - 5) / columns;
        double tileHeight = tileWidth * 1.5;

        activePane.setPrefTileWidth(tileWidth);
        activePane.setPrefTileHeight(tileHeight + 45);

        for (javafx.scene.Node node : activePane.getChildren()) {
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
}
