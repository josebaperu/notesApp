package org.example.notes.service;

import org.example.notes.model.Folder;
import org.example.notes.model.FolderKind;
import org.example.notes.model.Note;
import org.example.notes.model.NotesData;
import org.example.notes.persist.JsonRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;

public class NotesService {
    public static final Duration TRASH_RETENTION = Duration.ofDays(30);

    private final JsonRepository repository;
    private final Clock clock;
    private NotesData data = new NotesData();
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean seedIfEmpty = true;

    public NotesService(JsonRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void setSeedIfEmpty(boolean seedIfEmpty) {
        this.seedIfEmpty = seedIfEmpty;
    }

    public PathHandle dataFile() {
        return new PathHandle(repository.file());
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void load() {
        try {
            data = repository.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read notes file", e);
        }
        ensureSystemFolders();
        purgeExpiredTrash();
        if (seedIfEmpty && data.getNotes().isEmpty()) {
            seedWelcomeNotes();
            save();
        } else {
            save();
        }
        fire();
    }

    public void save() {
        try {
            repository.save(data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write notes file", e);
        }
    }

    public List<Folder> sidebarFolders() {
        List<Folder> folders = new ArrayList<>();
        folders.add(Folder.all());
        data.getFolders().stream()
                .filter(f -> f.getKind() != FolderKind.ALL && f.getKind() != FolderKind.DELETED)
                .sorted(Comparator.comparingInt(Folder::getSortOrder).thenComparing(Folder::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(folders::add);
        folders.add(Folder.deleted());
        return folders;
    }

    public List<Folder> movableFolders() {
        return data.getFolders().stream()
                .filter(f -> f.getKind() == FolderKind.DEFAULT || f.getKind() == FolderKind.USER)
                .sorted(Comparator.comparingInt(Folder::getSortOrder).thenComparing(Folder::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<Folder> folder(String id) {
        if (Folder.ALL_ID.equals(id)) {
            return Optional.of(Folder.all());
        }
        if (Folder.DELETED_ID.equals(id)) {
            return Optional.of(Folder.deleted());
        }
        return data.getFolders().stream().filter(f -> Objects.equals(f.getId(), id)).findFirst();
    }

    public int count(String folderId) {
        return (int) visibleNotes(folderId, "", null).size();
    }

    public List<Note> visibleNotes(String folderId, String query, String tag) {
        Predicate<Note> folderFilter = note -> matchesFolder(note, folderId);
        Predicate<Note> searchFilter = note -> matchesQuery(note, query);
        Predicate<Note> tagFilter = note -> tag == null || tag.isBlank() || Tags.contains(note.getPlainText(), tag);
        return data.getNotes().stream()
                .filter(folderFilter)
                .filter(searchFilter)
                .filter(tagFilter)
                .toList();
    }

    public Optional<Note> note(String id) {
        return data.getNotes().stream().filter(n -> Objects.equals(n.getId(), id)).findFirst();
    }

    public Set<String> allTags() {
        TreeSet<String> tags = new TreeSet<>();
        data.getNotes().stream()
                .filter(n -> !n.isDeleted())
                .map(Note::getPlainText)
                .map(Tags::extract)
                .forEach(tags::addAll);
        return tags;
    }

    public Note createNote(String folderId) {
        String target = resolveCreateFolder(folderId);
        Note note = new Note(UUID.randomUUID().toString(), target, clock.instant());
        note.setHtml("<h1><br></h1>");
        data.getNotes().add(note);
        fire();
        save();
        return note;
    }

    public void updateContent(String noteId, String html, String plainText) {
        note(noteId).ifPresent(note -> {
            note.setHtml(html == null ? "" : html);
            note.setPlainText(plainText == null ? "" : plainText);
            note.setTitle(titleFrom(plainText));
            note.setUpdatedAt(clock.instant());
        });
    }

    public void deleteNote(String noteId) {
        note(noteId).ifPresent(note -> {
            if (note.isDeleted()) {
                data.getNotes().remove(note);
            } else {
                note.setOriginalFolderId(note.getFolderId());
                note.setDeletedAt(clock.instant());
                note.setPinned(false);
                note.setUpdatedAt(clock.instant());
            }
            fire();
            save();
        });
    }

    public void restoreNote(String noteId) {
        note(noteId).ifPresent(note -> {
            if (!note.isDeleted()) {
                return;
            }
            String target = note.getOriginalFolderId();
            if (target == null || folder(target).isEmpty() || Folder.DELETED_ID.equals(target) || Folder.ALL_ID.equals(target)) {
                target = Folder.DEFAULT_ID;
            }
            note.setFolderId(target);
            note.setDeletedAt(null);
            note.setOriginalFolderId(null);
            note.setUpdatedAt(clock.instant());
            fire();
            save();
        });
    }

    public void emptyTrash() {
        data.getNotes().removeIf(Note::isDeleted);
        fire();
        save();
    }

    public void setPinned(String noteId, boolean pinned) {
        note(noteId).ifPresent(note -> {
            if (note.isDeleted()) {
                return;
            }
            note.setPinned(pinned);
            note.setUpdatedAt(clock.instant());
            fire();
            save();
        });
    }

    public void moveNote(String noteId, String folderId) {
        note(noteId).ifPresent(note -> {
            if (note.isDeleted() || Folder.ALL_ID.equals(folderId) || Folder.DELETED_ID.equals(folderId)) {
                return;
            }
            if (folder(folderId).isEmpty()) {
                return;
            }
            note.setFolderId(folderId);
            note.setUpdatedAt(clock.instant());
            fire();
            save();
        });
    }

    public boolean discardIfEmpty(String noteId) {
        Optional<Note> found = note(noteId);
        if (found.isEmpty()) {
            return false;
        }
        Note note = found.get();
        if (!note.isEmpty() || note.isDeleted()) {
            return false;
        }
        data.getNotes().remove(note);
        fire();
        save();
        return true;
    }

    public Folder createFolder(String name) {
        String trimmed = sanitizeFolderName(name);
        Folder folder = new Folder(UUID.randomUUID().toString(), trimmed, FolderKind.USER, nextSortOrder());
        data.getFolders().add(folder);
        fire();
        save();
        return folder;
    }

    public void renameFolder(String folderId, String name) {
        folder(folderId).ifPresent(folder -> {
            if (folder.getKind() != FolderKind.USER && folder.getKind() != FolderKind.DEFAULT) {
                return;
            }
            folder.setName(sanitizeFolderName(name));
            fire();
            save();
        });
    }

    public void deleteFolder(String folderId) {
        Optional<Folder> found = folder(folderId);
        if (found.isEmpty()) {
            return;
        }
        Folder folder = found.get();
        if (folder.getKind() != FolderKind.USER) {
            return;
        }
        Instant now = clock.instant();
        data.getNotes().stream()
                .filter(n -> Objects.equals(n.getFolderId(), folderId) && !n.isDeleted())
                .forEach(n -> {
                    n.setOriginalFolderId(folderId);
                    n.setDeletedAt(now);
                    n.setPinned(false);
                    n.setUpdatedAt(now);
                });
        data.getFolders().removeIf(f -> Objects.equals(f.getId(), folderId));
        fire();
        save();
    }

    private boolean matchesFolder(Note note, String folderId) {
        if (Folder.DELETED_ID.equals(folderId)) {
            return note.isDeleted();
        }
        if (note.isDeleted()) {
            return false;
        }
        if (Folder.ALL_ID.equals(folderId) || folderId == null) {
            return true;
        }
        return Objects.equals(note.getFolderId(), folderId);
    }

    private boolean matchesQuery(Note note, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase(Locale.ROOT).strip();
        String title = note.displayTitle().toLowerCase(Locale.ROOT);
        String body = note.getPlainText() == null ? "" : note.getPlainText().toLowerCase(Locale.ROOT);
        return title.contains(q) || body.contains(q);
    }

    private String resolveCreateFolder(String folderId) {
        if (folderId == null || Folder.ALL_ID.equals(folderId) || Folder.DELETED_ID.equals(folderId)) {
            return Folder.DEFAULT_ID;
        }
        return folder(folderId).map(Folder::getId).orElse(Folder.DEFAULT_ID);
    }

    private void ensureSystemFolders() {
        boolean hasDefault = data.getFolders().stream().anyMatch(f -> Folder.DEFAULT_ID.equals(f.getId()));
        if (!hasDefault) {
            data.getFolders().add(Folder.defaultNotes());
        }
        data.getFolders().removeIf(f -> f.getKind() == FolderKind.ALL || f.getKind() == FolderKind.DELETED);
        for (Folder folder : data.getFolders()) {
            if (Folder.DEFAULT_ID.equals(folder.getId())) {
                folder.setKind(FolderKind.DEFAULT);
                if (folder.getName() == null || folder.getName().isBlank()) {
                    folder.setName("Notes");
                }
            } else if (folder.getKind() == null) {
                folder.setKind(FolderKind.USER);
            }
        }
    }

    private void purgeExpiredTrash() {
        Instant cutoff = clock.instant().minus(TRASH_RETENTION);
        data.getNotes().removeIf(n -> n.getDeletedAt() != null && n.getDeletedAt().isBefore(cutoff));
    }

    private void seedWelcomeNotes() {
        Instant now = clock.instant();
        Folder personal = new Folder(UUID.randomUUID().toString(), "Personal", FolderKind.USER, 1);
        data.getFolders().add(personal);

        Note welcome = new Note(UUID.randomUUID().toString(), Folder.DEFAULT_ID, now);
        welcome.setPinned(true);
        welcome.setTitle("Welcome to Notes");
        welcome.setHtml("""
                <h1>Welcome to Notes</h1>
                <p>A Mac Notes-style notebook that lives entirely on this computer. Everything is stored in a JSON file, so your notes stay with you.</p>
                <h2>Get started</h2>
                <p>Press <b>Ctrl+N</b> for a new note. The first line is the title — keep typing and the rest becomes the body.</p>
                <ul>
                <li>Pin important notes to keep them at the top</li>
                <li>Group related notes in folders</li>
                <li>Tag notes in the body, for example #ideas</li>
                </ul>
                <p>Try a tag: #welcome</p>
                """);
        welcome.setPlainText("""
                Welcome to Notes
                A Mac Notes-style notebook that lives entirely on this computer. Everything is stored in a JSON file, so your notes stay with you.
                Get started
                Press Ctrl+N for a new note. The first line is the title — keep typing and the rest becomes the body.
                Pin important notes to keep them at the top
                Group related notes in folders
                Tag notes in the body, for example #ideas
                Try a tag: #welcome
                """);
        welcome.setUpdatedAt(now);

        Note meeting = new Note(UUID.randomUUID().toString(), Folder.DEFAULT_ID, now.minus(Duration.ofHours(5)));
        meeting.setTitle("Staff meeting");
        meeting.setHtml("""
                <h1>Staff meeting</h1>
                <h3>Tuesday</h3>
                <ul>
                <li>Review the Q3 timeline</li>
                <li>Demo the notes prototype</li>
                <li>Assign follow-ups</li>
                </ul>
                <p>Next steps live in #work</p>
                """);
        meeting.setPlainText("""
                Staff meeting
                Tuesday
                Review the Q3 timeline
                Demo the notes prototype
                Assign follow-ups
                Next steps live in #work
                """);
        meeting.setUpdatedAt(now.minus(Duration.ofHours(5)));
        meeting.setCreatedAt(now.minus(Duration.ofDays(1)));

        Note shopping = new Note(UUID.randomUUID().toString(), personal.getId(), now.minus(Duration.ofDays(2)));
        shopping.setTitle("Shopping list");
        shopping.setHtml("""
                <h1>Shopping list</h1>
                <div class="checklist-item"><input type="checkbox" checked><span class="cl-text">Oat milk</span></div>
                <div class="checklist-item"><input type="checkbox"><span class="cl-text">Sourdough</span></div>
                <div class="checklist-item"><input type="checkbox"><span class="cl-text">Lemons</span></div>
                <div class="checklist-item"><input type="checkbox"><span class="cl-text">Coffee beans</span></div>
                <p>#personal #errands</p>
                """);
        shopping.setPlainText("""
                Shopping list
                Oat milk
                Sourdough
                Lemons
                Coffee beans
                #personal #errands
                """);
        shopping.setUpdatedAt(now.minus(Duration.ofDays(2)));
        shopping.setCreatedAt(now.minus(Duration.ofDays(3)));

        data.getNotes().addAll(List.of(welcome, meeting, shopping));
    }

    private int nextSortOrder() {
        return data.getFolders().stream().mapToInt(Folder::getSortOrder).max().orElse(0) + 1;
    }

    private static String sanitizeFolderName(String name) {
        String trimmed = name == null ? "" : name.strip();
        return trimmed.isEmpty() ? "New Folder" : trimmed;
    }

    static String titleFrom(String plainText) {
        if (plainText == null) {
            return "New Note";
        }
        for (String line : plainText.replace('\u200B', ' ').split("\\R")) {
            String stripped = line.strip();
            if (!stripped.isEmpty()) {
                return stripped.length() > 120 ? stripped.substring(0, 120) : stripped;
            }
        }
        return "New Note";
    }

    private void fire() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    public record PathHandle(java.nio.file.Path path) {
    }
}
