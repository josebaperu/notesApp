package org.example.notes.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class FolderReveal {
    private FolderReveal() {
    }

    public static void reveal(Path file) throws IOException {
        if (file == null) {
            throw new IOException("Storage file is not set");
        }
        Path absolute = file.toAbsolutePath().normalize();
        Path directory = absolute.getParent();
        if (directory == null) {
            throw new IOException("Storage directory is not set");
        }
        Files.createDirectories(directory);

        IOException last = new IOException("No file manager command was available");
        for (List<String> command : commandsFor(System.getProperty("os.name"), absolute)) {
            try {
                if (launch(command, windowsExplorer(command))) {
                    return;
                }
                last = new IOException("File manager command exited unsuccessfully: " + command);
            } catch (IOException e) {
                last = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while opening the storage folder", e);
            }
        }
        throw last;
    }

    static List<List<String>> commandsFor(String osName, Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        Path directory = absolute.getParent();
        String filePath = absolute.toString();
        String dirPath = directory.toString();
        String fileUri = absolute.toUri().toString();
        String dirUri = directory.toUri().toString();
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);

        List<List<String>> commands = new ArrayList<>();
        if (os.contains("win")) {
            commands.add(List.of("explorer.exe", "/select," + filePath));
            commands.add(List.of("explorer.exe", dirPath));
            commands.add(List.of("cmd.exe", "/c", "start", "", dirPath));
            return commands;
        }
        if (os.contains("mac")) {
            commands.add(List.of("open", "-R", filePath));
            commands.add(List.of("open", dirPath));
            return commands;
        }
        commands.add(List.of(
                "gdbus", "call", "--session",
                "--dest", "org.freedesktop.FileManager1",
                "--object-path", "/org/freedesktop/FileManager1",
                "--method", "org.freedesktop.FileManager1.ShowItems",
                "['" + fileUri + "']", ""
        ));
        commands.add(List.of(
                "dbus-send", "--session", "--type=method_call",
                "--dest=org.freedesktop.FileManager1",
                "/org/freedesktop/FileManager1",
                "org.freedesktop.FileManager1.ShowItems",
                "array:string:" + fileUri,
                "string:"
        ));
        commands.add(List.of(
                "gdbus", "call", "--session",
                "--dest", "org.freedesktop.FileManager1",
                "--object-path", "/org/freedesktop/FileManager1",
                "--method", "org.freedesktop.FileManager1.ShowFolders",
                "['" + dirUri + "']", ""
        ));
        commands.add(List.of("xdg-open", dirPath));
        commands.add(List.of("gio", "open", dirPath));
        commands.add(List.of("exo-open", "--launch", "FileManager", dirPath));
        commands.add(List.of("thunar", "--window", dirPath));
        commands.add(List.of("nautilus", dirPath));
        commands.add(List.of("dolphin", dirPath));
        commands.add(List.of("pcmanfm", dirPath));
        return commands;
    }

    private static boolean launch(List<String> command, boolean treatAnyStartAsSuccess)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (treatAnyStartAsSuccess) {
            return true;
        }
        boolean finished = process.waitFor(1500, TimeUnit.MILLISECONDS);
        if (!finished) {
            return true;
        }
        return process.exitValue() == 0;
    }

    private static boolean windowsExplorer(List<String> command) {
        return !command.isEmpty() && command.getFirst().toLowerCase(Locale.ROOT).contains("explorer");
    }
}
