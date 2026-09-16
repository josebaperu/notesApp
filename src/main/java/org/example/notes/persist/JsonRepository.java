package org.example.notes.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.notes.model.NotesData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class JsonRepository {
    private final Path file;
    private final ObjectMapper mapper;

    public JsonRepository(Path file) {
        this.file = file;
        this.mapper = JsonFiles.mapper();
    }

    public Path file() {
        return file;
    }

    public NotesData load() throws IOException {
        if (!Files.exists(file)) {
            return new NotesData();
        }
        NotesData data = JsonFiles.read(file, NotesData.class, mapper);
        return data == null ? new NotesData() : data;
    }

    public void save(NotesData data) throws IOException {
        JsonFiles.writeAtomic(file, data, mapper);
    }

    public static Path defaultDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "NotesApp");
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "NotesApp");
            }
            return Path.of(home, "AppData", "Roaming", "NotesApp");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "notes-app");
        }
        return Path.of(home, ".local", "share", "notes-app");
    }

    public static Path defaultPath() {
        return defaultDirectory().resolve("notes.json");
    }
}
