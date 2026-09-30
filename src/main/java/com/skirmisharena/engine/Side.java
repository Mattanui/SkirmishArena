package com.skirmisharena.engine;

/** The two seats of a match. Side A is the first bot given on the command line (DESIGN.md §2). */
public enum Side {
    A,
    B;

    public Side opponent() {
        return this == A ? B : A;
    }
}
