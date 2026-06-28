package app.simplereader.repository;

import javafx.scene.Parent;

/**
 *
 * @author david
 */
public interface AppScene {
    Parent getScene();
    String getName();
    String getParentName();
    
}