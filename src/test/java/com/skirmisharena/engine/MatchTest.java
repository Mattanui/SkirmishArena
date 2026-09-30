package com.skirmisharena.engine;

import com.skirmisharena.bot.BotView;
import com.skirmisharena.bot.Strategy;
import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DrawCard;
import com.skirmisharena.card.StealCard;
import com.skirmisharena.log.NoMatchLog;
import com.skirmisharena.log.TextMatchLog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static com.skirmisharena.engine.TestStrategies.PASS;
import static com.skirmisharena.engine.TestStrategies.PLAY_FIRST_AFFORDABLE;
import static com.skirmisharena.engine.TestStrategies.failIfAskedAfterKo;
import static com.skirmisharena.engine.TestStrategies.recording;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card METEOR = new AttackCard("Meteor", 5, 8);
    private static final List<Card> TWENTY_JABS = Collections.nCopies(20, JAB);

    @Test
    void capacityGrowsByOneEachOwnTurnUpTo10AndManaIsRefilledEveryTurn() {
        List<BotView> viewsOfA = new ArrayList<>();
        Champion a = new Champion("A", recording(viewsOfA, PASS), TWENTY_JABS);
        Champion b = new Champion("B", PASS, TWENTY_JABS);

        new Match(a, b, Side.A, new Random(1), new NoMatchLog()).play();

        List<Integer> expected = IntStream.rangeClosed(1, 25).map(turn -> Math.min(turn, 10)).boxed().toList();
        assertEquals(expected, viewsOfA.stream().map(BotView::capacity).toList(), "capacity at each of A's turns");
        assertEquals(expected, viewsOfA.stream().map(BotView::mana).toList(), "mana at the start of each of A's turns");
    }

    @Test
    void twoPassingBotsDrawAfterExactly50Turns() {
        List<BotView> viewsOfA = new ArrayList<>();
        List<BotView> viewsOfB = new ArrayList<>();
        Champion a = new Champion("A", recording(viewsOfA, PASS), TWENTY_JABS);
        Champion b = new Champion("B", recording(viewsOfB, PASS), TWENTY_JABS);

        MatchResult result = new Match(a, b, Side.A, new Random(1), new NoMatchLog()).play();

        assertAll(
                () -> assertTrue(result.isDraw()),
                () -> assertEquals(50, result.turnsPlayed()),
                () -> assertEquals(EndReason.TURN_LIMIT, result.endReason()),
                () -> assertEquals(0, result.damageDealtBy(Side.A)),
                () -> assertEquals(0, result.damageDealtBy(Side.B)),
                () -> assertEquals(25, viewsOfA.size(), "turns played by A"),
                () -> assertEquals(25, viewsOfB.size(), "turns played by B"));
    }

    @Test
    void higherHpWinsAtTheTurnLimitAndAPlayedCardComesBackFromTheBottomOfThePile() {
        // A owns a single Jab: played, put at the bottom of an otherwise empty pile, drawn again next turn.
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, List.of(JAB));
        Champion b = new Champion("B", PASS, TWENTY_JABS);

        MatchResult result = new Match(a, b, Side.A, new Random(1), new NoMatchLog()).play();

        assertAll(
                () -> assertEquals(Optional.of(Side.A), result.winner()),
                () -> assertEquals(EndReason.TURN_LIMIT, result.endReason()),
                () -> assertEquals(50, result.turnsPlayed()),
                () -> assertEquals(25, result.damageDealtBy(Side.A), "1 damage in each of A's 25 turns"),
                () -> assertEquals(0, result.damageDealtBy(Side.B)),
                () -> assertEquals(5, b.hp()));
    }

    @Test
    void aKoEndsTheMatchAtOnce() {
        // Meteor (5 mana, 8 damage) from A's 5th turn: B goes 30 -> 22 -> 14 -> 6 -> 0 on turns 9, 11, 13, 15.
        Champion a = new Champion("A", failIfAskedAfterKo(PLAY_FIRST_AFFORDABLE), Collections.nCopies(20, METEOR));
        Champion b = new Champion("B", failIfAskedAfterKo(PASS), TWENTY_JABS);

        MatchResult result = new Match(a, b, Side.A, new Random(1), new NoMatchLog()).play();

        assertAll(
                () -> assertEquals(Optional.of(Side.A), result.winner()),
                () -> assertEquals(EndReason.KO, result.endReason()),
                () -> assertEquals(15, result.turnsPlayed()),
                () -> assertEquals(32, result.damageDealtBy(Side.A), "4 Meteors, overkill included"),
                () -> assertEquals(0, b.hp()));
    }

    @Test
    void withFirstSideBThenBPlaysTurn1() {
        List<String> askedInOrder = new ArrayList<>();
        Strategy a = view -> {
            askedInOrder.add("A");
            return Optional.empty();
        };
        Strategy b = view -> {
            askedInOrder.add("B");
            return Optional.empty();
        };

        new Match(new Champion("A", a, TWENTY_JABS), new Champion("B", b, TWENTY_JABS), Side.B,
                new Random(1), new NoMatchLog()).play();

        assertEquals(List.of("B", "A", "B", "A"), askedInOrder.subList(0, 4));
        assertEquals(50, askedInOrder.size());
    }

    @Test
    void aCardNotInTheHandIsRejected() {
        Champion a = new Champion("A", view -> Optional.of(METEOR), TWENTY_JABS);
        Champion b = new Champion("B", PASS, TWENTY_JABS);
        Match match = new Match(a, b, Side.A, new Random(1), new NoMatchLog());

        assertThrows(IllegalStateException.class, match::play);
    }

    @Test
    void aCardCostingMoreThanTheManaIsRejected() {
        // Turn 1: A has 1 mana and tries to play a Meteor (5 mana) from its hand.
        Strategy firstCardWhateverTheCost = view -> view.hand().stream().findFirst();
        Champion a = new Champion("A", firstCardWhateverTheCost, Collections.nCopies(20, METEOR));
        Champion b = new Champion("B", PASS, TWENTY_JABS);
        Match match = new Match(a, b, Side.A, new Random(1), new NoMatchLog());

        assertThrows(IllegalStateException.class, match::play);
    }

    @Test
    void aStolenCardBelongsToTheThiefOncePlayed() {
        // Turn 3: A's Pickpocket takes B's only card, a Meteor. From turn 9 A plays it whenever it can:
        // B goes 30 -> 22 (turn 9) -> 14 (turn 13) -> 6 (turn 15) -> 0 (turn 19).
        Card pickpocket = new StealCard("Pickpocket", 2, 1);
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, List.of(pickpocket));
        Champion b = new Champion("B", PASS, List.of(METEOR));

        MatchResult result = new Match(a, b, Side.A, new Random(1), new NoMatchLog()).play();

        assertAll(
                () -> assertEquals(Optional.of(Side.A), result.winner()),
                () -> assertEquals(EndReason.KO, result.endReason()),
                () -> assertEquals(19, result.turnsPlayed()),
                () -> assertEquals(32, result.damageDealtBy(Side.A)),
                () -> assertTrue(a.drawPile().contains(METEOR), "the Meteor went to the thief's pile"),
                () -> assertFalse(b.hand().contains(METEOR) || b.drawPile().contains(METEOR),
                        "the Meteor never went back to B"));
    }

    @Test
    void aTurnWithMoreThan50CardsPlayedIsStopped() {
        // Test-only card: free and draws a card, so the hand never shrinks and the turn would never end.
        Card freeInsight = new DrawCard("Free Insight", 0, 1);
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, Collections.nCopies(10, freeInsight));
        Champion b = new Champion("B", PASS, TWENTY_JABS);
        Match match = new Match(a, b, Side.A, new Random(1), new NoMatchLog());

        IllegalStateException error = assertThrows(IllegalStateException.class, match::play);
        assertTrue(error.getMessage().contains("more than 50 cards in one turn"), error.getMessage());
    }

    @Test
    void aMatchCanBePlayedOnlyOnce() {
        Match match = new Match(new Champion("A", PASS, TWENTY_JABS), new Champion("B", PASS, TWENTY_JABS),
                Side.A, new Random(1), new NoMatchLog());
        match.play();

        assertThrows(IllegalStateException.class, match::play);
    }

    @Test
    void logShowsStartingHandsDrawsManaPlaysAndPasses() {
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, List.of(JAB));
        Champion b = new Champion("B", PASS, Collections.nCopies(4, JAB));

        new Match(a, b, Side.A, new Random(1), log).play();

        assertEquals(List.of(
                "A starting hand: Jab",
                "B starting hand: Jab, Jab, Jab",
                "--- Turn 1: A ---",
                "A draws nothing (draw pile empty)",
                "A has 1/1 mana",
                "A plays Jab (1 mana): 1 damage. B: 29 HP",
                "A passes, 0 mana unused",
                "After turn 1: A 30 HP, B 29 HP",
                "--- Turn 2: B ---",
                "B draws Jab. Hand: Jab, Jab, Jab, Jab",
                "B has 1/1 mana",
                "B passes, 1 mana unused",
                "After turn 2: A 30 HP, B 29 HP"), log.lines().subList(0, 13));
    }
}
