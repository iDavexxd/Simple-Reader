package app.simplereader.scenes.others;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class SideMenu {
    private final VBox pane;
    private final VBox bottom;
    private final VBox top;
    
    public SideMenu(){
        top = new VBox(8);
        top.setAlignment(Pos.TOP_CENTER); 
        bottom = new VBox(8);
        bottom.setAlignment(Pos.BOTTOM_CENTER);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        pane = new VBox(top,spacer,bottom);
        pane.getStyleClass().add("side-menu");
        pane.setPrefWidth(45);
        pane.setMinWidth(45);
        pane.setMaxWidth(45);
        pane.setAlignment(Pos.CENTER);
    }
    
    public SideMenu addTop(Parent parent){
        top.getChildren().add(parent);
        return this;
    }
    
    public SideMenu addBottom(Parent parent){
        bottom.getChildren().add(parent);
        return this;
    }
    public VBox getPane(){
        return pane;
    }
}
