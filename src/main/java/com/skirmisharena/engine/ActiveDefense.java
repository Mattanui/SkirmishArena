package com.skirmisharena.engine;

import com.skirmisharena.card.DefenseKind;

import java.util.Objects;
import java.util.Optional;

/**
 * The defense currently protecting a champion (DESIGN.md §5). {@code turnsLeft} counts the
 * opponent's turns still covered. {@code cardName} is kept for the log ("B's Guard absorbs 1").
 */
public record ActiveDefense(String cardName, DefenseKind kind, int amount, int turnsLeft) {

    public ActiveDefense {
        Objects.requireNonNull(cardName, "cardName");
        Objects.requireNonNull(kind, "kind");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0, was " + amount);
        }
        if (turnsLeft <= 0) {
            throw new IllegalArgumentException("turnsLeft must be > 0, was " + turnsLeft);
        }
    }

    /** The damage one incoming attack card still deals through this defense (DESIGN.md §5). */
    public int absorb(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage must be >= 0, was " + damage);
        }
        return switch (kind) {
            case REDUCE -> Math.max(0, damage - amount);
            case HALVE -> damage / 2; // integer division rounds down, as the rule says
            case BLOCK -> 0;
        };
    }

    /** This defense after one more opponent turn, or empty once it has run out. */
    public Optional<ActiveDefense> countDown() {
        return turnsLeft == 1
                ? Optional.empty()
                : Optional.of(new ActiveDefense(cardName, kind, amount, turnsLeft - 1));
    }
}
