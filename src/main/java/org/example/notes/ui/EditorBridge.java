package org.example.notes.ui;

import javafx.application.Platform;

import java.util.function.BiConsumer;

public class EditorBridge {
    private BiConsumer<String, String> listener = (html, text) -> {
    };

    public void setListener(BiConsumer<String, String> listener) {
        this.listener = listener == null ? (html, text) -> {
        } : listener;
    }

    public void onContentChanged(String html, String text) {
        BiConsumer<String, String> target = listener;
        Platform.runLater(() -> target.accept(html, text));
    }
}
