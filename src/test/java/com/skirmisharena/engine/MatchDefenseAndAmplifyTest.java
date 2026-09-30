package com.skirmisharena.engine;

import com.skirmisharena.bot.BotView;
import com.skirmisharena.bot.Strategy;
import com.skirmisharena.card.AmplifyCard;
import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.log.NoMatchLog;
import com.skirmisharena.log.TextMatchLog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.skirmisharena.engine.TestStrategies.DEFEND_WHEN_POSSIBLE;
import static com.skirmisharena.engine.TestStrategies.ONE_AFFORDABLE_CARD_PER_TURN;
import static com.skirmisharena.engine.TestStrategies.PASS;
import static com.skirmisharena.engine.TestStrategies.PLAY_FIRST_AFFORDABLE;
import static com.skirmisharena.engine.TestStrategies.recording;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Defense timing and Amplify inside a real turn loop (DESIGN.md §3 and §5). */
class MatchDefenseAndAmplifyTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card GUARD = DefenseCard.reduce("Guard", 1, 1, 2);
    private static final Card AEGIS = DefenseCard.block("Aegis", 5, 1);
    private static final Card AMPLIFY = new AmplifyCard("Amplify", 5);
    private static final List<Card> TWENTY_JABS = Collections.nCopies(20, JAB);

    @Test
    void aDefenseCardIsIllegalWhileTheCasterHasOneActive() {
        // Turn 1: A plays a Guard (covers B's turns 2 and 4). Turn 3: A tries its second Guard.
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, pileStartingWith(GUARD, GUARD));
        Champion b = new Champion("B", PASS, TWENTY_JABS);
        Match match = new Match(a, b, Side.A, new Random(1), new NoMatchLog());

        IllegalStateException error = assertThrows(IllegalStateException.class, match::play);
        assertTrue(error.getMessage().contains("already has an active defense"), error.getMessage());
    }

    @Test
    void guardCoversExactlyTheOpponentsNext2Turns() {
        // A plays one Jab per turn. B plays its Guard on turn 2: A's Jabs of turns 3 and 5 deal 0, turn 7 deals 1.
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", ONE_AFFORDABLE_CARD_PER_TURN, TWENTY_JABS);
        Champion b = new Champion("B", DEFEND_WHEN_POSSIBLE, pileStartingWith(GUARD));

        new Match(a, b, Side.A, new Random(1), log).play();

        List<String> lines = log.lines();
        assertInOrder(lines,
                "After turn 1: A 30 HP, B 29 HP",
                "B plays Guard (1 mana): incoming attack cards deal 1 less during A's next 2 turns",
                "A plays Jab (1 mana): 1 damage, B's Guard absorbs 1 -> 0 damage. B: 29 HP",
                "End of turn 3: B's Guard has 1 turn left",
                "End of turn 5: B's Guard ends",
                "After turn 7: A 30 HP, B 28 HP");
    }

    @Test
    void aegisCoversExactlyOneOpponentTurn() {
        // B can afford Aegis (5 mana) on turn 10: A's Jab of turn 11 is blocked, the one of turn 13 is not.
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", ONE_AFFORDABLE_CARD_PER_TURN, TWENTY_JABS);
        Champion b = new Champion("B", DEFEND_WHEN_POSSIBLE, pileStartingWith(AEGIS));

        new Match(a, b, Side.A, new Random(1), log).play();

        assertInOrder(log.lines(),
                "After turn 9: A 30 HP, B 25 HP",
                "B plays Aegis (5 mana): every incoming attack card is blocked during A's next turn",
                "After turn 11: A 30 HP, B 25 HP",
                "End of turn 11: B's Aegis ends",
                "After turn 13: A 30 HP, B 24 HP");
    }

    @Test
    void theBotSeesThePendingAmplifyAndTheBonusIsLostAtTheEndOfTheTurn() {
        // A plays Amplify as soon as it can (turn 9, 5 mana), then passes instead of using the bonus.
        List<BotView> viewsOfA = new ArrayList<>();
        Strategy amplifyThenPass = view -> view.amplifyPending() || view.mana() < 5
                ? Optional.empty()
                : view.hand().stream().filter(card -> card instanceof AmplifyCard).findFirst();
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", recording(viewsOfA, amplifyThenPass), pileStartingWith(AMPLIFY));
        Champion b = new Champion("B", PASS, TWENTY_JABS);

        new Match(a, b, Side.A, new Random(1), log).play();

        BotView afterAmplify = viewsOfA.stream()
                .filter(view -> view.cardsPlayedThisTurn().contains(AMPLIFY))
                .findFirst().orElseThrow();
        int index = viewsOfA.indexOf(afterAmplify);
        assertTrue(afterAmplify.amplifyPending(), "the bot is told a bonus is pending");
        assertFalse(viewsOfA.get(index + 1).amplifyPending(), "the next turn starts without the bonus");
        assertInOrder(log.lines(),
                "A plays Amplify (5 mana): the next card this turn is doubled",
                "End of turn 9: A's Amplify is lost, no card followed it");
    }

    @Test
    void theCardAfterAmplifyIsDoubledInAMatch() {
        // Turn 11 (6 mana): Amplify (5) then Jab (1) doubled to 2 damage.
        Strategy amplifyThenJab = view -> {
            if (view.amplifyPending()) {
                return view.hand().stream().filter(card -> card == JAB).findFirst();
            }
            return view.mana() >= 6
                    ? view.hand().stream().filter(card -> card instanceof AmplifyCard).findFirst()
                    : Optional.empty();
        };
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", amplifyThenJab, pileStartingWith(AMPLIFY));
        Champion b = new Champion("B", PASS, TWENTY_JABS);

        new Match(a, b, Side.A, new Random(1), log).play();

        assertInOrder(log.lines(),
                "--- Turn 11: A ---",
                "A plays Amplify (5 mana): the next card this turn is doubled",
                "A plays Jab (1 mana, amplified): 2 damage. B: 28 HP");
    }

    /** A 20-card pile: the given cards on top, then Jabs. */
    private static List<Card> pileStartingWith(Card... top) {
        List<Card> pile = new ArrayList<>(List.of(top));
        while (pile.size() < 20) {
            pile.add(JAB);
        }
        return pile;
    }

    /** Each expected line appears in the log, in this order (other lines may come between). */
    private static void assertInOrder(List<String> lines, String... expected) {
        int from = 0;
        for (String line : expected) {
            int found = lines.subList(from, lines.size()).indexOf(line);
            assertTrue(found >= 0, "missing, or out of order: " + line);
            from += found + 1;
        }
    }
}
