package com.skirmisharena.bot;

import com.skirmisharena.card.Card;

import java.util.Optional;

/** A bot's decision rule (DESIGN.md §7). Asked again after every card it plays. */
@FunctionalInterface
public interface Strategy {

    /**
     * The next card to play, taken from {@code view.hand()}, or empty to pass. A strategy only reads
     * the view: the engine checks and applies the play.
     */
    Optional<Card> nextCard(BotView view);
}
