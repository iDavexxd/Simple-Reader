package app.simplereader.views.components;

import app.simplereader.controller.SceneController;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author david
 */
public class Buttons {
    
    private static double scale = 24.0 / 960.0;
    
    public static Button getBackButton(){
        SVGPath icnBack = new SVGPath();
        icnBack.setContent("M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z");
        icnBack.getStyleClass().add("icon");
        
        icnBack.setScaleX(scale);
        icnBack.setScaleY(scale);
        
        Group icon_back_group = new Group(icnBack);
        StackPane icon_back = new StackPane(icon_back_group);
        icon_back.setPrefSize(24, 24);
        icon_back.setMaxSize(24, 24);
                
        Button btnBack = new Button("", icon_back);
        btnBack.setOnAction(e -> {
            SceneController.getInstance().backScene();
        });
        btnBack.setMinSize(24, 24);
        btnBack.setMaxSize(24, 24);
        icon_back.setPrefSize(24, 24);
        icon_back.setMaxSize(24, 24);
        return btnBack;
    }
    
}
