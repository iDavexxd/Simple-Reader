package app.simplereader.service;

import app.simplereader.repository.AppLogger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Delegating Logger that uses an AppLogger provider.
 * Allows the app to inject custom logging implementations (e.g. file, database, UI) 
 * without breaking existing extensions.
 *
 * @author david
 */
public class Logger {
    
    // Default implementation logs to console
    private static AppLogger provider = new AppLogger() {
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
        private String getTime() { return LocalTime.now().format(TIME_FORMATTER); }
        
        @Override
        public void info(String msg) { System.out.println("[" + getTime() + "] [INFO]!!!! - " + msg); }
        @Override
        public void warning(String msg) { System.out.println("[" + getTime() + "] [WARNING]!!!! - " + msg); }
        @Override
        public void error(String msg) { System.out.println("[" + getTime() + "] [ERROR]!!!! - " + msg); }
    };

    /**
     * Sets a custom logger provider.
     */
    public static void setProvider(AppLogger newProvider) {
        if (newProvider != null) {
            provider = newProvider;
        }
    }

    public static void info(String msg) {
        provider.info(msg);
    }
    
    public static void warning(String msg) {
        provider.warning(msg);
    }
    
    public static void error(String msg) {
        provider.error(msg);
    }
}
