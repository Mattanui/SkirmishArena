package com.skirmisharena.engine;

import com.skirmisharena.bot.BotView;
import com.skirmisharena.bot.Strategy;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** Test-only strategies, simpler than the real bots, to drive the engine into exact situations. */
final class TestStrategies {

    static final Strategy PASS = view -> Optional.empty();

    /** Plays the first card of the hand it can afford, in hand order. */
    static final Strategy PLAY_FIRST_AFFORDABLE =
            view -> view.hand().stream().filter(card -> card.cost() <= view.mana()).findFirst();

    /** Plays at most one card per turn: the first it can afford. */
    static final Strategy ONE_AFFORDABLE_CARD_PER_TURN =
            view -> view.cardsPlayedThisTurn().isEmpty() ? PLAY_FIRST_AFFORDABLE.nextCard(view) : Optional.empty();

    /** Plays the first affordable Defense card whenever no own defense is active; nothing else. */
    static final Strategy DEFEND_WHEN_POSSIBLE = view -> view.ownDefenseActive()
            ? Optional.empty()
            : view.hand().stream()
                    .filter(card -> card instanceof DefenseCard && card.cost() <= view.mana())
                    .findFirst();

    /** Like PLAY_FIRST_AFFORDABLE, but never tries a Defense card while its own defense is active. */
    static final Strategy PLAY_FIRST_LEGAL = view -> view.hand().stream()
            .filter(card -> card.cost() <= view.mana())
            .filter(card -> !(card instanceof DefenseCard && view.ownDefenseActive()))
            .findFirst();

    private TestStrategies() {
    }

    /** Plays exactly the given moves, in order, one per question; {@link #endTurn()} passes. */
    @SafeVarargs
    static Strategy scripted(Optional<Card>... moves) {
        Iterator<Optional<Card>> next = List.of(moves).iterator();
        return view -> next.hasNext() ? next.next() : Optional.empty();
    }

    static Optional<Card> play(Card card) {
        return Optional.of(card);
    }

    static Optional<Card> endTurn() {
        return Optional.empty();
    }

    /** Records every view the engine shows, then lets {@code inner} decide. */
    static Strategy recording(List<BotView> views, Strategy inner) {
        return view -> {
            views.add(view);
            return inner.nextCard(view);
        };
    }

    /** Fails the test if the engine asks for a card once a champion is at 0 HP. */
    static Strategy failIfAskedAfterKo(Strategy inner) {
        return view -> {
            if (view.hp() == 0 || view.opponentHp() == 0) {
                throw new AssertionError("the engine asked for a card after a KO");
            }
            return inner.nextCard(view);
        };
    }
}
