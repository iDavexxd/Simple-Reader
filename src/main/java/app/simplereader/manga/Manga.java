package app.simplereader.manga;

import app.simplereader.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author david
 */
public class Manga {
    
    private String title;
    private String author;
    private String description;
    private transient File cover;
    private transient File folder;

    
    private List<String> tags;
    private transient List<Chapter> chapters;
    
    public Manga(File folder,String title, String author, String description){
        this.folder = folder;
        this.title = title;
        this.author = author;
        this.description = description;
        this.tags = new ArrayList<>();
        this.chapters = new ArrayList<>();
        //crear json
        openJson();
        loadChapters();
        loadCover();
    }
    
    private void loadChapters() {
        //filtrar carpetas
        File[] folders = folder.listFiles(File::isDirectory);
        if(folders == null || folders.length == 0) {
            Logger.warning("No hay capitulos en:" + title);
            return;
        }
        Arrays.sort(folders);
        for(File subfolder:folders){
            chapters.add(new Chapter(subfolder));
        }
    }
    
    private void openJson(){
        File jsonfile = new File(folder, "info.json");
        if(!jsonfile.exists()){
            createJson(jsonfile);
        }else{
            readJson(jsonfile);
        }       
    }
    
    private void createJson(File jsonfile) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this); 
    
        try {
            FileWriter writer = new FileWriter(jsonfile);
            writer.write(json);
            writer.close();
            Logger.info("manga.json creado: " + jsonfile.getPath());
        } catch (IOException e) {
            Logger.error("No se pudo crear manga.json: " + e.getMessage());
        }        
    }
    private void readJson(File jsonfile){
        try
        {
            Gson gson = new Gson();
            java.io.FileReader reader = new java.io.FileReader(jsonfile);
            Manga datos = gson.fromJson(reader, Manga.class);
            reader.close();

            // sobrescribir los datos con los del JSON
            this.title = datos.title;
            this.author = datos.author;
            this.description = datos.description;
            this.tags = datos.tags != null ? datos.tags : new ArrayList<>();

            Logger.info("JSON leído: " + title);
        } 
        catch (IOException e)
        {
            Logger.error("No se pudo leer info.json: " + e.getMessage());
        }    
    }
    public File getFolder() {
        return folder;
    }
    
    private void loadCover(){
        File cover = new File(folder, "cover.jpg");
    
        if (!cover.exists()) {
            cover = new File(folder, "cover.png");
        }

        if (!cover.exists()) {
            cover = new File(folder, "cover.jpeg");
        }

        if (!cover.exists()) {
            Logger.warning("No hay cover en: " + title);
            return;
        }

        this.cover = cover;
        Logger.info(this.getTitle()+" - Loaded Cover: " + cover.getName());
    }
    
    public void setCover(File cover) {
        if (cover == null) {
        Logger.warning("Cover es null");
        return;
        }
    
        String nombre = cover.getName().toLowerCase();
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") || nombre.endsWith(".png")) {
            this.cover = cover;
        } else {
            Logger.warning("El archivo no es una imagen válida: " + cover.getName());
        }

    }
    
    public File getCover() {
        return cover;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String Title) {
        this.title = Title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String Author) {
        this.author = Author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String Description) {
        this.description = Description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }
        
}
