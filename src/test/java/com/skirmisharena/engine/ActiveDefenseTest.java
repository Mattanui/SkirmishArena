package com.skirmisharena.engine;

import com.skirmisharena.card.DefenseKind;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveDefenseTest {

    @Test
    void reduceTakesItsAmountOffEachAttackCardButNeverBelowZero() {
        ActiveDefense guard = new ActiveDefense("Guard", DefenseKind.REDUCE, 1, 2);
        ActiveDefense shield = new ActiveDefense("Shield", DefenseKind.REDUCE, 2, 2);

        assertAll(
                () -> assertEquals(1, guard.absorb(2)),
                () -> assertEquals(0, guard.absorb(1)),
                () -> assertEquals(6, shield.absorb(8)),
                () -> assertEquals(0, shield.absorb(1)));
    }

    @Test
    void halveRoundsDown() {
        ActiveDefense barrier = new ActiveDefense("Barrier", DefenseKind.HALVE, 0, 2);

        assertAll(
                () -> assertEquals(0, barrier.absorb(1)),
                () -> assertEquals(1, barrier.absorb(2)),
                () -> assertEquals(1, barrier.absorb(3)),
                () -> assertEquals(2, barrier.absorb(4)),
                () -> assertEquals(4, barrier.absorb(8)));
    }

    @Test
    void blockStopsEverything() {
        assertEquals(0, new ActiveDefense("Aegis", DefenseKind.BLOCK, 0, 1).absorb(16));
    }

    @Test
    void countDownRemovesOneTurnAndEndsTheDefenseAtZero() {
        ActiveDefense guard = new ActiveDefense("Guard", DefenseKind.REDUCE, 1, 2);

        Optional<ActiveDefense> afterOneTurn = guard.countDown();

        assertEquals(Optional.of(new ActiveDefense("Guard", DefenseKind.REDUCE, 1, 1)), afterOneTurn);
        assertEquals(Optional.empty(), afterOneTurn.get().countDown());
    }

    @Test
    void invalidValuesAreRejected() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new ActiveDefense(null, DefenseKind.REDUCE, 1, 2)),
                () -> assertThrows(NullPointerException.class, () -> new ActiveDefense("Guard", null, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ActiveDefense("Guard", DefenseKind.REDUCE, -1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ActiveDefense("Aegis", DefenseKind.BLOCK, 0, 0)));
    }
}
