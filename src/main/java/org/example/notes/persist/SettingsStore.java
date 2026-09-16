package org.example.notes.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.notes.model.AppSettings;
import org.example.notes.model.ThemePreference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class SettingsStore {
    private final Path file;
    private final ObjectMapper mapper;
    private AppSettings settings = new AppSettings();

    public SettingsStore(Path file) {
        this.file = file;
        this.mapper = JsonFiles.mapper();
    }

    public Path file() {
        return file;
    }

    public AppSettings get() {
        return settings;
    }

    public void load() {
        try {
            if (Files.exists(file)) {
                AppSettings loaded = JsonFiles.read(file, AppSettings.class, mapper);
                settings = loaded == null ? new AppSettings() : loaded;
            } else {
                settings = new AppSettings();
                save();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read settings file", e);
        }
    }

    public void save() {
        try {
            JsonFiles.writeAtomic(file, settings, mapper);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write settings file", e);
        }
    }

    public void update(Consumer<AppSettings> mutator) {
        mutator.accept(settings);
        save();
    }

    public boolean dark(boolean systemDark) {
        return switch (settings.getTheme()) {
            case DARK -> true;
            case LIGHT -> false;
            case SYSTEM -> systemDark;
        };
    }

    public static Path defaultPath() {
        return JsonRepository.defaultDirectory().resolve("settings.json");
    }

    public static ThemePreference fromDark(boolean dark) {
        return dark ? ThemePreference.DARK : ThemePreference.LIGHT;
    }
}
