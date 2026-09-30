package com.skirmisharena.engine;

/** The game numbers of DESIGN.md. Change a value here and in DESIGN.md in the same commit. */
public final class GameRules {

    /** A champion's HP is also its maximum: healing never goes above it (DESIGN.md §1). */
    public static final int MAX_HP = 30;
    public static final int STARTING_HP = MAX_HP;

    public static final int STARTING_HAND_SIZE = 3;
    public static final int HAND_LIMIT = 7;

    /** Cards picked at random from the 27-card pool for each champion (DESIGN.md §2). */
    public static final int DECK_SIZE = 20;

    /** Cards drawn in the Draw phase of each turn (DESIGN.md §3). */
    public static final int CARDS_DRAWN_PER_TURN = 1;

    /** Total turns of a match, both champions together: 25 each (DESIGN.md §6). */
    public static final int MAX_TURNS = 50;

    public static final int MANA_CAP = 10;

    /** The Defensive bot switches to defense and healing when its HP is strictly below this (DESIGN.md §7). */
    public static final int DEFENSIVE_THRESHOLD = 15;

    private GameRules() {
    }
}
