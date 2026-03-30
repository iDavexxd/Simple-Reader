package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author david
 */
public class ScnMainMenu implements Navigable{
    private final Navegador nav;    
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
    }
    @Override
    public Scene getScene(){
        Button btnReader = new Button("Leer");
        btnReader.setOnAction(e -> {
            nav.goTo(new ScnReader(nav));
        });
        BorderPane layout = new BorderPane();
        layout.setBottom(btnReader);
        return new Scene(layout,AppConfig.WIDTH,AppConfig.HEIGHT);
    }
    @Override
    public String getName(){
        return "Menu";
    }
}
