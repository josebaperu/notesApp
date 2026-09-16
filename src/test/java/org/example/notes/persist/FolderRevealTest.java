package org.example.notes.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderRevealTest {
    @TempDir
    Path temp;

    @Test
    void windowsSelectsTheFileInExplorer() {
        Path file = temp.resolve("NotesApp").resolve("notes.json");
        String filePath = file.toAbsolutePath().normalize().toString();
        String dirPath = file.getParent().toAbsolutePath().normalize().toString();
        List<List<String>> commands = FolderReveal.commandsFor("Windows 11", file);
        assertEquals(List.of("explorer.exe", "/select," + filePath), commands.getFirst());
        assertEquals(List.of("explorer.exe", dirPath), commands.get(1));
    }

    @Test
    void macRevealsInFinder() {
        Path file = temp.resolve("NotesApp").resolve("notes.json");
        String filePath = file.toAbsolutePath().normalize().toString();
        List<List<String>> commands = FolderReveal.commandsFor("Mac OS X", file);
        assertEquals(List.of("open", "-R", filePath), commands.getFirst());
    }

    @Test
    void linuxUsesFreedesktopFileManagerThenXdgOpen() {
        Path file = temp.resolve("notes-app").resolve("notes.json");
        List<List<String>> commands = FolderReveal.commandsFor("Linux", file);
        assertEquals("gdbus", commands.getFirst().getFirst());
        assertTrue(commands.getFirst().contains("org.freedesktop.FileManager1.ShowItems"));
        assertTrue(commands.stream().anyMatch(command -> command.getFirst().equals("xdg-open")));
    }
}
