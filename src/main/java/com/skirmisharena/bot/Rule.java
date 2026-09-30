package com.skirmisharena.bot;

import com.skirmisharena.card.Card;

import java.util.List;
import java.util.Optional;

/** One line of a bot's priority list (DESIGN.md §7): it picks a card, or leaves the choice to the next rule. */
@FunctionalInterface
interface Rule {

    Optional<Card> pick(BotView view);

    /** The card of the first rule that picks one; empty when no rule applies, so the bot passes. */
    static Optional<Card> firstMatch(List<Rule> rules, BotView view) {
        for (Rule rule : rules) {
            Optional<Card> card = rule.pick(view);
            if (card.isPresent()) {
                return card;
            }
        }
        return Optional.empty();
    }
}
