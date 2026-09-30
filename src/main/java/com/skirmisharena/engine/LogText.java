package com.skirmisharena.engine;

import com.skirmisharena.card.Card;

import java.util.List;
import java.util.stream.Collectors;

/** Small text helpers shared by the log lines of the engine (DESIGN.md §9). */
final class LogText {

    private LogText() {
    }

    /** "Jab, Strike, Meteor". */
    static String names(List<Card> cards) {
        return cards.stream().map(Card::name).collect(Collectors.joining(", "));
    }

    /** "A plays Strike (2 mana): ", or "A plays Focus: " for a card that costs nothing. */
    static String playPrefix(Champion caster, Card card) {
        String cost = card.cost() > 0 ? " (" + card.cost() + " mana)" : "";
        return caster.name() + " plays " + card.name() + cost + ": ";
    }
}
