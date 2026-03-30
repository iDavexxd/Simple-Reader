package app.simplereader;

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
}
