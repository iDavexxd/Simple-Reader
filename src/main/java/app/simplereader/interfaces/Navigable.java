package app.simplereader.interfaces;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author david
 */
public interface Navigable {
    Scene getScene();
    String getName();
    String getParentName();
    
}