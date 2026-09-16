package org.example.notes.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.notes.model.Folder;
import org.example.notes.model.FolderKind;
import org.example.notes.service.NotesService;

import java.util.Set;
import java.util.function.Consumer;

public class SidebarPane extends BorderPane {
    private final NotesService service;
    private final VBox list = new VBox(2);
    private String selectedFolderId = Folder.ALL_ID;
    private String selectedTag;
    private Consumer<String> onFolder = id -> {
    };
    private Consumer<String> onTag = tag -> {
    };
    private Runnable onNewFolder = () -> {
    };
    private Button addFolderButton;
    private Consumer<Folder> onRename = f -> {
    };
    private Consumer<Folder> onDelete = f -> {
    };
    private Runnable onEraseAll = () -> {
    };

    public SidebarPane(NotesService service) {
        this.service = service;
        getStyleClass().add("sidebar");

        ScrollPane scroll = new ScrollPane(list);
        scroll.getStyleClass().add("sidebar-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);

        addFolderButton = new Button("New Folder");
        addFolderButton.setGraphic(Icons.plus("#1d1d1f"));
        addFolderButton.getStyleClass().add("folder-add");
        addFolderButton.setTooltip(new javafx.scene.control.Tooltip("New Folder"));
        addFolderButton.setOnAction(e -> onNewFolder.run());
        HBox footer = new HBox(addFolderButton);
        footer.getStyleClass().add("sidebar-footer");
        setBottom(footer);
    }

    public void setOnFolder(Consumer<String> onFolder) {
        this.onFolder = onFolder;
    }

    public void setOnTag(Consumer<String> onTag) {
        this.onTag = onTag;
    }

    public void setOnNewFolder(Runnable onNewFolder) {
        this.onNewFolder = onNewFolder;
    }

    public void setOnRename(Consumer<Folder> onRename) {
        this.onRename = onRename;
    }

    public void setOnDelete(Consumer<Folder> onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnEraseAll(Runnable onEraseAll) {
        this.onEraseAll = onEraseAll;
    }

    public String selectedFolderId() {
        return selectedFolderId;
    }

    public String selectedTag() {
        return selectedTag;
    }

    public void setInk(String color) {
        if (addFolderButton != null) {
            addFolderButton.setGraphic(Icons.plus(color));
        }
    }

    public void selectFolder(String id) {
        selectedFolderId = id;
        selectedTag = null;
        refresh();
    }

    public void refresh() {
        list.getChildren().clear();
        Label account = new Label("ON MY COMPUTER");
        account.getStyleClass().add("section-label");
        list.getChildren().add(account);

        for (Folder folder : service.sidebarFolders()) {
            list.getChildren().add(folderRow(folder));
        }

        Set<String> tags = service.allTags();
        if (!tags.isEmpty()) {
            Label tagsLabel = new Label("TAGS");
            tagsLabel.getStyleClass().add("section-label");
            list.getChildren().add(tagsLabel);
            for (String tag : tags) {
                list.getChildren().add(tagRow(tag));
            }
        }
    }

    private Node folderRow(Folder folder) {
        HBox row = new HBox(8);
        row.getStyleClass().add("folder-row");
        if (selectedTag == null && folder.getId().equals(selectedFolderId)) {
            row.getStyleClass().add("selected");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(iconFor(folder));
        Label name = new Label(folder.getName());
        name.getStyleClass().add("folder-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(Integer.toString(service.count(folder.getId())));
        count.getStyleClass().add("folder-count");
        row.getChildren().addAll(name, spacer, count);
        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectedFolderId = folder.getId();
                selectedTag = null;
                refresh();
                onFolder.accept(folder.getId());
            }
        });
        if (folder.getKind() == FolderKind.USER || folder.getKind() == FolderKind.DEFAULT) {
            MenuItem rename = new MenuItem("Rename Folder");
            rename.setOnAction(e -> onRename.accept(folder));
            ContextMenu menu = new ContextMenu(rename);
            if (folder.getKind() == FolderKind.USER) {
                MenuItem delete = new MenuItem("Delete Folder");
                delete.setOnAction(e -> onDelete.accept(folder));
                menu.getItems().addAll(new SeparatorMenuItem(), delete);
            }
            row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));
        } else if (folder.getKind() == FolderKind.DELETED) {
            MenuItem erase = new MenuItem("Erase All");
            erase.setOnAction(e -> onEraseAll.run());
            ContextMenu menu = new ContextMenu(erase);
            row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));
        }
        return row;
    }

    private Node tagRow(String tag) {
        HBox row = new HBox(8);
        row.getStyleClass().add("tag-row");
        if (tag.equals(selectedTag)) {
            row.getStyleClass().add("selected");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(Icons.tag("#86868b"));
        Label name = new Label("#" + tag);
        name.getStyleClass().add("tag-name");
        row.getChildren().add(name);
        row.setOnMouseClicked(e -> {
            selectedTag = tag;
            selectedFolderId = Folder.ALL_ID;
            refresh();
            onTag.accept(tag);
        });
        return row;
    }

    private Node iconFor(Folder folder) {
        return switch (folder.getKind()) {
            case ALL -> Icons.allNotes();
            case DEFAULT -> Icons.notesPad();
            case DELETED -> Icons.trash("#86868b");
            case USER -> Icons.folder();
        };
    }
}
