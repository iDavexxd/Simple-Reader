package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.scenes.others.SideMenu;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class ScnConfig implements Navigable {
    
    private Navegador nav;
    
    public ScnConfig(Navegador nav){
        this.nav = nav;
    }

    @Override
    public Scene getScene() {
        VBox container = new VBox(15); // Espacio de 15px entre elementos
        container.setPadding(new Insets(20));

        // --- SECCIÓN: LECTOR ---
        Label lblLector = new Label("Configuración del Lector");
        lblLector.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ComboBox<String> cbDirection = new ComboBox<>();
        cbDirection.getItems().addAll("Izquierda a derecha (LTR)", "Derecha a izquierda (RTL)");
        cbDirection.setValue(AppConfig.get().READING_DIR.equals("LTR") ? 
                             "Izquierda a derecha (LTR)" : "Derecha a izquierda (RTL)");

        ComboBox<String> cbScaling = new ComboBox<>();
        cbScaling.getItems().addAll("Ajustar al ancho", "Ajustar al alto");
        cbScaling.setValue(AppConfig.get().SCALING_MODE.equals("FIT_WIDTH") ? 
                           "Ajustar al ancho" : "Ajustar al alto");

        // --- SECCIÓN: ACERCA DE ---
        Separator sep = new Separator();
        Label lblAbout = new Label("Acerca de");
        lblAbout.setStyle("-fx-font-weight: bold;");
        Label lblInfo = new Label(AppConfig.get().APP_TITLE + "\nVersión: " + AppConfig.get().VERSION + "\nDesarrollado por: David");

        // --- BOTÓN GUARDAR ---
        Button btnSave = new Button("Guardar Cambios");
        btnSave.setOnAction(e -> {
            // Sincronizar UI -> Objeto Config
            AppConfig.get().READING_DIR = cbDirection.getValue().contains("LTR") ? "LTR" : "RTL";
            AppConfig.get().SCALING_MODE = cbScaling.getValue().contains("ancho") ? "FIT_WIDTH" : "FIT_HEIGHT";
            
            AppConfig.get().save(); // Guardar a JSON
            
            // Opcional: Mostrar una alerta de éxito
            new Alert(Alert.AlertType.INFORMATION, "Configuración guardada").show();
        });

        // Agregar todo al container
        container.getChildren().addAll(lblLector, new Label("Dirección:"), cbDirection, 
                                     new Label("Escalado:"), cbScaling, 
                                     sep, lblAbout, lblInfo, btnSave);
        
        SideMenu lateralmenu = new SideMenu();        

        ScrollPane Scroll = new ScrollPane(container);
        Scroll.setFitToWidth(true); // Para que el VBox use todo el ancho

        BorderPane root = new BorderPane();
        root.setCenter(Scroll);
        root.setLeft(lateralmenu.getPane());
        Scene scene = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        
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
