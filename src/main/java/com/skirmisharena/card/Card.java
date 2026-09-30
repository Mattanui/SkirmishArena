package com.skirmisharena.card;

/**
 * A card of the pool (DESIGN.md §4). One record per effect; the engine applies effects with an
 * exhaustive switch over these types. Amplified effects are not modelled here: the engine
 * derives them when a card follows Amplify (DESIGN.md §4, "Amplified" column).
 */
public sealed interface Card
        permits AttackCard, DefenseCard, ResourceCard, DrawCard, StealCard, HealCard, AmplifyCard {

    String name();

    /** Mana needed to play the card; 0 for Resource cards. */
    int cost();

    CardCategory category();
}
