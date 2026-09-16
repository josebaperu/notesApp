package org.example.notes.service;

import org.example.notes.model.Note;
import org.example.notes.model.SortMode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class DateGroups {
    private DateGroups() {
    }

    public static List<ListItem> build(List<Note> notes, SortMode sortMode, Clock clock) {
        if (notes == null || notes.isEmpty()) {
            return List.of();
        }
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);

        List<Note> pinned = notes.stream()
                .filter(Note::isPinned)
                .sorted(noteComparator(sortMode))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Note> rest = notes.stream()
                .filter(n -> !n.isPinned())
                .sorted(noteComparator(sortMode))
                .collect(Collectors.toCollection(ArrayList::new));

        List<ListItem> items = new ArrayList<>();
        if (!pinned.isEmpty()) {
            items.add(new ListItem.Header("Pinned"));
            pinned.forEach(n -> items.add(new ListItem.NoteRow(n)));
        }
        if (sortMode == SortMode.TITLE) {
            if (!rest.isEmpty() && !pinned.isEmpty()) {
                items.add(new ListItem.Header("Notes"));
            }
            rest.forEach(n -> items.add(new ListItem.NoteRow(n)));
            return items;
        }

        String lastHeader = null;
        for (Note note : rest) {
            String header = headerFor(note, sortMode, today, zone);
            if (!header.equals(lastHeader)) {
                items.add(new ListItem.Header(header));
                lastHeader = header;
            }
            items.add(new ListItem.NoteRow(note));
        }
        return items;
    }

    public static String listTimestamp(Note note, Clock clock) {
        Instant instant = note.getUpdatedAt() == null ? clock.instant() : note.getUpdatedAt();
        ZoneId zone = clock.getZone();
        LocalDate date = instant.atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(clock);
        long days = ChronoUnit.DAYS.between(date, today);
        if (days == 0) {
            return DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant);
        }
        if (days == 1) {
            return "Yesterday";
        }
        if (days < 7) {
            return DateTimeFormatter.ofPattern("EEEE", Locale.UK).format(date);
        }
        if (date.getYear() == today.getYear()) {
            return DateTimeFormatter.ofPattern("d MMM", Locale.UK).format(date);
        }
        return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK).format(date);
    }

    public static String editorTimestamp(Note note, Clock clock) {
        Instant instant = note.getCreatedAt() == null ? clock.instant() : note.getCreatedAt();
        return DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm", Locale.UK)
                .withZone(clock.getZone())
                .format(instant);
    }

    public static String preview(Note note) {
        String title = note.displayTitle();
        String text = note.getPlainText() == null ? "" : note.getPlainText().replace('\u200B', ' ').strip();
        if (text.startsWith(title)) {
            text = text.substring(title.length()).strip();
        }
        text = text.replaceAll("\\s+", " ");
        return text;
    }

    static String headerFor(Note note, SortMode sortMode, LocalDate today, ZoneId zone) {
        Instant instant = sortMode == SortMode.DATE_CREATED ? note.getCreatedAt() : note.getUpdatedAt();
        if (instant == null) {
            return "Today";
        }
        LocalDate date = instant.atZone(zone).toLocalDate();
        long days = ChronoUnit.DAYS.between(date, today);
        if (days <= 0) {
            return "Today";
        }
        if (days == 1) {
            return "Yesterday";
        }
        if (days <= 7) {
            return "Previous 7 Days";
        }
        if (days <= 30) {
            return "Previous 30 Days";
        }
        if (date.getYear() == today.getYear()) {
            return DateTimeFormatter.ofPattern("MMMM", Locale.UK).format(date);
        }
        YearMonth month = YearMonth.from(date);
        if (month.getYear() == today.getYear() - 1 && days < 400) {
            return DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK).format(date);
        }
        return String.valueOf(date.getYear());
    }

    private static Comparator<Note> noteComparator(SortMode sortMode) {
        return switch (sortMode) {
            case TITLE -> Comparator.comparing((Note n) -> n.displayTitle().toLowerCase(Locale.ROOT))
                    .thenComparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case DATE_CREATED -> Comparator.comparing(Note::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case DATE_EDITED -> Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Note::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
}
