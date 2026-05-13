package app.simplereader.repository;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author david
 */
public interface AppScene {
    Scene getScene();
    String getName();
    String getParentName();
    
}