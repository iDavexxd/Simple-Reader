package app.simplereader.model;

import app.simplereader.controller.Logger;
import app.simplereader.controller.Sorter;
import app.simplereader.repository.MangaSource;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author david
 */
public class LocalSource implements MangaSource {
    
    private static final String BASE_PATH = AppConfig.LOCAL_MANGA_FOLDER
            +File.separator
            +"local";
    
    public LocalSource(){
        
    }
    
    @Override
    public String getID() {
        return "local";
    }

    @Override
    public String getName() {
        return "Local";
    }

    @Override
    public String getCoverURL(String mangaID) {
        File mangaFolder = new File(BASE_PATH + File.separator + mangaID);

        if (!mangaFolder.exists()) return null;

        File cover = new File(mangaFolder, "cover.jpg");
        if (!cover.exists()) cover = new File(mangaFolder, "cover.png");
        if (!cover.exists()) cover = new File(mangaFolder, "cover.jpeg");

        if (cover.exists()) {
            return cover.toURI().toString();
        }else{
            return null;
        }        
    }

    @Override
    public List<Manga> searchManga(String query) {
        File baseFolder = new File(BASE_PATH);
        
        if(!baseFolder.exists()) return new ArrayList<>();
        
        File[] items = baseFolder.listFiles();
        if(items == null) return new ArrayList<>();
        
        List<Manga> resultados = new ArrayList<>();
        
        for(File item : items){
            if(item.isDirectory()){
                String name = item.getName();
                if (name.toLowerCase().contains(query.toLowerCase())) {

                    Manga manga = new Manga(name, this.getID());
                    manga.setTitle(name);

                    File cover = new File(item, "cover.jpg");
                    if (!cover.exists()) cover = new File(item, "cover.png");
                    if (!cover.exists()) cover = new File(item, "cover.jpeg");
                    if (cover.exists()) {
                        manga.setCoverURL(cover.toURI().toString());
                    }

                    resultados.add(manga);
                }
            }
        }
        return resultados;
    }

    @Override
    public List<Chapter> getChapters(String mangaID) {
        File folder = new File(BASE_PATH+File.separator+mangaID);
        if(!folder.exists()) return new ArrayList<>();
        File[] items = folder.listFiles();
        if(items == null) return new ArrayList<>();
        
        List<File> validChapters = new ArrayList<>();
        for(File item : items){
            String name = item.getName();
            if(item.isDirectory() || name.endsWith(".cbz") || name.endsWith(".zip")){
                validChapters.add(item);
            }
        }
        validChapters.sort((a, b) -> Sorter.compare(a.getName(), b.getName()));
        
        List<Chapter> chapters = new ArrayList<>();
        for (File item : validChapters) {
            Chapter ch = new Chapter(item.getName(), null);
            ch.setTitle(item.getName());
            try {
                java.nio.file.attribute.BasicFileAttributes attr = java.nio.file.Files.readAttributes(
                        item.toPath(), java.nio.file.attribute.BasicFileAttributes.class);
                ch.setDate(attr.creationTime().toString().substring(0, 10));
            } catch (Exception e) {
                ch.setDate("");
            }
            // Si es un archivo zip/cbz, intentamos leer el XML
            String name = item.getName().toLowerCase();
            if (name.endsWith(".cbz") || name.endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(item)) {
                    // Buscamos el archivo ComicInfo.xml dentro del zip
                    ZipEntry xmlEntry = zip.getEntry("ComicInfo.xml");

                    if (xmlEntry != null) {
                        InputStream is = zip.getInputStream(xmlEntry);

                        // Parseamos el XML
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(is);

                        // Leemos el Título
                        NodeList titles = doc.getElementsByTagName("Title");
                        if (titles.getLength() > 0 && !titles.item(0).getTextContent().isEmpty()) {
                            ch.setTitle(titles.item(0).getTextContent());
                        }

                        // Leemos el Número
                        NodeList numbers = doc.getElementsByTagName("Number");
                        if (numbers.getLength() > 0) {
                            ch.setNumber(numbers.item(0).getTextContent());
                        }
                    }
                } catch (Exception e) {
                    // Si falla (no hay xml, error de formato), usamos el nombre del archivo
                }
            }

            chapters.add(ch);
        }
        return chapters;
    }

    @Override
    public List<String> getPages(String mangaID, String chapterID) {
        File chapterFile = new File(BASE_PATH + File.separator + mangaID + File.separator + chapterID);
        List<String> pages = new ArrayList<>();
        
        if (chapterFile.isDirectory()) {
            File[] images = chapterFile.listFiles(f -> 
                f.getName().toLowerCase().endsWith(".jpg") || 
                f.getName().toLowerCase().endsWith(".png") || 
                f.getName().toLowerCase().endsWith(".jpeg")
            );
            
            if (images != null) {
                Arrays.sort(images, (a, b) -> Sorter.compare(a.getName(), b.getName()));
                for (File img : images) {
                    pages.add(img.toURI().toString());
                }
            }
        } else if (chapterFile.getName().toLowerCase().endsWith(".cbz") || chapterFile.getName().toLowerCase().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(chapterFile)) {
                List<String> entries = new ArrayList<>();
                for (ZipEntry e : zip.stream().toList()) {
                    String name = e.getName().toLowerCase();
                    if (!e.isDirectory() && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"))) {
                        entries.add(e.getName());
                    }
                }
                entries.sort(String::compareToIgnoreCase);
                for (String entry : entries) {
                    pages.add("jar:" + chapterFile.toURI() + "!/" + entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return pages;
    }

    @Override
    public void fetchMangaData(Manga manga) {
        File folder = new File(BASE_PATH + File.separator + manga.getMangaID());
        File yamlFile = new File(folder, "info.yaml");
        
        if (yamlFile.exists()) {
            try (FileReader reader = new FileReader(yamlFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(reader);
                
                if (data != null) {
                    if (data.containsKey("title")) manga.setTitle((String) data.get("title"));
                    if (data.containsKey("author")) manga.setAuthor((String) data.get("author"));
                    if (data.containsKey("description")) manga.setDescription((String) data.get("description"));
                    if (data.containsKey("tags")) manga.setTags((List<String>) data.get("tags"));
                }
            } catch (IOException e) {
                Logger.error("Error reading info.yaml: " + e.getMessage());
            }
        } else {
            // Si no existe, lo creamos con valores por defecto
            try {
                Map<String, Object> data = new HashMap<>();
                // Usamos el título que ya tiene (nombre de carpeta) o el ID
                data.put("title", manga.getTitle() != null ? manga.getTitle() : manga.getMangaID());
                data.put("author", "");
                data.put("description", "");
                data.put("tags", new ArrayList<>());
                
                Yaml yaml = new Yaml();
                try (FileWriter writer = new FileWriter(yamlFile)) {
                    yaml.dump(data, writer);
                }
                Logger.info("Created info.yaml for " + manga.getTitle());
            } catch (IOException e) {
                Logger.error("Error creating info.yaml: " + e.getMessage());
            }
        }
    }
    
}
