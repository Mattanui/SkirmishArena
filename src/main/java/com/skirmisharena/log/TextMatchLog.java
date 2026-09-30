package com.skirmisharena.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Keeps every line in memory, in order. Used for the sample match and in tests. */
public final class TextMatchLog implements MatchLog {

    private final List<String> lines = new ArrayList<>();

    @Override
    public void line(String text) {
        lines.add(Objects.requireNonNull(text, "text"));
    }

    /** Copy of the lines written so far. */
    public List<String> lines() {
        return List.copyOf(lines);
    }

    /** The whole log, one line per row, with "\n" whatever the operating system (same output everywhere). */
    public String text() {
        return String.join("\n", lines);
    }
}
