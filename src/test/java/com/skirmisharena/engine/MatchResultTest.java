package com.skirmisharena.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchResultTest {

    @Test
    void aKoAlwaysHasAWinner() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult(Optional.empty(), 15, EndReason.KO, Map.of(Side.A, 30, Side.B, 0)));
    }

    @Test
    void damageMustBeGivenForBothSides() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult(Optional.empty(), 50, EndReason.TURN_LIMIT, Map.of(Side.A, 3)));
    }

    @Test
    void aMatchLastsFrom1To50Turns() {
        Map<Side, Integer> noDamage = Map.of(Side.A, 0, Side.B, 0);
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult(Optional.empty(), 51, EndReason.TURN_LIMIT, noDamage));
    }
}
