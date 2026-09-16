package org.example.notes.service;

import org.example.notes.model.Note;

public sealed interface ListItem {
    record Header(String label) implements ListItem {
    }

    record NoteRow(Note note) implements ListItem {
    }
}
