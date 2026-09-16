package org.example.notes.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.notes.model.Folder;
import org.example.notes.model.Note;
import org.example.notes.model.SortMode;
import org.example.notes.service.DateGroups;
import org.example.notes.service.ListItem;
import org.example.notes.service.NotesService;

import java.time.Clock;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NoteListPane extends BorderPane {
    private final NotesService service;
    private final Clock clock;
    private final Label title = new Label();
    private final Label count = new Label();
    private final VBox list = new VBox(2);
    private final FlowPane gallery = new FlowPane();
    private final ScrollPane scroll;
    private final StackPane body;
    private final Label empty = new Label("No Notes");
    private boolean galleryMode;
    private String selectedNoteId;
    private Consumer<Note> onSelect = n -> {
    };
    private Consumer<Note> onDelete = n -> {
    };
    private Consumer<Note> onPin = n -> {
    };
    private Consumer<Note> onRestore = n -> {
    };
    private BiConsumer<Note, String> onMove = (n, folderId) -> {
    };
    private BiConsumer<Note, Boolean> onReadOnly = (n, readOnly) -> {
    };

    public NoteListPane(NotesService service, Clock clock) {
        this.service = service;
        this.clock = clock;
        getStyleClass().add("list-pane");

        title.getStyleClass().add("list-title");
        count.getStyleClass().add("list-count");
        HBox header = new HBox(title, count);
        header.getStyleClass().add("list-header");
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        empty.getStyleClass().add("empty-list");
        gallery.getStyleClass().add("gallery-flow");
        list.setFillWidth(true);

        body = new StackPane(list, gallery, empty);
        StackPane.setAlignment(empty, Pos.CENTER);
        scroll = new ScrollPane(body);
        scroll.getStyleClass().add("notes-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);
    }

    public void setOnSelect(Consumer<Note> onSelect) {
        this.onSelect = onSelect;
    }

    public void setOnDelete(Consumer<Note> onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnPin(Consumer<Note> onPin) {
        this.onPin = onPin;
    }

    public void setOnRestore(Consumer<Note> onRestore) {
        this.onRestore = onRestore;
    }

    public void setOnMove(BiConsumer<Note, String> onMove) {
        this.onMove = onMove;
    }

    public void setOnReadOnly(BiConsumer<Note, Boolean> onReadOnly) {
        this.onReadOnly = onReadOnly;
    }

    public void setGalleryMode(boolean galleryMode) {
        this.galleryMode = galleryMode;
    }

    public boolean isGalleryMode() {
        return galleryMode;
    }

    public String selectedNoteId() {
        return selectedNoteId;
    }

    public void selectNote(String id) {
        selectedNoteId = id;
    }

    public void render(Folder folder, List<Note> notes, SortMode sortMode, String tag) {
        String heading = tag != null ? "#" + tag : folder.getName();
        title.setText(heading);
        int n = notes.size();
        count.setText(n + (n == 1 ? " note" : " notes"));

        list.getChildren().clear();
        gallery.getChildren().clear();

        if (notes.isEmpty()) {
            empty.setVisible(true);
            list.setVisible(false);
            gallery.setVisible(false);
            return;
        }
        empty.setVisible(false);
        list.setVisible(!galleryMode);
        gallery.setVisible(galleryMode);

        if (galleryMode) {
            for (Note note : notes) {
                gallery.getChildren().add(galleryCard(note));
            }
            return;
        }

        for (ListItem item : DateGroups.build(notes, sortMode, clock)) {
            switch (item) {
                case ListItem.Header header -> {
                    Label label = new Label(header.label().toUpperCase());
                    label.getStyleClass().add("date-header");
                    list.getChildren().add(label);
                }
                case ListItem.NoteRow row -> list.getChildren().add(listCell(row.note()));
            }
        }
    }

    public void refreshPreviews(List<Note> notes) {
        renderKeepStructure(notes);
    }

    private void renderKeepStructure(List<Note> notes) {
        // Cells bind from note objects already mutated; just update visible labels.
        list.getChildren().forEach(node -> {
            if (node instanceof VBox cell && cell.getUserData() instanceof Note note) {
                updateCell(cell, note);
            }
        });
        gallery.getChildren().forEach(node -> {
            if (node instanceof VBox card && card.getUserData() instanceof Note note) {
                updateCard(card, note);
            }
        });
    }

    private VBox listCell(Note note) {
        Label titleLabel = new Label(note.displayTitle());
        titleLabel.getStyleClass().add("note-title");
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label date = new Label(DateGroups.listTimestamp(note, clock));
        date.getStyleClass().add("note-date");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, readOnlyBox(note), titleLabel, spacer, date);
        top.setAlignment(Pos.CENTER_LEFT);

        Label preview = new Label(previewText(note));
        preview.getStyleClass().add("note-preview");
        preview.setWrapText(true);
        preview.setMaxHeight(36);
        preview.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox cell = new VBox(2, top, preview);
        cell.getStyleClass().add("note-cell");
        cell.setUserData(note);
        applySelected(cell, note);
        cell.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectedNoteId = note.getId();
                onSelect.accept(note);
            }
        });
        cell.setOnContextMenuRequested(e -> menuFor(note).show(cell, e.getScreenX(), e.getScreenY()));
        return cell;
    }

    private VBox galleryCard(Note note) {
        Label titleLabel = new Label(note.displayTitle());
        titleLabel.getStyleClass().add("note-title");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        HBox heading = new HBox(6, readOnlyBox(note), titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label preview = new Label(previewText(note));
        preview.getStyleClass().add("note-preview");
        preview.setWrapText(true);
        VBox.setVgrow(preview, Priority.ALWAYS);
        Label date = new Label(DateGroups.listTimestamp(note, clock));
        date.getStyleClass().add("note-date");
        VBox card = new VBox(6, heading, preview, date);
        card.getStyleClass().add("gallery-card");
        card.setUserData(note);
        applySelected(card, note);
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectedNoteId = note.getId();
                onSelect.accept(note);
            }
        });
        card.setOnContextMenuRequested(e -> menuFor(note).show(card, e.getScreenX(), e.getScreenY()));
        return card;
    }

    private CheckBox readOnlyBox(Note note) {
        CheckBox box = new CheckBox();
        box.getStyleClass().add("note-readonly");
        box.setSelected(note.isReadOnly());
        box.setFocusTraversable(false);
        box.setTooltip(new Tooltip(note.isReadOnly() ? "Read-only" : "Mark as read-only"));
        box.setOnAction(e -> {
            onReadOnly.accept(note, box.isSelected());
            box.setTooltip(new Tooltip(box.isSelected() ? "Read-only" : "Mark as read-only"));
        });
        box.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        return box;
    }

    private void updateCell(VBox cell, Note note) {
        if (cell.getChildren().getFirst() instanceof HBox top) {
            updateHeading(top, note);
        }
        if (cell.getChildren().size() > 1 && cell.getChildren().get(1) instanceof Label preview) {
            preview.setText(previewText(note));
        }
        applySelected(cell, note);
    }

    private void updateCard(VBox card, Note note) {
        if (card.getChildren().getFirst() instanceof HBox heading) {
            updateHeading(heading, note);
        } else if (card.getChildren().getFirst() instanceof Label titleLabel) {
            titleLabel.setText(note.displayTitle());
        }
        if (card.getChildren().size() > 1 && card.getChildren().get(1) instanceof Label preview) {
            preview.setText(previewText(note));
        }
        applySelected(card, note);
    }

    private void updateHeading(HBox heading, Note note) {
        for (Node child : heading.getChildren()) {
            if (child instanceof CheckBox box) {
                box.setSelected(note.isReadOnly());
                box.setTooltip(new Tooltip(note.isReadOnly() ? "Read-only" : "Mark as read-only"));
            } else if (child instanceof Label label && label.getStyleClass().contains("note-title")) {
                label.setText(note.displayTitle());
            } else if (child instanceof Label label && label.getStyleClass().contains("note-date")) {
                label.setText(DateGroups.listTimestamp(note, clock));
            }
        }
    }

    private void applySelected(VBox node, Note note) {
        node.getStyleClass().remove("selected");
        if (note.getId().equals(selectedNoteId)) {
            node.getStyleClass().add("selected");
        }
    }

    private ContextMenu menuFor(Note note) {
        ContextMenu menu = new ContextMenu();
        if (note.isDeleted()) {
            MenuItem restore = new MenuItem("Recover");
            restore.setOnAction(e -> onRestore.accept(note));
            MenuItem delete = new MenuItem("Delete Permanently");
            delete.setOnAction(e -> onDelete.accept(note));
            menu.getItems().addAll(restore, delete);
            return menu;
        }
        MenuItem pin = new MenuItem(note.isPinned() ? "Unpin Note" : "Pin Note");
        pin.setOnAction(e -> onPin.accept(note));
        Menu move = new Menu("Move to Folder");
        for (Folder folder : service.movableFolders()) {
            MenuItem item = new MenuItem(folder.getName());
            item.setDisable(folder.getId().equals(note.getFolderId()));
            item.setOnAction(e -> onMove.accept(note, folder.getId()));
            move.getItems().add(item);
        }
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> onDelete.accept(note));
        menu.getItems().addAll(pin, move, new SeparatorMenuItem(), delete);
        return menu;
    }

    private static String previewText(Note note) {
        String preview = DateGroups.preview(note);
        return preview.isBlank() ? "No additional text" : preview;
    }
}
