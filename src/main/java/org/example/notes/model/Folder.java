package org.example.notes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Folder {
    public static final String ALL_ID = "all";
    public static final String DEFAULT_ID = "notes";
    public static final String DELETED_ID = "deleted";

    private String id;
    private String name;
    private FolderKind kind = FolderKind.USER;
    private int sortOrder;

    public Folder() {
    }

    public Folder(String id, String name, FolderKind kind, int sortOrder) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.sortOrder = sortOrder;
    }

    public static Folder all() {
        return new Folder(ALL_ID, "All Notes", FolderKind.ALL, -2);
    }

    public static Folder defaultNotes() {
        return new Folder(DEFAULT_ID, "Notes", FolderKind.DEFAULT, 0);
    }

    public static Folder deleted() {
        return new Folder(DELETED_ID, "Recently Deleted", FolderKind.DELETED, Integer.MAX_VALUE);
    }

    @JsonIgnore
    public boolean isSystem() {
        return kind != FolderKind.USER;
    }

    @JsonIgnore
    public boolean isVirtual() {
        return kind == FolderKind.ALL || kind == FolderKind.DELETED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FolderKind getKind() {
        return kind;
    }

    public void setKind(FolderKind kind) {
        this.kind = kind;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Folder folder)) {
            return false;
        }
        return Objects.equals(id, folder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
