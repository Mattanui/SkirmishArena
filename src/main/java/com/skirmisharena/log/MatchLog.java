package com.skirmisharena.log;

/** Receives the human-readable lines of a match (DESIGN.md §9). */
public interface MatchLog {

    void line(String text);
}
