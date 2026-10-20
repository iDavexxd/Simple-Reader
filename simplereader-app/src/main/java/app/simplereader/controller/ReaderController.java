package app.simplereader.controller;

import app.simplereader.service.Logger;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.service.Cache;
import app.simplereader.service.ImageManager;
import app.simplereader.service.PageLoader;
import app.simplereader.views.ScnReader;
import java.util.List;

import javafx.scene.control.ComboBox;


/**
 *
 * @author david
 */
public class ReaderController {
    private ScnReader view;
    
    private static ReaderController instance;
    private SceneController nav = SceneController.getInstance();
    private final SourceManager sourceManager = SourceManager.getInstance();
    private final PageLoader pageLoader = PageLoader.getInstance();
    
    private Manga manga;
    private Chapter chapter;
    private int chapterIndex;
    private int currentPageIndex;
    
    private boolean menuVisible = true;
    private boolean inZoom = false;
    private boolean updatingUI = false;
    private boolean programmaticNav = false;
    private boolean startAtLastPage = false;

    private ComboBox<Integer> paginaComboBox;
    private ComboBox<Chapter> chapterComboBox;
    private javafx.scene.control.Label chapterNameLabel;
    
    public ReaderController(ScnReader view){
        this.view = view;
    }
    
    public static void doInstance(ScnReader view){
        instance = new ReaderController(view);
    }
    
    public static ReaderController getInstance(){
        return instance;
    }
    
    public void init(Manga manga, Chapter chapter, int chapterIndex){
        this.manga = manga;
        this.chapter = chapter;
        this.chapterIndex = chapterIndex;
        this.currentPageIndex = chapter.isReaded() ? 0 : chapter.getLastRead();
    }
    
    public void setUIComponents(ComboBox<Integer> pagina, ComboBox<Chapter> caps, javafx.scene.control.Label chnameLabel){
        this.paginaComboBox = pagina;
        this.chapterComboBox = caps;
        this.chapterNameLabel = chnameLabel;
    }
       
    
    public void loadChapter(Chapter chapter, int index){
        
        pageLoader.clear();
        pageLoader.setSource(sourceManager.getSource(manga.getSourceID()));
        pageLoader.setImageUrlList(sourceManager.getSource(manga.getSourceID()).getPages(manga.getMangaID(), chapter.getChapterID()));
        
        view.setImageViewImage(null); 
        
        this.chapter = chapter;
        this.chapterIndex = index;
        
        if (startAtLastPage) {
            this.currentPageIndex = Math.max(0, chapter.getPageCount() - 1);
            startAtLastPage = false;
        } else {
            this.currentPageIndex = chapter.isReaded() ? 0 : chapter.getLastRead();
        }
        
        view.setChapter(chapter);
        nav.getStage().setTitle(view.getName());
        
        if (paginaComboBox != null) {
            paginaComboBox.getItems().clear();
            for (int i = 1; i <= chapter.getPageCount(); i++) {
                paginaComboBox.getItems().add(i);
            }
            updatingUI = true;
            paginaComboBox.setValue(currentPageIndex + 1);
            updatingUI = false;
        }
        
        if (chapterComboBox != null) {
            updatingUI = true;
            chapterComboBox.getItems().setAll(manga.getChapters());
            chapterComboBox.setValue(chapter);
            updatingUI = false;
        }
        
        if (chapterNameLabel != null) {
            chapterNameLabel.setText(chapter.getTitle());
        }
        
        if (totalPages() > 0) {
            if (paginaComboBox != null) paginaComboBox.setDisable(totalPages() <= 1);
            loadCurrentImage();
        } else {
            if (paginaComboBox != null) paginaComboBox.setDisable(true);
            view.showNoPagesOverlay();
        }
        
        view.doUpdatePageLabel();
    }
    
    public int totalPages() {
        return chapter != null ? chapter.getPageCount() : 0;
    }
    
    public void nextPage() {
        if (currentPageIndex < totalPages() - 1) {
            view.setImageViewImage(null);
            goToPage(currentPageIndex + 1);
        } else {
            nextChapter();
        }   
    }
    
    public void changeToChapter(Chapter next, int nextIndex) {
        app.simplereader.controller.MangaMenuController.getInstance().openChapter(next);
    }
    
    public void previousPage() {
        if (currentPageIndex > 0){
            view.setImageViewImage(null);
            goToPage(currentPageIndex - 1);
        } else {
            previousChapter();
        }   
    }
    
    public void goToPage(int index) {
        if (index < 0 || index >= totalPages()) return;
        
        if (index == totalPages() - 1){
            chapter.markAsReaded();
        }
        
        currentPageIndex = index;
        if (!chapter.isReaded()) {
            chapter.setLastRead(currentPageIndex);
        }
        
        loadCurrentImage();
        if (paginaComboBox != null) {
            updatingUI = true;
            paginaComboBox.setValue(currentPageIndex + 1);
            view.doUpdatePageLabel();
            updatingUI = false;
        }
    }
    
    public void nextChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex < chapters.size() - 1) {
            int nextIndex = chapterIndex + 1;
            Chapter next = chapters.get(nextIndex);
            chapter.markAsReaded();
            
            app.simplereader.controller.MangaMenuController.getInstance().openChapter(next);
        }
    }
    
    public void previousChapter() {
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return;
        
        if (chapterIndex > 0) {
            int prevIndex = chapterIndex - 1;
            Chapter prev = chapters.get(prevIndex);
            
            startAtLastPage = true;
            app.simplereader.controller.MangaMenuController.getInstance().openChapter(prev);
        }
    }
    
    public void reloadCurrentImage() {
        view.setImageViewImage(null);
        Cache.getInstance().getPagesLRU().invalidate(chapter.getPage(currentPageIndex));
        loadCurrentImage();
    }
    
    
    public void loadCurrentImage() {
        view.setImageViewImage(null); // Muestra el overlay de carga

        pageLoader.getImage(currentPageIndex, img -> {
            if (img != null && !img.isError()) {
                view.setImageViewImage(img);
                view.fitImageToScreen();
                resetZoom();
            } else {
                view.showErrorOverlay();
            }
        });
    }
    
        
    public void resetZoom() {
        if (view != null && view.getVisor() != null) {
            view.getVisor().setScaleX(1.0);
            view.getVisor().setScaleY(1.0);
        }
        inZoom = false;
    }
    
    public void setInZoom(boolean zoom) {
        this.inZoom = zoom;
    }
    
    public boolean isInZoom() {
        return inZoom;
    }
    
    public boolean isMenuVisible() {
        return menuVisible;
    }
    
    public void hideMenu() {
        menuVisible = false;
        if (view != null) view.setMenuVisible(false);
    }
    
    public void showMenu() {
        menuVisible = true;
        if (view != null) view.setMenuVisible(true);
    }
    
    public boolean isUpdatingUI() {
        return updatingUI;
    }
    
    public boolean isProgrammaticNav() {
        return programmaticNav;
    }
    
    public void cleanupResources() {
        pageLoader.clear();
    }
    
    public void saveAndCleanup() {
        pageLoader.clear();
    }
    
    public Manga getManga() {
        return manga;
    }
    
    public Chapter getChapter() {
        return chapter;
    }
    
    public int getChapterIndex() {
        return chapterIndex;
    }
    
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }
}