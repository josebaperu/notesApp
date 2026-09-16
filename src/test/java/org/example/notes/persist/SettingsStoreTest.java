package org.example.notes.persist;

import org.example.notes.model.ThemePreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsStoreTest {
    @TempDir
    Path temp;

    @Test
    void createsSettingsFileWithSystemThemeOnFirstLoad() throws Exception {
        Path file = temp.resolve("settings.json");
        SettingsStore store = new SettingsStore(file);
        store.load();

        assertTrue(Files.exists(file));
        assertEquals(ThemePreference.SYSTEM, store.get().getTheme());
        assertTrue(store.dark(true));
        assertFalse(store.dark(false));
    }

    @Test
    void persistsExplicitThemeAcrossReload() {
        Path file = temp.resolve("settings.json");
        SettingsStore store = new SettingsStore(file);
        store.load();
        store.update(settings -> settings.setTheme(ThemePreference.LIGHT));

        SettingsStore reloaded = new SettingsStore(file);
        reloaded.load();
        assertEquals(ThemePreference.LIGHT, reloaded.get().getTheme());
        assertFalse(reloaded.dark(true));
    }

    @Test
    void ignoresUnknownFutureFields() throws Exception {
        Path file = temp.resolve("settings.json");
        Files.writeString(file, """
                {
                  "version": 1,
                  "theme": "dark",
                  "futureFlag": true
                }
                """);
        SettingsStore store = new SettingsStore(file);
        store.load();
        assertEquals(ThemePreference.DARK, store.get().getTheme());
    }
}
