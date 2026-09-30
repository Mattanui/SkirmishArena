package com.skirmisharena.card;

/** Draws {@code count} cards from the caster's own pile (DESIGN.md §5, Insight). */
public record DrawCard(String name, int cost, int count) implements Card {

    public DrawCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        CardValidation.requirePositive("count", count);
    }

    @Override
    public CardCategory category() {
        return CardCategory.UTILITY;
    }
}
