package org.example.notes.ui;

import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.example.notes.model.Note;
import org.example.notes.service.DateGroups;

import java.time.Clock;
import java.util.function.BiConsumer;

public class EditorPane extends BorderPane {
    private final Clock clock;
    private final WebView webView;
    private final WebEngine engine;
    private final Label dateLabel;
    private final Label empty;
    private final EditorBridge bridge = new EditorBridge();
    private boolean ready;
    private boolean applying;
    private boolean dark;
    private boolean readOnly;
    private String currentNoteId;
    private String pendingHtml;

    public EditorPane(Clock clock) {
        this.clock = clock;
        getStyleClass().add("editor-pane");

        dateLabel = new Label();
        dateLabel.getStyleClass().add("editor-date");
        dateLabel.setMaxWidth(Double.MAX_VALUE);
        dateLabel.setAlignment(Pos.CENTER);

        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.getStyleClass().add("web-view");
        engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaApp", bridge);
                ready = true;
                engine.executeScript("setDark(" + dark + ")");
                engine.executeScript("setReadOnly(" + readOnly + ")");
                if (pendingHtml != null) {
                    applyHtmlNow(pendingHtml);
                    pendingHtml = null;
                    focusEditor();
                }
            }
        });
        var resource = getClass().getResource("/editor/editor.html");
        if (resource != null) {
            engine.load(resource.toExternalForm());
        }

        empty = new Label("Select a note or create a new one");
        empty.getStyleClass().add("empty-editor");

        StackPane center = new StackPane(webView, empty);
        setTop(dateLabel);
        setCenter(center);
        showEmpty();
    }

    public void setChangeListener(BiConsumer<String, String> listener) {
        bridge.setListener((html, text) -> {
            if (applying || currentNoteId == null || readOnly) {
                return;
            }
            listener.accept(html, text);
        });
    }

    public void show(Note note) {
        empty.setVisible(false);
        webView.setVisible(true);
        dateLabel.setVisible(true);
        String stamp = DateGroups.editorTimestamp(note, clock);
        dateLabel.setText(note.isReadOnly() ? stamp + "  ·  Read-only" : stamp);
        boolean same = note.getId().equals(currentNoteId);
        currentNoteId = note.getId();
        setReadOnly(note.isReadOnly());
        if (!same) {
            applyHtml(note.getHtml());
            if (!note.isReadOnly()) {
                focusEditor();
            }
        }
    }

    public void showEmpty() {
        currentNoteId = null;
        empty.setVisible(true);
        webView.setVisible(false);
        dateLabel.setVisible(false);
        dateLabel.setText("");
    }

    public void setDark(boolean dark) {
        this.dark = dark;
        if (ready) {
            engine.executeScript("setDark(" + dark + ")");
        }
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (ready) {
            engine.executeScript("setReadOnly(" + readOnly + ")");
        }
    }

    public void command(String command, String value) {
        if (!ready || currentNoteId == null || readOnly) {
            return;
        }
        String escaped = value == null ? "null" : "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
        engine.executeScript("applyCommand('" + command + "', " + escaped + ")");
    }

    public String currentNoteId() {
        return currentNoteId;
    }

    private void applyHtml(String html) {
        if (!ready) {
            pendingHtml = html;
            return;
        }
        applyHtmlNow(html);
    }

    private void applyHtmlNow(String html) {
        applying = true;
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.call("setHtml", html == null ? "" : html);
        } finally {
            applying = false;
        }
    }

    private void focusEditor() {
        if (!ready) {
            return;
        }
        webView.requestFocus();
        engine.executeScript("focusEditor()");
    }
}
