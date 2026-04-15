/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package app.simplereader.interfaces;

import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public interface Chapter {
    int getPageCount();
    Image getPage(int index);
    boolean hasPages();
    String getName();
}
