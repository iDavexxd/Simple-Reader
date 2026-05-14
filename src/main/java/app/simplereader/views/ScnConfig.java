package app.simplereader.views;

import app.simplereader.views.ScnMainMenu;
import app.simplereader.model.AppConfig;
import app.simplereader.controller.SceneController;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import app.simplereader.repository.AppScene;

/**
 *
 * @author david
 */
public class ScnConfig implements AppScene {
    
    private SceneController nav;
    
    public ScnConfig(SceneController nav){
        this.nav = nav;
    }
    
//    private void saveConfig(){
//        // Sincronizar UI -> Objeto Config
//            AppConfig.get().READING_DIR = cbDirection.getValue().contains("LTR") ? "LTR" : "RTL";
//            AppConfig.get().SCALING_MODE = cbScaling.getValue().contains("ancho") ? "FIT_WIDTH" : "FIT_HEIGHT";
//            
//            AppConfig.get().save(); // Guardar a JSON
//            
//    }
    @Override
    public Scene getScene() {
        
        
        // Menu lateral
        SideMenu lateralmenu = new SideMenu();        
        
        // botones
        SVGPath svgBack = new SVGPath();
        svgBack.setContent("M640-80 240-480l400-400 71 71-329 329 329 329-71 71Z");
        svgBack.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        svgBack.setScaleX(scale);
        svgBack.setScaleY(scale);
        
        Group gpBack = new Group(svgBack);
        StackPane back_container = new StackPane(gpBack);
        back_container.setPrefSize(24, 24);
        back_container.setMinSize(24, 24);

        Button btnBackToMenu = new Button("",back_container);
        btnBackToMenu.setOnAction(e ->{
            nav.goTo(new ScnMainMenu(this.nav));
        });
        btnBackToMenu.setPrefSize(24, 24);
        btnBackToMenu.setMinSize(24, 24);
        
        
        lateralmenu.addTop(btnBackToMenu);
        
        ScrollPane Scroll = new ScrollPane();
        Scroll.setFitToWidth(true);

        BorderPane root = new BorderPane();
        root.setCenter(Scroll);
        root.setLeft(lateralmenu.getPane());
        root.getStyleClass().add("conf-root");
        
        Scene scene = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        return scene;
        
        
    }

    @Override
    public String getName() {
        return "Configuration";
    }

    @Override
    public String getParentName() {
        return "Configuration";  
    }
    
}
