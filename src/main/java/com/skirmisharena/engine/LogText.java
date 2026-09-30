package com.skirmisharena.engine;

import com.skirmisharena.card.Card;

import java.util.ArrayList;
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

    /**
     * "A plays Strike (2 mana): ", "A plays Meteor (5 mana, amplified): ",
     * "A plays Focus: " for a card that costs nothing.
     */
    static String playPrefix(Champion caster, Card card, boolean amplified) {
        List<String> details = new ArrayList<>();
        if (card.cost() > 0) {
            details.add(card.cost() + " mana");
        }
        if (amplified) {
            details.add("amplified");
        }
        String between = details.isEmpty() ? "" : " (" + String.join(", ", details) + ")";
        return caster.name() + " plays " + card.name() + between + ": ";
    }

    /** "next turn" or "next 2 turns". */
    static String nextTurns(int turns) {
        return turns == 1 ? "next turn" : "next " + turns + " turns";
    }

    /** "1 turn left" or "2 turns left". */
    static String turnsLeft(int turns) {
        return turns == 1 ? "1 turn left" : turns + " turns left";
    }
}
