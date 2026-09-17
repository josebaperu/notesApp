package org.example.notes.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorClipboardStateTest {
    @Test
    void cutIsGreyedOutWhenReadOnlyEvenIfTextIsSelected() {
        assertFalse(EditorClipboardState.cutEnabled(true, true));
        assertFalse(EditorClipboardState.cutEnabled(true, false));
    }

    @Test
    void cutIsAvailableOnlyWhenEditableWithASelection() {
        assertTrue(EditorClipboardState.cutEnabled(false, true));
        assertFalse(EditorClipboardState.cutEnabled(false, false));
    }

    @Test
    void copyStaysAvailableInReadOnlyWhenTextIsSelected() {
        assertTrue(EditorClipboardState.copyEnabled(true));
        assertFalse(EditorClipboardState.copyEnabled(false));
    }

    @Test
    void pasteIsGreyedOutWhenReadOnlyOrClipboardIsEmpty() {
        assertFalse(EditorClipboardState.pasteEnabled(true, true));
        assertFalse(EditorClipboardState.pasteEnabled(true, false));
        assertTrue(EditorClipboardState.pasteEnabled(false, true));
        assertFalse(EditorClipboardState.pasteEnabled(false, false));
    }
}
