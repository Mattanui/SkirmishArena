package com.skirmisharena.card;

/** Restores {@code mana}, never above the current capacity (DESIGN.md §3). */
public record ResourceCard(String name, int cost, int mana) implements Card {

    public ResourceCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        CardValidation.requirePositive("mana", mana);
    }

    @Override
    public CardCategory category() {
        return CardCategory.RESOURCE;
    }
}
