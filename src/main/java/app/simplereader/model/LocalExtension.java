package app.simplereader.model;

import app.simplereader.repository.AppExtension;
import app.simplereader.repository.MangaSource;
import java.util.List;

/**
 * Extensión que envuelve la lectura de mangas locales.
 */
public class LocalExtension extends AppExtension {

    @Override
    public String getName() {
        return "Local";
    }

    @Override
    public List<MangaSource> getSources() {
        // Devuelve el LocalSource que ya tenías programado
        return List.of(new LocalSource());
    }
}
