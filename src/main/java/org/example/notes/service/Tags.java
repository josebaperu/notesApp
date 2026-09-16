package org.example.notes.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tags {
    private static final Pattern TAG = Pattern.compile("(?<![\\w])#([A-Za-z][A-Za-z0-9_]{0,31})");

    private Tags() {
    }

    public static Set<String> extract(String plainText) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (plainText == null || plainText.isBlank()) {
            return tags;
        }
        Matcher matcher = TAG.matcher(plainText);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tags;
    }

    public static boolean contains(String plainText, String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        return extract(plainText).contains(tag.toLowerCase(Locale.ROOT));
    }
}
