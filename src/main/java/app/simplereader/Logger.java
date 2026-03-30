package app.simplereader;

import app.simplereader.manga.Chapter;
import javafx.scene.control.Alert;

/**
 *
 * @author david
 */
public class Logger {
    public static void info(String msg) {
        System.out.println("[INFO]!!!! - "+msg);
    }
    public static void warning(String msg) {
        System.out.println("[WARNING]!!!! - "+msg);
    }
    public static void error(String msg) {
        System.out.println("[ERROR]!!!! - "+msg);
    }
    
    public static void noPagesAlert(Chapter selChapter){
        // Creamos la alerta de tipo ADVERTENCIA
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Capítulo vacío");
        alerta.setHeaderText(null); // Esto quita el encabezado gris si no lo necesitas
        alerta.setContentText("El capítulo '" + selChapter.getChName() + "' no contiene imágenes válidas.");
        alerta.showAndWait(); // Muestra la ventana y detiene el código hasta que se cierre
    }
}
