package app.simplereader.controller;

import app.simplereader.model.Category;
import app.simplereader.views.ScnConfig;
import java.util.List;

/**
 *
 * @author david
 */
public class ConfigSceneController {
    private final ScnConfig scene;
    private static ConfigSceneController instance;
    
    private final LibraryController lib = LibraryController.getInstance();
    
    public ConfigSceneController(ScnConfig scene){
        this.scene = scene;
    }
    
    public static void doInstance(ScnConfig scene){
        instance = new ConfigSceneController(scene);
    }
    
    public static ConfigSceneController getInstance(){
        return instance;
    }
    
    public List<Category> getCategoryList(){
        return lib.getAllCategories();
    }
    
    public void doCreateCategory(String name){
        lib.addCategory(name);
        reloadCategories();
    }
    
    public void doRemoveCategory(String name){
        lib.removeCategory(name);
        reloadCategories();
    }
    
    public void doUpdateListView(){
        
    }
    
    private void reloadCategories(){
       MainMenuController mainMenu = MainMenuController.getInstance();
       if (mainMenu != null) mainMenu.reloadCategoryTabs();
       MangaMenuController mangaMenu = MangaMenuController.getInstance();
       if (mangaMenu != null) mangaMenu.doReloadCategoryButtons();
    }
}
