package com.skirmisharena.card;

/** Takes {@code count} random cards from the opponent's hand (DESIGN.md §5, Pickpocket). */
public record StealCard(String name, int cost, int count) implements Card {

    public StealCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        CardValidation.requirePositive("count", count);
    }

    @Override
    public CardCategory category() {
        return CardCategory.UTILITY;
    }
}
