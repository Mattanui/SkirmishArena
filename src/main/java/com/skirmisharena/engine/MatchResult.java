package com.skirmisharena.engine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of one match (DESIGN.md §6 and §9). {@code damageDealt} is the damage each side
 * actually dealt, after defense, overkill included.
 */
public record MatchResult(Optional<Side> winner, int turnsPlayed, EndReason endReason, Map<Side, Integer> damageDealt) {

    public MatchResult {
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(endReason, "endReason");
        if (turnsPlayed < 1 || turnsPlayed > GameRules.MAX_TURNS) {
            throw new IllegalArgumentException("turnsPlayed must be 1 to " + GameRules.MAX_TURNS + ", was " + turnsPlayed);
        }
        if (endReason == EndReason.KO && winner.isEmpty()) {
            throw new IllegalArgumentException("a KO always has a winner");
        }
        for (Side side : Side.values()) {
            if (!damageDealt.containsKey(side)) {
                throw new IllegalArgumentException("damage dealt is missing for side " + side);
            }
        }
        damageDealt = Collections.unmodifiableMap(new EnumMap<>(damageDealt));
    }

    public boolean isDraw() {
        return winner.isEmpty();
    }

    public int damageDealtBy(Side side) {
        return damageDealt.get(side);
    }
}
