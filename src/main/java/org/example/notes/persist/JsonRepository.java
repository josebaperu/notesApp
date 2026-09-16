package org.example.notes.persist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path file() {
        return file;
    }

    public NotesData load() throws IOException {
        if (!Files.exists(file)) {
            return new NotesData();
        }
        try (var in = Files.newBufferedReader(file)) {
            NotesData data = mapper.readValue(in, NotesData.class);
            return data == null ? new NotesData() : data;
        }
    }

    public void save(NotesData data) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        mapper.writeValue(temp.toFile(), data);
        try {
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Path defaultPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "NotesApp", "notes.json");
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "NotesApp", "notes.json");
            }
            return Path.of(home, "AppData", "Roaming", "NotesApp", "notes.json");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "notes-app", "notes.json");
        }
        return Path.of(home, ".local", "share", "notes-app", "notes.json");
    }
}
