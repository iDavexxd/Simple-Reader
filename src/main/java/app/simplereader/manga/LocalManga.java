package app.simplereader.manga;

import app.simplereader.Logger;
import app.simplereader.Sorter;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Manga;
import app.simplereader.manga.chapter.FolderChapter;
import app.simplereader.manga.chapter.ZipChapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author david
 */

public class LocalManga implements Manga{
    
    private String title;
    private String author;
    private String description;
    private transient File cover;
    private final transient File folder;
    
    private final HashMap<String, Integer> lastChapterPage = new HashMap<>();
    private Set<String> readedChapters = new HashSet<>();
    private List<String> tags;
    private transient List<Chapter> chapters;
    
    public LocalManga(File folder,String title, String author, String description){
        this.folder = folder;
        this.title = title;
        this.author = author;
        this.description = description;
        this.tags = new ArrayList<>();
        this.chapters = new ArrayList<>();
        //crear json
        openYml();
        loadChapters();
        loadCover();
    }
    
    private void loadChapters() {
        //filtrar carpetas
        File[] folders = folder.listFiles(file -> 
            file.isDirectory() || 
            file.getName().toLowerCase().endsWith(".cbz") ||
            (file.getName().toLowerCase().endsWith(".zip") && isValidZipCbz(file))
        );
        
        // Si no hay capitulos terminar to'
        if(folders == null || folders.length == 0) {
            Logger.warning("No hay capitulos en:" + title);
            return;
        }
        //sortea
        Arrays.sort(folders, (f1, f2) ->
            Sorter.compare(f1.getName(), f2.getName())
        );
        int i =0;
        // Lo que carga los capitulos a la lista
        for(File subfolder : folders){
            i++;
            ChapterType type = detectChapterType(subfolder);

            Chapter chapter = null;

            switch(type){
                case FOLDER -> chapter = new FolderChapter(this, subfolder);
                case ZIP, CBZ -> chapter = new ZipChapter(this,subfolder);
                default -> {
                    Logger.warning("Tipo desconocido: " + subfolder.getName());
                    continue;
                }
            }

            chapters.add(chapter);
            Logger.info("Loaded chapter: " + i);
        }
    }
    private Boolean isValidZipCbz(File file){
        // saber si el zip es válido o no
        try (ZipFile zipFile = new ZipFile(file)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private ChapterType detectChapterType(File file) {
        if (file.isDirectory()) return ChapterType.FOLDER;
        String name = file.getName().toLowerCase();
        if (name.endsWith(".cbz")) return ChapterType.CBZ;
        if (name.endsWith(".zip") && isValidZipCbz(file)) return ChapterType.ZIP;
        return ChapterType.UNKNOWN;
    }
    
    private void openYml(){
        File yamlFile = new File(folder, "info.yaml");
        if (!yamlFile.exists()) {
            // si no existe lo crea
            createYml(yamlFile);
        } else {
            // si existe lo lee
            readYaml(yamlFile);
        }   
    }
    
    private void createYml(File ymlfile) {
        MangaData data = new MangaData();
        data.title = this.title;
        data.author = this.author;
        data.description = this.description;
        data.tags = this.tags;
        
        org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);

        org.yaml.snakeyaml.representer.Representer representer = new org.yaml.snakeyaml.representer.Representer(options);
        representer.addClassTag(MangaData.class, org.yaml.snakeyaml.nodes.Tag.MAP); // ← esta línea

        Yaml yaml = new Yaml(representer, options);
        try(FileWriter writer = new FileWriter(ymlfile))
        {
            yaml.dump(data,writer);
            Logger.info(this.getTitle()+" - Loaded YAML: "+ymlfile.getPath());
        } catch(IOException e){
            Logger.error(this.getTitle()+" - Error al cargar YAML.");
        }
    }
    private void readYaml(File ymlfile){
        Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.Constructor(
            MangaData.class,
            new org.yaml.snakeyaml.LoaderOptions()
        ));

        try
        {
            java.io.FileReader reader = new java.io.FileReader(ymlfile);
            MangaData data = yaml.loadAs(reader, MangaData.class);
            this.author = data.author;
            this.title = data.title;
            this.description = data.description != null
            ? String.join("\n", data.description)
            : "";
            this.tags = data.tags != null ? data.tags : new ArrayList<>();
            Logger.info("YAML leído: " + title);
            reader.close();
        } 
        catch (Exception e)
        {
            Logger.error("No se pudo leer info.yaml: " + e.getMessage());
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
    // No uso esto aun, lo dejo por si sirve
    private void setCover(File cover) {
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
    
    @Override
    public void saveData(){
        //Guardar datos en un json
    }
    @Override
    public Set<String> getReadedChapters(){
       return this.readedChapters; 
    }
    
    @Override
    public HashMap<String, Integer> getChapterLastPage(){
        return lastChapterPage;
    }
    
    @Override
    public String getCover() {
        if(cover == null) return null;
        return cover.toURI().toString();
    }
    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String Title) {
        this.title = Title;
    }
    @Override
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String Author) {
        this.author = Author;
    }
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String Description) {
        this.description = Description;
    }
    @Override
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    @Override
    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }
        
}
