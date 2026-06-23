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
    
    private StackPane placeholder(){
        SVGPath svg = new SVGPath();
        svg.setContent("");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    } // solo sirve para copiar
    
    public StackPane getZoomOutIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400ZM280-540v-80h200v80H280Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    } 
    
    public StackPane getZoomInIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Zm-40-60v-80h-80v-80h80v-80h80v80h80v80h-80v80h-80Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    
    public StackPane getConfigIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-13.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l119-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24 12L590-80H370Zm70-80h79l14-106q31-8 57.5-23.5T639-327l99 41 39-68-86-65q5-14 7-29.5t2-31.5q0-16-2-31.5t-7-29.5l86-65-39-68-99 42q-22-23-48.5-38.5T533-694l-13-106h-79l-14 106q-31 8-57.5 23.5T321-633l-99-41-39 68 86 64q-5 15-7 30t-2 32q0 16 2 31t7 30l-86 65 39 68 99-42q22 23 48.5 38.5T427-266l13 106Zm42-180q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Zm-2-140Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    public StackPane getExitFullScreenIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M240-120v-120H120v-80h200v200h-80Zm400 0v-200h200v80H720v120h-80ZM120-640v-80h120v-120h80v200H120Zm520 0v-200h80v120h120v80H640Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
    }
    
    public StackPane getFullScreenIcon(){
        SVGPath svg = new SVGPath();
        svg.setContent("M120-120v-200h80v120h120v80H120Zm520 0v-80h120v-120h80v200H640ZM120-640v-200h200v80H200v120h-80Zm640 0v-120H640v-80h200v200h-80Z");
        svg.getStyleClass().add("icon");
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        Group group = new Group(svg);
        StackPane sp = new StackPane(group);
        return sp;
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
