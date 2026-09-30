package com.skirmisharena.engine;

import com.skirmisharena.card.DefenseKind;

import java.util.Objects;

/**
 * The defense currently protecting a champion (DESIGN.md §5). {@code turnsLeft} counts the
 * opponent's turns still covered. Used from step 6 of PLAN.md.
 */
public record ActiveDefense(DefenseKind kind, int amount, int turnsLeft) {

    public ActiveDefense {
        Objects.requireNonNull(kind, "kind");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0, was " + amount);
        }
        if (turnsLeft <= 0) {
            throw new IllegalArgumentException("turnsLeft must be > 0, was " + turnsLeft);
        }
    }
}
