package org.example.notes.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagsTest {
    @Test
    void extractsHashtagsInOrderWithoutDuplicates() {
        Set<String> tags = Tags.extract("Hello #Work and #personal, plus #work again");
        assertEquals(Set.of("work", "personal"), tags);
    }

    @Test
    void ignoresHashInTheMiddleOfWords() {
        assertTrue(Tags.extract("c# and issue#12").isEmpty());
        assertEquals(Set.of("errands"), Tags.extract("Buy milk #errands"));
    }

    @Test
    void containsIsCaseInsensitive() {
        assertTrue(Tags.contains("See you #Welcome", "welcome"));
        assertFalse(Tags.contains("No tags here", "welcome"));
    }
}
