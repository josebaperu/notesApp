package org.example.notes.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.notes.model.Folder;
import org.example.notes.model.Note;
import org.example.notes.model.SortMode;
import org.example.notes.service.NotesService;

import java.awt.Desktop;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public class NotesWorkspace extends BorderPane {
    private final Stage stage;
    private final NotesService service;
    private final Clock clock;
    private final SidebarPane sidebar;
    private final NoteListPane listPane;
    private final EditorPane editor;
    private final FormatPopup formatPopup = new FormatPopup();
    private final SplitPane split = new SplitPane();
    private final TextField search = new TextField();
    private final Button newNoteButton = new Button();
    private final Button deleteButton = new Button();
    private final Button pinButton = new Button();
    private final Button recoverButton = new Button();
    private final Button formatButton = new Button();
    private final Button checklistButton = new Button();
    private final Button tableButton = new Button();
    private final ToggleButton galleryButton = new ToggleButton();
    private final ToggleButton sidebarButton = new ToggleButton();
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(450));
    private SortMode sortMode = SortMode.DATE_EDITED;
    private boolean dark;
    private String leavingNoteId;

    public NotesWorkspace(Stage stage, NotesService service, Clock clock) {
        this.stage = stage;
        this.service = service;
        this.clock = clock;
        getStyleClass().add("workspace");

        sidebar = new SidebarPane(service);
        listPane = new NoteListPane(service, clock);
        editor = new EditorPane(clock);

        styleToolButton(newNoteButton, "New Note");
        newNoteButton.getStyleClass().add("btn-new-note");
        styleToolButton(deleteButton, "Delete");
        styleToolButton(pinButton, "Pin");
        styleToolButton(recoverButton, "Recover");
        styleToolButton(formatButton, "Formatting");
        styleToolButton(checklistButton, "Checklist");
        styleToolButton(tableButton, "Table");
        galleryButton.getStyleClass().add("tool-btn");
        galleryButton.setTooltip(new Tooltip("Gallery View"));
        sidebarButton.getStyleClass().add("tool-btn");
        sidebarButton.setSelected(true);
        sidebarButton.setTooltip(new Tooltip("Hide Folders"));

        setTop(new BorderPane(toolbar(), null, null, null, menuBar()));
        buildSplit();
        wire();
        applyTheme(detectDark());
        refreshAll();
        selectInitial();
    }

    private void buildSplit() {
        split.getItems().addAll(sidebar, listPane, editor);
        split.setDividerPositions(0.18, 0.42);
        SplitPane.setResizableWithParent(sidebar, false);
        SplitPane.setResizableWithParent(listPane, false);
        setCenter(split);
    }

    private MenuBar menuBar() {
        MenuBar bar = new MenuBar();

        MenuItem newNote = item("New Note", KeyCode.N, true, false, this::createNote);
        MenuItem newFolder = item("New Folder", KeyCode.N, true, true, this::createFolder);
        MenuItem showData = new MenuItem("Show Data File");
        showData.setOnAction(e -> revealDataFile());
        MenuItem close = item("Close", KeyCode.W, true, false, stage::close);
        Menu file = new Menu("File", null, newNote, newFolder, new SeparatorMenuItem(), showData, close);

        MenuItem find = item("Find", KeyCode.F, true, false, () -> search.requestFocus());
        Menu edit = new Menu("Edit", null, find);

        Menu format = new Menu("Format");
        format.getItems().addAll(
                item("Title", KeyCode.DIGIT1, true, false, () -> editor.command("title", null)),
                item("Heading", KeyCode.DIGIT2, true, false, () -> editor.command("heading", null)),
                item("Subheading", KeyCode.DIGIT3, true, false, () -> editor.command("subheading", null)),
                item("Body", KeyCode.DIGIT4, true, false, () -> editor.command("body", null)),
                new SeparatorMenuItem(),
                item("Bold", KeyCode.B, true, false, () -> editor.command("bold", null)),
                item("Italic", KeyCode.I, true, false, () -> editor.command("italic", null)),
                item("Underline", KeyCode.U, true, false, () -> editor.command("underline", null)),
                new SeparatorMenuItem(),
                item("Bulleted List", KeyCode.L, true, true, () -> editor.command("insertUnorderedList", null)),
                item("Checklist", KeyCode.L, true, false, () -> editor.command("checklist", null))
        );

        RadioMenuItem listView = new RadioMenuItem("as List");
        RadioMenuItem galleryView = new RadioMenuItem("as Gallery");
        ToggleGroup viewGroup = new ToggleGroup();
        listView.setToggleGroup(viewGroup);
        galleryView.setToggleGroup(viewGroup);
        listView.setSelected(true);
        listView.setOnAction(e -> setGallery(false));
        galleryView.setOnAction(e -> setGallery(true));
        galleryButton.selectedProperty().addListener((o, a, selected) -> {
            listView.setSelected(!selected);
            galleryView.setSelected(selected);
        });

        RadioMenuItem byEdited = new RadioMenuItem("Date Edited");
        RadioMenuItem byCreated = new RadioMenuItem("Date Created");
        RadioMenuItem byTitle = new RadioMenuItem("Title");
        ToggleGroup sortGroup = new ToggleGroup();
        byEdited.setToggleGroup(sortGroup);
        byCreated.setToggleGroup(sortGroup);
        byTitle.setToggleGroup(sortGroup);
        byEdited.setSelected(true);
        byEdited.setOnAction(e -> setSort(SortMode.DATE_EDITED));
        byCreated.setOnAction(e -> setSort(SortMode.DATE_CREATED));
        byTitle.setOnAction(e -> setSort(SortMode.TITLE));

        MenuItem darkMode = new MenuItem("Toggle Dark Mode");
        darkMode.setOnAction(e -> toggleDark());
        Menu view = new Menu("View", null,
                listView, galleryView,
                new SeparatorMenuItem(),
                new Menu("Sort By", null, byEdited, byCreated, byTitle),
                new SeparatorMenuItem(),
                darkMode);

        MenuItem pin = item("Pin Note", KeyCode.P, true, true, this::togglePin);
        MenuItem delete = item("Delete", KeyCode.BACK_SPACE, true, false, this::deleteSelected);
        Menu note = new Menu("Note", null, pin, delete);

        bar.getMenus().addAll(file, edit, format, view, note);
        return bar;
    }

    private HBox toolbar() {
        formatButton.setOnAction(e -> formatPopup.show(formatButton));
        checklistButton.setOnAction(e -> editor.command("checklist", null));
        tableButton.setOnAction(e -> editor.command("table", null));

        newNoteButton.setOnAction(e -> createNote());
        deleteButton.setOnAction(e -> deleteSelected());
        pinButton.setOnAction(e -> togglePin());
        recoverButton.setOnAction(e -> restoreSelected());
        recoverButton.setVisible(false);
        recoverButton.setManaged(false);

        sidebarButton.setOnAction(e -> toggleSidebar(sidebarButton.isSelected()));
        galleryButton.setOnAction(e -> setGallery(galleryButton.isSelected()));

        search.setPromptText("Search");
        search.getStyleClass().add("search-field");
        StackPane searchWrap = new StackPane(search, Icons.search("#86868b"));
        searchWrap.getStyleClass().add("search-wrap");
        StackPane.setAlignment(searchWrap.getChildren().get(1), Pos.CENTER_LEFT);
        searchWrap.getChildren().get(1).setTranslateX(10);
        search.textProperty().addListener((o, a, b) -> refreshAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(6,
                sidebarButton, galleryButton, vline(), newNoteButton, vline(),
                deleteButton, pinButton, recoverButton, formatButton, checklistButton, tableButton,
                spacer, searchWrap);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void wire() {
        saveDebounce.setOnFinished(e -> service.save());
        formatPopup.setOnCommand(editor::command);
        sidebar.setOnFolder(id -> {
            discardEmptyLeaving();
            listPane.selectNote(null);
            refreshAll();
            selectFirstNote();
        });
        sidebar.setOnTag(tag -> {
            discardEmptyLeaving();
            listPane.selectNote(null);
            refreshAll();
            selectFirstNote();
        });
        sidebar.setOnNewFolder(this::createFolder);
        sidebar.setOnRename(this::renameFolder);
        sidebar.setOnDelete(this::deleteFolder);
        listPane.setOnSelect(this::selectNote);
        listPane.setOnDelete(note -> {
            listPane.selectNote(note.getId());
            deleteSelected();
        });
        listPane.setOnPin(note -> {
            service.setPinned(note.getId(), !note.isPinned());
            refreshAll();
        });
        listPane.setOnRestore(note -> {
            service.restoreNote(note.getId());
            sidebar.selectFolder(Folder.ALL_ID);
            listPane.selectNote(note.getId());
            refreshAll();
        });
        listPane.setOnMove((note, folderId) -> {
            service.moveNote(note.getId(), folderId);
            refreshAll();
        });
        editor.setChangeListener((html, text) -> {
            String id = editor.currentNoteId();
            if (id == null) {
                return;
            }
            service.updateContent(id, html, text);
            listPane.refreshPreviews(currentNotes());
            saveDebounce.playFromStart();
        });
        stage.setOnCloseRequest(e -> {
            discardEmptyLeaving();
            service.save();
        });
    }

    public void refreshAll() {
        sidebar.refresh();
        Folder folder = service.folder(sidebar.selectedFolderId()).orElse(Folder.all());
        List<Note> notes = currentNotes();
        listPane.render(folder, notes, sortMode, sidebar.selectedTag());
        boolean trash = Folder.DELETED_ID.equals(folder.getId());
        newNoteButton.setDisable(trash);
        recoverButton.setVisible(trash);
        recoverButton.setManaged(trash);
        pinButton.setVisible(!trash);
        pinButton.setManaged(!trash);
        restoreEditor();
        updateActionState();
    }

    private void restoreEditor() {
        String id = listPane.selectedNoteId();
        if (id == null) {
            editor.showEmpty();
            return;
        }
        Optional<Note> note = service.note(id);
        if (note.isEmpty()) {
            editor.showEmpty();
            listPane.selectNote(null);
            return;
        }
        editor.show(note.get());
    }

    private void selectInitial() {
        sidebar.selectFolder(Folder.ALL_ID);
        refreshAll();
        selectFirstNote();
    }

    private void selectFirstNote() {
        currentNotes().stream().findFirst().ifPresent(this::selectNote);
        if (currentNotes().isEmpty()) {
            editor.showEmpty();
            updateActionState();
        }
    }

    private void selectNote(Note note) {
        String previous = leavingNoteId;
        if (previous != null && !previous.equals(note.getId())) {
            service.discardIfEmpty(previous);
        }
        leavingNoteId = note.getId();
        listPane.selectNote(note.getId());
        listPane.render(
                service.folder(sidebar.selectedFolderId()).orElse(Folder.all()),
                currentNotes(),
                sortMode,
                sidebar.selectedTag());
        editor.show(note);
        updateActionState();
    }

    private void createNote() {
        if (Folder.DELETED_ID.equals(sidebar.selectedFolderId())) {
            return;
        }
        if (leavingNoteId != null) {
            service.discardIfEmpty(leavingNoteId);
        }
        Note created = service.createNote(sidebar.selectedFolderId());
        listPane.selectNote(created.getId());
        leavingNoteId = created.getId();
        refreshAll();
        editor.show(created);
        updateActionState();
    }

    private void createFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a folder");
        dialog.setContentText("Name:");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(name -> {
            Folder folder = service.createFolder(name);
            sidebar.selectFolder(folder.getId());
            refreshAll();
        });
    }

    private void renameFolder(Folder folder) {
        TextInputDialog dialog = new TextInputDialog(folder.getName());
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText("Rename “" + folder.getName() + "”");
        dialog.setContentText("Name:");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(name -> {
            service.renameFolder(folder.getId(), name);
            refreshAll();
        });
    }

    private void deleteFolder(Folder folder) {
        if (!confirm("Delete Folder", "Delete “" + folder.getName() + "” and move its notes to Recently Deleted?")) {
            return;
        }
        service.deleteFolder(folder.getId());
        sidebar.selectFolder(Folder.ALL_ID);
        refreshAll();
        selectFirstNote();
    }

    private void deleteSelected() {
        String id = listPane.selectedNoteId();
        if (id == null) {
            return;
        }
        Optional<Note> note = service.note(id);
        if (note.isEmpty()) {
            return;
        }
        if (note.get().isDeleted() && !confirm("Delete Permanently", "This note cannot be recovered.")) {
            return;
        }
        leavingNoteId = null;
        service.deleteNote(id);
        listPane.selectNote(null);
        refreshAll();
        selectFirstNote();
    }

    private void restoreSelected() {
        String id = listPane.selectedNoteId();
        if (id == null) {
            return;
        }
        service.restoreNote(id);
        sidebar.selectFolder(Folder.ALL_ID);
        listPane.selectNote(id);
        refreshAll();
    }

    private void togglePin() {
        String id = listPane.selectedNoteId();
        if (id == null) {
            return;
        }
        service.note(id).ifPresent(note -> {
            service.setPinned(id, !note.isPinned());
            refreshAll();
        });
    }

    private void setGallery(boolean gallery) {
        galleryButton.setSelected(gallery);
        listPane.setGalleryMode(gallery);
        refreshAll();
    }

    private void setSort(SortMode mode) {
        this.sortMode = mode;
        refreshAll();
    }

    private void toggleSidebar(boolean show) {
        sidebarButton.setSelected(show);
        sidebarButton.setTooltip(new Tooltip(show ? "Hide Folders" : "Show Folders"));
        if (show && !split.getItems().contains(sidebar)) {
            split.getItems().addFirst(sidebar);
            split.setDividerPositions(0.18, 0.42);
        } else if (!show) {
            split.getItems().remove(sidebar);
            split.setDividerPositions(0.32);
        }
    }

    private void toggleDark() {
        applyTheme(!dark);
    }

    private void applyTheme(boolean dark) {
        this.dark = dark;
        getStyleClass().remove("theme-dark");
        if (dark) {
            getStyleClass().add("theme-dark");
        }
        editor.setDark(dark);
        paintIcons();
    }

    private void paintIcons() {
        String ink = dark ? "#f5f5f7" : "#1d1d1f";
        sidebarButton.setGraphic(Icons.sidebar(ink));
        galleryButton.setGraphic(Icons.gallery(ink));
        newNoteButton.setGraphic(Icons.pencil("#3a2a00"));
        deleteButton.setGraphic(Icons.trash(ink));
        pinButton.setGraphic(Icons.pin(ink));
        recoverButton.setGraphic(Icons.recover(ink));
        formatButton.setGraphic(Icons.formatAa(ink));
        checklistButton.setGraphic(Icons.checklist(ink));
        tableButton.setGraphic(Icons.table(ink));
        sidebar.setInk(ink);
    }

    private static boolean detectDark() {
        String gtkTheme = System.getenv("GTK_THEME");
        if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
            return true;
        }
        try {
            Process process = new ProcessBuilder("gsettings", "get",
                    "org.gnome.desktop.interface", "color-scheme")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            return output.toLowerCase().contains("dark");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void discardEmptyLeaving() {
        if (leavingNoteId == null) {
            return;
        }
        String id = leavingNoteId;
        leavingNoteId = null;
        service.discardIfEmpty(id);
    }

    private void updateActionState() {
        boolean has = listPane.selectedNoteId() != null;
        deleteButton.setDisable(!has);
        pinButton.setDisable(!has || Folder.DELETED_ID.equals(sidebar.selectedFolderId()));
        recoverButton.setDisable(!has);
        service.note(listPane.selectedNoteId()).ifPresent(note ->
                pinButton.setTooltip(new Tooltip(note.isPinned() ? "Unpin" : "Pin")));
    }

    private List<Note> currentNotes() {
        return service.visibleNotes(sidebar.selectedFolderId(), search.getText(), sidebar.selectedTag());
    }

    private void revealDataFile() {
        try {
            var file = service.dataFile().path();
            if (file.getParent() != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.getParent().toFile());
            }
        } catch (Exception ignored) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, service.dataFile().path().toString(), ButtonType.OK);
            alert.setHeaderText("Notes are stored at");
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    private boolean confirm(String title, String body) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, body, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    private static MenuItem item(String label, KeyCode key, boolean shortcut, boolean shift, Runnable action) {
        MenuItem item = new MenuItem(label);
        KeyCombination.Modifier[] mods = shift
                ? new KeyCombination.Modifier[]{KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN}
                : new KeyCombination.Modifier[]{KeyCombination.SHORTCUT_DOWN};
        item.setAccelerator(new KeyCodeCombination(key, mods));
        item.setOnAction(e -> action.run());
        return item;
    }

    private static void styleToolButton(Button button, String tooltip) {
        button.getStyleClass().add("tool-btn");
        button.setTooltip(new Tooltip(tooltip));
    }

    private static Node vline() {
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPrefHeight(18);
        return separator;
    }
}
