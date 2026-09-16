package org.example.notes.service;

import org.example.notes.model.Folder;
import org.example.notes.model.Note;
import org.example.notes.persist.JsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesServiceTest {
    @TempDir
    Path temp;

    private NotesService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);
        service = new NotesService(new JsonRepository(temp.resolve("notes.json")), clock);
        service.setSeedIfEmpty(false);
        service.load();
    }

    @Test
    void createsAndPersistsANote() {
        Note note = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(note.getId(), "<h1>Hello</h1>", "Hello");
        service.save();

        NotesService reloaded = new NotesService(new JsonRepository(temp.resolve("notes.json")),
                Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC));
        reloaded.setSeedIfEmpty(false);
        reloaded.load();
        Note loaded = reloaded.note(note.getId()).orElseThrow();
        assertEquals("Hello", loaded.getTitle());
        assertEquals("<h1>Hello</h1>", loaded.getHtml());
    }

    @Test
    void deleteMovesToTrashAndRestoreBringsItBack() {
        Note note = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(note.getId(), "<p>Keep me</p>", "Keep me");
        service.deleteNote(note.getId());
        assertEquals(1, service.visibleNotes(Folder.DELETED_ID, "", null).size());
        assertTrue(service.visibleNotes(Folder.ALL_ID, "", null).isEmpty());

        service.restoreNote(note.getId());
        assertEquals("Keep me", service.visibleNotes(Folder.ALL_ID, "", null).getFirst().getTitle());
        assertTrue(service.visibleNotes(Folder.DELETED_ID, "", null).isEmpty());
    }

    @Test
    void emptyNotesAreDiscarded() {
        Note note = service.createNote(Folder.DEFAULT_ID);
        assertTrue(service.discardIfEmpty(note.getId()));
        assertTrue(service.note(note.getId()).isEmpty());
    }

    @Test
    void searchAndTagsFilterNotes() {
        Note work = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(work.getId(), "<p>Ship it #work</p>", "Ship it #work");
        Note home = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(home.getId(), "<p>Garden #home</p>", "Garden #home");

        List<Note> found = service.visibleNotes(Folder.ALL_ID, "garden", null);
        assertEquals(1, found.size());
        assertEquals(home.getId(), found.getFirst().getId());
        assertEquals(1, service.visibleNotes(Folder.ALL_ID, "", "work").size());
        assertTrue(service.allTags().contains("work"));
        assertTrue(service.allTags().contains("home"));
    }

    @Test
    void deletingAFolderTrashesItsNotes() {
        Folder folder = service.createFolder("Projects");
        Note note = service.createNote(folder.getId());
        service.updateContent(note.getId(), "<p>Spec</p>", "Spec");
        service.deleteFolder(folder.getId());
        assertTrue(service.folder(folder.getId()).isEmpty());
        assertTrue(service.note(note.getId()).orElseThrow().isDeleted());
    }

    @Test
    void readOnlyNotesRejectContentUpdatesAndPersist() {
        Note note = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(note.getId(), "<p>Keep</p>", "Keep");
        service.setReadOnly(note.getId(), true);
        service.updateContent(note.getId(), "<p>Changed</p>", "Changed");
        Note locked = service.note(note.getId()).orElseThrow();
        assertTrue(locked.isReadOnly());
        assertEquals("Keep", locked.getTitle());
        assertEquals("<p>Keep</p>", locked.getHtml());

        service.save();
        NotesService reloaded = new NotesService(new JsonRepository(temp.resolve("notes.json")),
                Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC));
        reloaded.setSeedIfEmpty(false);
        reloaded.load();
        Note loaded = reloaded.note(note.getId()).orElseThrow();
        assertTrue(loaded.isReadOnly());
        assertEquals("Keep", loaded.getTitle());

        service.setReadOnly(note.getId(), false);
        service.updateContent(note.getId(), "<p>Changed</p>", "Changed");
        assertEquals("Changed", service.note(note.getId()).orElseThrow().getTitle());
    }

    @Test
    void emptyReadOnlyNotesAreNotDiscarded() {
        Note note = service.createNote(Folder.DEFAULT_ID);
        service.setReadOnly(note.getId(), true);
        assertFalse(service.discardIfEmpty(note.getId()));
        assertTrue(service.note(note.getId()).isPresent());
    }

    @Test
    void pinAndMove() {
        Folder folder = service.createFolder("Personal");
        Note note = service.createNote(Folder.DEFAULT_ID);
        service.updateContent(note.getId(), "<p>Idea</p>", "Idea");
        service.setPinned(note.getId(), true);
        service.moveNote(note.getId(), folder.getId());
        Note moved = service.note(note.getId()).orElseThrow();
        assertTrue(moved.isPinned());
        assertEquals(folder.getId(), moved.getFolderId());
        assertFalse(service.visibleNotes(Folder.DEFAULT_ID, "", null).contains(moved));
    }
}
