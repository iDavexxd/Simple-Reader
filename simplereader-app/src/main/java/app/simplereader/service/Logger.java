package app.simplereader.service;

import javafx.scene.control.Alert;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author david
 */
public class Logger {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static String getTime() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    public static void info(String msg) {
        System.out.println("[" + getTime() + "] [INFO]!!!! - " + msg);
    }
    public static void warning(String msg) {
        System.out.println("[" + getTime() + "] [WARNING]!!!! - " + msg);
    }
    public static void error(String msg) {
        System.out.println("[" + getTime() + "] [ERROR]!!!! - " + msg);
    }
    
    public static void noPagesAlert(String chapterName){
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Capítulo vacío");
        alerta.setHeaderText(null);
        alerta.setContentText("El capítulo '" + chapterName + "' no contiene imágenes válidas.");
        alerta.showAndWait();
    }
}
