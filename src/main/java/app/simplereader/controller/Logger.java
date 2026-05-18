package app.simplereader.controller;

import javafx.scene.control.Alert;

/**
 *
 * @author david
 */
public class Logger {
    public static void info(String msg) {
        System.out.println("[INFO]!!!! - " + msg);
    }
    public static void warning(String msg) {
        System.out.println("[WARNING]!!!! - " + msg);
    }
    public static void error(String msg) {
        System.out.println("[ERROR]!!!! - " + msg);
    }
    
    public static void noPagesAlert(String chapterName){
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Capítulo vacío");
        alerta.setHeaderText(null);
        alerta.setContentText("El capítulo '" + chapterName + "' no contiene imágenes válidas.");
        alerta.showAndWait();
    }
}
