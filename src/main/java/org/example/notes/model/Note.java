package org.example.notes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Objects;

public class Note {
    private String id;
    private String folderId;
    private String title = "New Note";
    private String html = "";
    private String plainText = "";
    private boolean pinned;
    private boolean readOnly;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private String originalFolderId;

    public Note() {
    }

    public Note(String id, String folderId, Instant now) {
        this.id = id;
        this.folderId = folderId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @JsonIgnore
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @JsonIgnore
    public boolean isEmpty() {
        String text = plainText == null ? "" : plainText.replace("\u200B", "").strip();
        return text.isEmpty();
    }

    @JsonIgnore
    public String displayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return "New Note";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getOriginalFolderId() {
        return originalFolderId;
    }

    public void setOriginalFolderId(String originalFolderId) {
        this.originalFolderId = originalFolderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Note note)) {
            return false;
        }
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
