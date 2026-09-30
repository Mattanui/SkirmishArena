package com.skirmisharena.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRulesTest {

    /** Guards the numbers of DESIGN.md §1 against an accidental change. */
    @Test
    void gameNumbersMatchTheDesign() {
        assertAll(
                () -> assertEquals(30, GameRules.MAX_HP),
                () -> assertEquals(30, GameRules.STARTING_HP),
                () -> assertEquals(3, GameRules.STARTING_HAND_SIZE),
                () -> assertEquals(7, GameRules.HAND_LIMIT),
                () -> assertEquals(20, GameRules.DECK_SIZE),
                () -> assertEquals(1, GameRules.CARDS_DRAWN_PER_TURN),
                () -> assertEquals(50, GameRules.MAX_TURNS),
                () -> assertEquals(10, GameRules.MANA_CAP),
                () -> assertEquals(15, GameRules.DEFENSIVE_THRESHOLD));
    }
}
