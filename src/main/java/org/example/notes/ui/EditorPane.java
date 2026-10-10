package org.example.notes.ui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.example.notes.model.Note;
import org.example.notes.service.DateGroups;

import java.time.Clock;
import java.util.Locale;
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
        // WebView's own edit menu paints on macOS and Linux. On Windows the same
        // right-click shows nothing, so the editor installs its own menu there.
        if (isWindows()) {
            installWindowsEditMenu();
        }
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

    private void installWindowsEditMenu() {
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        cut.setOnAction(e -> copySelection(true));
        copy.setOnAction(e -> copySelection(false));
        paste.setOnAction(e -> pasteFromClipboard());
        ContextMenu menu = new ContextMenu(cut, copy, paste);
        menu.setOnShowing(e -> {
            boolean selection = hasSelection();
            boolean editable = ready && !readOnly && currentNoteId != null;
            cut.setDisable(!editable || !selection);
            copy.setDisable(!selection);
            paste.setDisable(!editable || !clipboardHasContent());
        });
        webView.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() != MouseButton.SECONDARY || currentNoteId == null || !webView.isVisible()) {
                return;
            }
            double screenX = event.getScreenX();
            double screenY = event.getScreenY();
            if (ready) {
                engine.executeScript("saveSelection()");
            }
            // Showing during the release makes Windows dismiss the popup before it paints.
            Platform.runLater(() -> {
                if (menu.isShowing()) {
                    menu.hide();
                }
                menu.show(webView, screenX, screenY);
            });
        });
    }

    private void copySelection(boolean cut) {
        if (!ready || currentNoteId == null || (cut && readOnly)) {
            return;
        }
        engine.executeScript("restoreSelection()");
        String text = scriptString("selectedText()");
        String html = scriptString("selectedHtml()");
        if (text.isEmpty() && html.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        if (!text.isEmpty()) {
            content.putString(text);
        }
        if (!html.isEmpty()) {
            content.putHtml(html);
        }
        Clipboard.getSystemClipboard().setContent(content);
        if (cut) {
            engine.executeScript("deleteSelection()");
        }
    }

    private void pasteFromClipboard() {
        if (!ready || readOnly || currentNoteId == null) {
            return;
        }
        webView.requestFocus();
        engine.executeScript("restoreSelection()");
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String html = clipboard.getHtml() == null ? "" : clipboard.getHtml();
        String text = clipboard.getString() == null ? "" : clipboard.getString();
        if (html.isBlank() && text.isEmpty()) {
            return;
        }
        JSObject window = (JSObject) engine.executeScript("window");
        window.call("insertClipboard", html, text);
    }

    private String scriptString(String script) {
        Object value = engine.executeScript(script);
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "null".equals(text) ? "" : text;
    }

    private boolean hasSelection() {
        if (!ready) {
            return false;
        }
        return Boolean.TRUE.equals(engine.executeScript("hasSelection()"));
    }

    private static boolean clipboardHasContent() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        // On Windows hasString() stays false for text copied by other programs.
        String text = clipboard.getString();
        String html = clipboard.getHtml();
        return (text != null && !text.isEmpty()) || (html != null && !html.isBlank());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
