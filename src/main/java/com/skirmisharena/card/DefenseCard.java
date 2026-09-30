package com.skirmisharena.card;

import java.util.Objects;

/**
 * Protects its caster during the opponent's next {@code duration} turns (DESIGN.md §5).
 * Only {@link DefenseKind#REDUCE} uses {@code amount}; HALVE and BLOCK must have an amount of 0.
 */
public record DefenseCard(String name, int cost, DefenseKind kind, int amount, int duration) implements Card {

    public DefenseCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        Objects.requireNonNull(kind, "kind");
        if (kind == DefenseKind.REDUCE) {
            CardValidation.requirePositive("amount", amount);
        } else if (amount != 0) {
            throw new IllegalArgumentException(kind + " takes no amount, was " + amount);
        }
        CardValidation.requirePositive("duration", duration);
    }

    public static DefenseCard reduce(String name, int cost, int amount, int duration) {
        return new DefenseCard(name, cost, DefenseKind.REDUCE, amount, duration);
    }

    public static DefenseCard halve(String name, int cost, int duration) {
        return new DefenseCard(name, cost, DefenseKind.HALVE, 0, duration);
    }

    public static DefenseCard block(String name, int cost, int duration) {
        return new DefenseCard(name, cost, DefenseKind.BLOCK, 0, duration);
    }

    @Override
    public CardCategory category() {
        return CardCategory.DEFENSE;
    }
}
