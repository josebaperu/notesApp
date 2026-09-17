package org.example.notes.ui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.notes.model.Note;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EditorClipboardMenuFxTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assumeTrue(started.await(15, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void cutIsGreyedOutOnReadOnlyNotesWhenTextIsSelected() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean cutDisabled = new AtomicBoolean();
        AtomicBoolean copyDisabled = new AtomicBoolean(true);
        AtomicBoolean pasteDisabled = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                EditorPane pane = new EditorPane(Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC));
                pane.setReadOnly(true);
                pane.updateClipboardMenuState(true);
                cutDisabled.set(pane.isCutMenuDisabled());
                copyDisabled.set(pane.isCopyMenuDisabled());
                pasteDisabled.set(pane.isPasteMenuDisabled());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(20, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        assertTrue(cutDisabled.get(), "Cut should be greyed out in read-only mode");
        assertFalse(copyDisabled.get(), "Copy should stay available when text is selected");
        assertTrue(pasteDisabled.get(), "Paste should be greyed out in read-only mode");
    }

    @Test
    void editorContextMenuNotifiesJavaWithSelection() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch notified = new CountDownLatch(1);
        AtomicBoolean hasSelection = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                WebView webView = new WebView();
                webView.setContextMenuEnabled(false);
                WebEngine engine = webView.getEngine();
                EditorBridge bridge = new EditorBridge();
                bridge.setContextMenuListener((x, y, selected) -> {
                    hasSelection.set(selected);
                    notified.countDown();
                });
                engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                    if (state != Worker.State.SUCCEEDED) {
                        return;
                    }
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaApp", bridge);
                        window.call("setHtml", "<p>Hello world</p>");
                        engine.executeScript("""
                                (function () {
                                    const range = document.createRange();
                                    range.selectNodeContents(content);
                                    const sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);
                                    content.dispatchEvent(new MouseEvent('contextmenu', {
                                        clientX: 12,
                                        clientY: 18,
                                        bubbles: true,
                                        cancelable: true
                                    }));
                                })();
                                """);
                        loaded.countDown();
                    } catch (Throwable t) {
                        error.set(t);
                        loaded.countDown();
                    }
                });
                var resource = EditorPane.class.getResource("/editor/editor.html");
                assumeTrue(resource != null);
                engine.load(resource.toExternalForm());
                Stage stage = new Stage();
                stageRef.set(stage);
                stage.setScene(new Scene(webView, 400, 300));
                stage.show();
            } catch (Throwable t) {
                error.set(t);
                loaded.countDown();
            }
        });

        assertTrue(loaded.await(20, TimeUnit.SECONDS), "editor.html did not load");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        assertTrue(notified.await(10, TimeUnit.SECONDS), "right-click did not notify Java");
        assertTrue(hasSelection.get(), "context menu should see the selected text");
        Platform.runLater(() -> {
            Stage stage = stageRef.get();
            if (stage != null) {
                stage.close();
            }
        });
    }

    @Test
    void showingAReadOnlyNoteGreysOutCut() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean cutDisabled = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);
                EditorPane pane = new EditorPane(clock);
                Note note = new Note();
                note.setId("n1");
                note.setHtml("<p>Locked</p>");
                note.setPlainText("Locked");
                note.setReadOnly(true);
                pane.show(note);
                pane.updateClipboardMenuState(true);
                cutDisabled.set(pane.isCutMenuDisabled());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(20, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        assertTrue(cutDisabled.get());
    }
}
