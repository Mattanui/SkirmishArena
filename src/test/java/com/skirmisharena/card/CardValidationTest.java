package com.skirmisharena.card;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardValidationTest {

    @Test
    void negativeCostIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AttackCard("Jab", -1, 1));
    }

    @Test
    void zeroCostIsAccepted() {
        assertEquals(0, new ResourceCard("Focus", 0, 1).cost());
    }

    @Test
    void zeroOrNegativeValuesAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new AttackCard("Jab", 1, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ResourceCard("Focus", 0, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new DrawCard("Insight", 1, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new StealCard("Pickpocket", 2, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new HealCard("Bandage", 1, -1)),
                () -> assertThrows(IllegalArgumentException.class, () -> DefenseCard.reduce("Guard", 1, 0, 2)),
                () -> assertThrows(IllegalArgumentException.class, () -> DefenseCard.reduce("Guard", 1, 1, 0)));
    }

    @Test
    void halveAndBlockTakeNoAmount() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DefenseCard("Barrier", 3, DefenseKind.HALVE, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DefenseCard("Aegis", 5, DefenseKind.BLOCK, 1, 1)));
    }

    @Test
    void blankOrMissingNameIsRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new AttackCard(" ", 1, 1)),
                () -> assertThrows(NullPointerException.class, () -> new AmplifyCard(null, 5)));
    }

    @Test
    void missingDefenseKindIsRejected() {
        assertThrows(NullPointerException.class, () -> new DefenseCard("Guard", 1, null, 1, 2));
    }
}
