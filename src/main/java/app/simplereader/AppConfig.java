package app.simplereader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

/**
 *
 * @author david
 */
public class AppConfig {
    public int WIDTH = 1280;
    public int HEIGHT = 720;
    public String APP_TITLE = "Simple Reader";
    public Boolean readerfullscreen = false;
    
    public static AppConfig instance;
    private static final String CONFIG_FILE = System.getProperty("user.home") 
                                            + File.separator + "Documents" 
                                            + File.separator + "SimpleReader" 
                                            + File.separator + "config.json";
    
    public static AppConfig get(){
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    private static AppConfig load() {
        File file = new File(CONFIG_FILE);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs(); 
        }
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return new Gson().fromJson(reader, AppConfig.class);
            } catch (IOException e) {
                Logger.error("Error leyendo config.json");
            }
        }

        // Si no existe el archivo, calculamos según el monitor
        AppConfig defaultConf = new AppConfig();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        // 70% del ancho y alto disponible
        double targetWidth = bounds.getWidth() * 0.7;
        double targetHeight = bounds.getHeight() * 0.7;

        // Solo aplicamos el mínimo de 1280 si la pantalla es lo suficientemente grande
        if (bounds.getWidth() > 1280) {
            defaultConf.WIDTH = (int) Math.max(targetWidth, 1280);
        } else {
            defaultConf.WIDTH = (int) (bounds.getWidth() * 0.9); // 90% en pantallas pequeñas
        }

        if (bounds.getHeight() > 720) {
            defaultConf.HEIGHT = (int) Math.max(targetHeight, 720);
        } else {
            defaultConf.HEIGHT = (int) (bounds.getHeight() * 0.9);
        }

        defaultConf.save();
        return defaultConf;
    }
    
    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Error guardando config.json");
        }
    }
    
    
}
