package com.skirmisharena.card;

/** Deals {@code damage} to the opponent, before their defense applies (DESIGN.md §5). */
public record AttackCard(String name, int cost, int damage) implements Card {

    public AttackCard {
        CardValidation.requireName(name);
        CardValidation.requireCost(cost);
        CardValidation.requirePositive("damage", damage);
    }

    @Override
    public CardCategory category() {
        return CardCategory.ATTACK;
    }
}
