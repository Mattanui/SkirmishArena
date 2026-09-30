package com.skirmisharena.bot;

import com.skirmisharena.card.Card;

import java.util.List;

/**
 * Read-only snapshot of the game given to a bot before each card (DESIGN.md §7). The lists are
 * unmodifiable copies, so a bot can never change the real hand.
 */
public record BotView(
        List<Card> hand,
        int hp,
        int capacity,
        int mana,
        boolean ownDefenseActive,
        int ownPileSize,
        int opponentHp,
        int opponentHandSize,
        boolean amplifyPending,
        List<Card> cardsPlayedThisTurn) {

    public BotView {
        hand = List.copyOf(hand);
        cardsPlayedThisTurn = List.copyOf(cardsPlayedThisTurn);
    }
}
