package org.example.notes.model;

public class AppSettings {
    private int version = 1;
    private ThemePreference theme = ThemePreference.SYSTEM;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public ThemePreference getTheme() {
        return theme == null ? ThemePreference.SYSTEM : theme;
    }

    public void setTheme(ThemePreference theme) {
        this.theme = theme == null ? ThemePreference.SYSTEM : theme;
    }
}
