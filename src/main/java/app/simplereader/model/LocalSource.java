/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.simplereader.model;

import app.simplereader.controller.CategoryController;
import app.simplereader.controller.Logger;
import app.simplereader.repository.Manga;
import app.simplereader.repository.MangaSource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author david
 */
public class LocalSource implements MangaSource {
    
    private CategoryController manager;
    
    public LocalSource(CategoryController manager){
        // no sé que poner en el constructor o si necestia algo
        this.manager = manager;
    }
    
    
    @Override
    public List<Manga> loadMangas(){
        String home = System.getProperty("user.home");
        File mainfolder = new File(home + "/Documents/SimpleReader/mangas/local");
        if (!mainfolder.exists()) {
            mainfolder.mkdirs();
            Logger.info("Carpeta creada: " + mainfolder.getPath());
        }

        File[] mangas = mainfolder.listFiles((dir, nombre) -> 
            new File(dir, nombre).isDirectory()
        );
        if (mangas == null || mangas.length == 0) {
            return new ArrayList<>();
        }

        Arrays.sort(mangas);
        List<Manga> lista = new ArrayList<>();
        for (File subcarpeta : mangas) {
            LocalManga manga = new LocalManga(subcarpeta, subcarpeta.getName(), "", "");
            if (manga.getCategory() == null) manga.setCategory("Default");

            Category category = manager.getCategories().get(manga.getCategory());
            if (category == null) {                              // ← agrega esto
                manga.setCategory("Default");
                category = manager.getCategories().get("Default");
            }

            category.addManga(manga);
            lista.add(manga);
        }
        return lista;
    }
 
}
