package com.skirmisharena.engine;

import com.skirmisharena.bot.BotView;
import com.skirmisharena.bot.Strategy;

import java.util.List;
import java.util.Optional;

/** Test-only strategies, simpler than the real bots, to drive the engine into exact situations. */
final class TestStrategies {

    static final Strategy PASS = view -> Optional.empty();

    /** Plays the first card of the hand it can afford, in hand order. */
    static final Strategy PLAY_FIRST_AFFORDABLE =
            view -> view.hand().stream().filter(card -> card.cost() <= view.mana()).findFirst();

    private TestStrategies() {
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
