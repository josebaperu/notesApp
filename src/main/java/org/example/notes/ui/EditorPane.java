package org.example.notes.ui;

import javafx.concurrent.Worker;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
import java.util.function.BiConsumer;

public class EditorPane extends BorderPane {
    private final Clock clock;
    private final WebView webView;
    private final WebEngine engine;
    private final Label dateLabel;
    private final Label empty;
    private final EditorBridge bridge = new EditorBridge();
    private final ContextMenu clipboardMenu = new ContextMenu();
    private final MenuItem cutItem = new MenuItem("Cut");
    private final MenuItem copyItem = new MenuItem("Copy");
    private final MenuItem pasteItem = new MenuItem("Paste");
    private boolean ready;
    private boolean applying;
    private boolean dark;
    private boolean readOnly;
    private String currentNoteId;
    private String pendingHtml;
    private long lastMenuAt;

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
        buildClipboardMenu();
        bridge.setContextMenuListener(this::showClipboardMenuFromClient);
        webView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            showClipboardMenuFromEditor(event.getScreenX(), event.getScreenY());
            event.consume();
        });
        webView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                showClipboardMenuFromEditor(event.getScreenX(), event.getScreenY());
            }
        });
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
        clipboardMenu.hide();
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
        clipboardMenu.hide();
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
        if (readOnly) {
            cutItem.setDisable(true);
            pasteItem.setDisable(true);
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

    private void buildClipboardMenu() {
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        cutItem.setOnAction(e -> cut());
        copyItem.setOnAction(e -> copy());
        pasteItem.setOnAction(e -> paste());
        clipboardMenu.getItems().addAll(copyItem, cutItem, pasteItem);
    }

    private void showClipboardMenuFromClient(double clientX, double clientY, boolean hasSelection) {
        Point2D screen = webView.localToScreen(clientX, clientY);
        if (screen == null) {
            return;
        }
        showClipboardMenuAt(screen.getX(), screen.getY(), hasSelection);
    }

    private void showClipboardMenuFromEditor(double screenX, double screenY) {
        if (!ready || currentNoteId == null || !webView.isVisible()) {
            return;
        }
        JSObject window = window();
        window.call("saveSelection");
        boolean hasSelection = isTrue(window.call("hasSelection"));
        showClipboardMenuAt(screenX, screenY, hasSelection);
    }

    private void showClipboardMenuAt(double screenX, double screenY, boolean hasSelection) {
        if (currentNoteId == null || !webView.isVisible()) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastMenuAt < 80_000_000L && clipboardMenu.isShowing()) {
            return;
        }
        lastMenuAt = now;
        clipboardMenu.hide();
        updateClipboardMenuState(hasSelection);
        clipboardMenu.show(webView, screenX, screenY);
    }

    void updateClipboardMenuState(boolean hasSelection) {
        boolean clipboardHasContent = clipboardHasContent();
        cutItem.setDisable(!EditorClipboardState.cutEnabled(readOnly, hasSelection));
        copyItem.setDisable(!EditorClipboardState.copyEnabled(hasSelection));
        pasteItem.setDisable(!EditorClipboardState.pasteEnabled(readOnly, clipboardHasContent));
    }

    boolean isCutMenuDisabled() {
        return cutItem.isDisable();
    }

    boolean isCopyMenuDisabled() {
        return copyItem.isDisable();
    }

    boolean isPasteMenuDisabled() {
        return pasteItem.isDisable();
    }

    private void cut() {
        if (!ready || !EditorClipboardState.cutEnabled(readOnly, true)) {
            return;
        }
        JSObject window = window();
        if (isTrue(window.call("doCut"))) {
            return;
        }
        copySavedToClipboard(window);
        window.call("deleteSelection");
    }

    private void copy() {
        if (!ready) {
            return;
        }
        JSObject window = window();
        if (isTrue(window.call("doCopy"))) {
            return;
        }
        copySavedToClipboard(window);
    }

    private void paste() {
        if (!ready || readOnly) {
            return;
        }
        JSObject window = window();
        if (isTrue(window.call("doPaste"))) {
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasHtml()) {
            window.call("insertHtml", clipboard.getHtml());
        } else if (clipboard.hasString()) {
            window.call("insertText", clipboard.getString());
        }
    }

    private void copySavedToClipboard(JSObject window) {
        String text = asString(window.call("getSavedText"));
        String html = asString(window.call("getSavedHtml"));
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
    }

    private JSObject window() {
        return (JSObject) engine.executeScript("window");
    }

    private static boolean clipboardHasContent() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.hasString() || clipboard.hasHtml();
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String asString(Object value) {
        if (value instanceof String string) {
            return string;
        }
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "undefined".equals(text) ? "" : text;
    }
}
