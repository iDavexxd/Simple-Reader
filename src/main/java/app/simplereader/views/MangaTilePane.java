package app.simplereader.views;

import app.simplereader.controller.MangaTilePaneController;
import app.simplereader.controller.SceneController;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;

/**
 *
 * @author david
 */
public class MangaTilePane {  
    
    
    private ScrollPane scroll;
    private TilePane panel;
    
    
    public MangaTilePane(SceneController nav){
        this.scroll = new ScrollPane();
        MangaTilePaneController controller = new MangaTilePaneController(this.panel, nav,this);
        
        initPaneConfig();
        initScrollConfig();
    }
    
    private void initPaneConfig(){
        panel = new TilePane();
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;        
        
        
        // Panel con las tiles
        panel.setHgap(hgap);
        panel.setVgap(vgap);
        panel.setPadding(new Insets(padding));
        panel.setPrefColumns(columns);
        
    }  
    
    private void initScrollConfig(){
        scroll.setContent(panel);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
    }
    
    
    public ScrollPane getPane(){
        return scroll;
    }
}
