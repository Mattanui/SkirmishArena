package com.skirmisharena.log;

/** Discards every line: bulk runs of N matches only need the results, not the logs. */
public final class NoMatchLog implements MatchLog {

    @Override
    public void line(String text) {
        // Intentionally empty: nothing is kept.
    }
}
