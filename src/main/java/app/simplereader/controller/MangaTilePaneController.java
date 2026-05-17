package app.simplereader.controller;

import app.simplereader.views.MangaTile;
import app.simplereader.views.MangaTilePane;
import java.util.List;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import app.simplereader.repository.MangaInterface;

/**
 *
 * @author david
 */
public class MangaTilePaneController {
    
    private TilePane pane;
    private final SceneController nav;
    private List<MangaInterface> mangas;
    private MangaTilePane view;
    
    public MangaTilePaneController(TilePane pane, SceneController nav, MangaTilePane view){
        this.pane = pane;
        this.nav = nav;
        this.view = view;
    }

    
    public void addManga(MangaInterface manga){
        pane.getChildren().add(MangaTile.create(manga, nav));
    }
      

        
    public void createTiles(){
        for(MangaInterface manga: mangas){
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
