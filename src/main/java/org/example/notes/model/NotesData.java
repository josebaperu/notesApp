package org.example.notes.model;

import java.util.ArrayList;
import java.util.List;

public class NotesData {
    private int version = 1;
    private List<Folder> folders = new ArrayList<>();
    private List<Note> notes = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders == null ? new ArrayList<>() : folders;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes == null ? new ArrayList<>() : notes;
    }
}
