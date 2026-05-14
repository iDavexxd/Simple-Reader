package app.simplereader.views;

import app.simplereader.controller.MangaTileViewController;
import app.simplereader.controller.SceneController;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;

/**
 *
 * @author david
 */
public class MangaTileView {  
    
    
    private ScrollPane scroll;
    private TilePane panel;
    private MangaTileViewController controller;
    
    public MangaTileView(SceneController nav){
        initPaneConfig();
        this.scroll = new ScrollPane();
        this.controller = new MangaTileViewController(this.panel, nav,this);
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
        
        scroll = new ScrollPane(panel); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        
              
    }
    
    public ScrollPane getPane(){
        return scroll;
    }
}
