package com.skirmisharena.card;

/** Heals its caster by {@code amount}, never above the maximum HP (DESIGN.md §5). */
public record HealCard(String name, int cost, int amount) implements Card {

    public HealCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        CardValidation.requirePositive("amount", amount);
    }

    @Override
    public CardCategory category() {
        return CardCategory.UTILITY;
    }
}
