package org.example.notes.ui;

import javafx.application.Platform;

import java.util.function.BiConsumer;

public class EditorBridge {
    public interface ContextMenuListener {
        void onContextMenu(double clientX, double clientY, boolean hasSelection);
    }

    private BiConsumer<String, String> listener = (html, text) -> {
    };
    private ContextMenuListener contextMenuListener = (x, y, sel) -> {
    };

    public void setListener(BiConsumer<String, String> listener) {
        this.listener = listener == null ? (html, text) -> {
        } : listener;
    }

    public void setContextMenuListener(ContextMenuListener contextMenuListener) {
        this.contextMenuListener = contextMenuListener == null ? (x, y, sel) -> {
        } : contextMenuListener;
    }

    public void onContentChanged(String html, String text) {
        BiConsumer<String, String> target = listener;
        Platform.runLater(() -> target.accept(html, text));
    }

    public void onContextMenu(Object x, Object y, Object hasSelection) {
        double clientX = toDouble(x);
        double clientY = toDouble(y);
        boolean selection = toBoolean(hasSelection);
        ContextMenuListener target = contextMenuListener;
        Platform.runLater(() -> target.onContextMenu(clientX, clientY, selection));
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
