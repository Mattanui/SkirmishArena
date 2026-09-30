package com.skirmisharena.card;

/** Doubles the next card played in the same turn (DESIGN.md §5). Carries no value of its own. */
public record AmplifyCard(String name, int cost) implements Card {

    public AmplifyCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
    }

    @Override
    public CardCategory category() {
        return CardCategory.UTILITY;
    }
}
