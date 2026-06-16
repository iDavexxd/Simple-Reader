package app.simplereader.views.components;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author david
 */
public class SvgIcons {
    
    private static SvgIcons instance;
    
    private final double scale = 24.0 / 960.0;
    
    public static SvgIcons get(){
        if (instance == null) instance = new SvgIcons();
        return instance;
    }
    
    public StackPane getCloseIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    
    public StackPane getAddIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M440-120v-320H120v-80h320v-320h80v320h320v80H520v320h-80Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    
    public StackPane getVisibleIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M607.5-372.5Q660-425 660-500t-52.5-127.5Q555-680 480-680t-127.5 52.5Q300-575 300-500t52.5 127.5Q405-320 480-320t127.5-52.5Zm-204-51Q372-455 372-500t31.5-76.5Q435-608 480-608t76.5 31.5Q588-545 588-500t-31.5 76.5Q525-392 480-392t-76.5-31.5ZM214-281.5Q94-363 40-500q54-137 174-218.5T480-800q146 0 266 81.5T920-500q-54 137-174 218.5T480-200q-146 0-266-81.5ZM480-500Zm207.5 160.5Q782-399 832-500q-50-101-144.5-160.5T480-720q-113 0-207.5 59.5T128-500q50 101 144.5 160.5T480-280q113 0 207.5-59.5Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    
    public StackPane getRemoveIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M200-440v-80h560v80H200Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
}
