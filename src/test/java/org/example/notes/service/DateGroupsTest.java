package org.example.notes.service;

import org.example.notes.model.Note;
import org.example.notes.model.SortMode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DateGroupsTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-16T15:00:00Z"), ZoneId.of("UTC"));

    @Test
    void groupsPinnedThenTodayAndYesterday() {
        Note pinned = note("1", "Pinned idea", Instant.parse("2026-09-10T10:00:00Z"), true);
        Note today = note("2", "Today note", Instant.parse("2026-09-16T09:00:00Z"), false);
        Note yesterday = note("3", "Yesterday note", Instant.parse("2026-09-15T09:00:00Z"), false);

        List<ListItem> items = DateGroups.build(List.of(yesterday, today, pinned), SortMode.DATE_EDITED, clock);

        assertEquals(6, items.size());
        assertEquals("Pinned", ((ListItem.Header) items.get(0)).label());
        assertEquals("1", ((ListItem.NoteRow) items.get(1)).note().getId());
        assertEquals("Today", ((ListItem.Header) items.get(2)).label());
        assertEquals("2", ((ListItem.NoteRow) items.get(3)).note().getId());
        assertEquals("Yesterday", ((ListItem.Header) items.get(4)).label());
        assertEquals("3", ((ListItem.NoteRow) items.get(5)).note().getId());
    }

    @Test
    void titleSortSkipsDateHeadersExceptPinned() {
        Note a = note("a", "Alpha", Instant.parse("2026-09-16T09:00:00Z"), false);
        Note b = note("b", "Beta", Instant.parse("2026-09-01T09:00:00Z"), true);
        List<ListItem> items = DateGroups.build(List.of(a, b), SortMode.TITLE, clock);
        assertInstanceOf(ListItem.Header.class, items.get(0));
        assertEquals("Pinned", ((ListItem.Header) items.get(0)).label());
        assertEquals("Notes", ((ListItem.Header) items.get(2)).label());
        assertEquals("Alpha", ((ListItem.NoteRow) items.get(3)).note().getTitle());
    }

    @Test
    void previewStripsTitleFromBody() {
        Note note = note("1", "Shopping list", Instant.parse("2026-09-16T09:00:00Z"), false);
        note.setPlainText("Shopping list\nMilk and bread");
        assertEquals("Milk and bread", DateGroups.preview(note));
    }

    private static Note note(String id, String title, Instant updated, boolean pinned) {
        Note note = new Note(id, "notes", updated);
        note.setTitle(title);
        note.setPlainText(title);
        note.setUpdatedAt(updated);
        note.setPinned(pinned);
        return note;
    }
}
