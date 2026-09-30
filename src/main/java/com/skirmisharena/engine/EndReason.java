package com.skirmisharena.engine;

/** Why a match ended (DESIGN.md §6). */
public enum EndReason {
    /** A champion dropped to 0 HP. */
    KO,
    /** 50 turns were played; the higher HP wins, equal HP is a draw. */
    TURN_LIMIT
}
