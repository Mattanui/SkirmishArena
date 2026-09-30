package com.skirmisharena.bot;

import com.skirmisharena.card.Card;
import com.skirmisharena.card.CardPool;

import java.util.List;

/** Test-only: the real cards of the pool by name, and a builder for hand-made bot views. */
final class TestViews {

    static final Card JAB = card("Jab");
    static final Card STRIKE = card("Strike");
    static final Card HEAVY_BLOW = card("Heavy Blow");
    static final Card METEOR = card("Meteor");
    static final Card GUARD = card("Guard");
    static final Card SHIELD = card("Shield");
    static final Card BARRIER = card("Barrier");
    static final Card AEGIS = card("Aegis");
    static final Card FOCUS = card("Focus");
    static final Card SURGE = card("Surge");
    static final Card INSIGHT = card("Insight");
    static final Card PICKPOCKET = card("Pickpocket");
    static final Card BANDAGE = card("Bandage");
    static final Card POTION = card("Potion");
    static final Card ELIXIR = card("Elixir");
    static final Card AMPLIFY = card("Amplify");

    private TestViews() {
    }

    /** A view with 30 HP, 10/10 mana, no defense, 10 cards in the pile, 3 in the opponent's hand. */
    static Builder view() {
        return new Builder();
    }

    private static Card card(String name) {
        return CardPool.standard().stream().filter(card -> card.name().equals(name)).findFirst().orElseThrow();
    }

    static final class Builder {
        private List<Card> hand = List.of();
        private int hp = 30;
        private int capacity = 10;
        private int mana = 10;
        private boolean ownDefenseActive;
        private int ownPileSize = 10;
        private int opponentHandSize = 3;
        private boolean amplifyPending;
        private List<Card> playedThisTurn = List.of();

        Builder hand(Card... cards) {
            hand = List.of(cards);
            return this;
        }

        Builder hp(int value) {
            hp = value;
            return this;
        }

        /** Current mana out of the turn's capacity. */
        Builder mana(int current, int ofCapacity) {
            mana = current;
            capacity = ofCapacity;
            return this;
        }

        Builder defenseActive() {
            ownDefenseActive = true;
            return this;
        }

        Builder pileSize(int value) {
            ownPileSize = value;
            return this;
        }

        Builder opponentHandSize(int value) {
            opponentHandSize = value;
            return this;
        }

        Builder amplifyPending() {
            amplifyPending = true;
            return this;
        }

        Builder played(Card... cards) {
            playedThisTurn = List.of(cards);
            return this;
        }

        BotView build() {
            return new BotView(hand, hp, capacity, mana, ownDefenseActive, ownPileSize, 30, opponentHandSize,
                    amplifyPending, playedThisTurn);
        }
    }
}
