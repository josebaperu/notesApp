package org.example.notes.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static ThemePreference fromJson(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "dark" -> DARK;
            case "light" -> LIGHT;
            default -> SYSTEM;
        };
    }
}
