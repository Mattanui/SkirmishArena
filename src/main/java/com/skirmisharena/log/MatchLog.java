package com.skirmisharena.log;

/** Receives the human-readable lines of a match (DESIGN.md §9). */
public interface MatchLog {

    void line(String text);

    /**
     * False when lines are thrown away (bulk runs). The engine checks it before building a line,
     * so running thousands of matches does not pay for text nobody reads.
     */
    default boolean enabled() {
        return true;
    }
}
