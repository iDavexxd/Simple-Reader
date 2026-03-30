package app.simplereader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
                System.err.println("Error leyendo config.json");
            }
        }
        AppConfig defaultConf = new AppConfig();
        defaultConf.save(); // Crea el archivo si no existe
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
