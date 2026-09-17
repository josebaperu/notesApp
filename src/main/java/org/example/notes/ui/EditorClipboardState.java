package org.example.notes.ui;

final class EditorClipboardState {
    private EditorClipboardState() {
    }

    static boolean cutEnabled(boolean readOnly, boolean hasSelection) {
        return !readOnly && hasSelection;
    }

    static boolean copyEnabled(boolean hasSelection) {
        return hasSelection;
    }

    static boolean pasteEnabled(boolean readOnly, boolean clipboardHasContent) {
        return !readOnly && clipboardHasContent;
    }
}
