package com.skirmisharena.engine;

import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.CardPool;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.log.MatchLog;
import com.skirmisharena.log.NoMatchLog;
import com.skirmisharena.log.TextMatchLog;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.skirmisharena.engine.TestStrategies.PASS;
import static com.skirmisharena.engine.TestStrategies.PLAY_FIRST_AFFORDABLE;
import static com.skirmisharena.engine.TestStrategies.PLAY_FIRST_LEGAL;
import static com.skirmisharena.engine.TestStrategies.endTurn;
import static com.skirmisharena.engine.TestStrategies.play;
import static com.skirmisharena.engine.TestStrategies.scripted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** DESIGN.md §9: the human-readable log of a whole match. */
class MatchLogTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card GUARD = DefenseCard.reduce("Guard", 1, 1, 2);
    /** Test-only card, so that a KO comes on turn 5 and the whole log stays short. */
    private static final Card FINISHER = new AttackCard("Finisher", 2, 30);

    @Test
    void theFullLogOfAShortMatchReadsLikeTheDesign() {
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A",
                scripted(play(GUARD), endTurn(), play(JAB), endTurn(), play(FINISHER)),
                List.of(GUARD, JAB, FINISHER));
        Champion b = new Champion("B",
                scripted(play(JAB), endTurn(), play(JAB), play(JAB), endTurn()),
                Collections.nCopies(4, JAB));
        log.line(Match.header(1, "Scripted", "Scripted", 7, Side.A));

        new Match(a, b, Side.A, new Random(7), log).play();

        assertEquals(List.of(
                "=== Match 1: Scripted (A) vs Scripted (B), seed 7. A starts ===",
                "A starting hand: Guard, Jab, Finisher",
                "B starting hand: Jab, Jab, Jab",
                "--- Turn 1: A ---",
                "A draws nothing (draw pile empty)",
                "A has 1/1 mana",
                "A plays Guard (1 mana): incoming attack cards deal 1 less during B's next 2 turns",
                "A passes, 0 mana unused",
                "After turn 1: A 30 HP, B 30 HP",
                "--- Turn 2: B ---",
                "B draws Jab. Hand: Jab, Jab, Jab, Jab",
                "B has 1/1 mana",
                "B plays Jab (1 mana): 1 damage, A's Guard absorbs 1 -> 0 damage. A: 30 HP",
                "B passes, 0 mana unused",
                "After turn 2: A 30 HP, B 30 HP",
                "End of turn 2: A's Guard has 1 turn left",
                "--- Turn 3: A ---",
                "A draws Guard. Hand: Jab, Finisher, Guard",
                "A has 2/2 mana",
                "A plays Jab (1 mana): 1 damage. B: 29 HP",
                "A passes, 1 mana unused",
                "After turn 3: A 30 HP, B 29 HP",
                "--- Turn 4: B ---",
                "B draws Jab. Hand: Jab, Jab, Jab, Jab",
                "B has 2/2 mana",
                "B plays Jab (1 mana): 1 damage, A's Guard absorbs 1 -> 0 damage. A: 30 HP",
                "B plays Jab (1 mana): 1 damage, A's Guard absorbs 1 -> 0 damage. A: 30 HP",
                "B passes, 0 mana unused",
                "After turn 4: A 30 HP, B 29 HP",
                "End of turn 4: A's Guard ends",
                "--- Turn 5: A ---",
                "A draws Jab. Hand: Finisher, Guard, Jab",
                "A has 3/3 mana",
                "A plays Finisher (2 mana): 30 damage. B: 0 HP",
                "=== Result: A wins by KO on turn 5. HP A 30, B 0 ==="), log.lines());
    }

    @Test
    void theHeaderNamesTheMatchTheBotsTheSeedAndWhoStarts() {
        assertEquals("=== Match 2: Balanced (A) vs Aggressive (B), seed 42. B starts ===",
                Match.header(2, "Balanced", "Aggressive", 42, Side.B));
    }

    @Test
    void theLastLineOfAWinOnHp() {
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", PLAY_FIRST_AFFORDABLE, List.of(JAB));
        Champion b = new Champion("B", PASS, Collections.nCopies(20, JAB));

        new Match(a, b, Side.A, new Random(1), log).play();

        assertEquals("=== Result: A wins on HP after 50 turns. HP A 30, B 5 ===", lastLine(log));
    }

    @Test
    void theLastLineOfADraw() {
        TextMatchLog log = new TextMatchLog();
        Champion a = new Champion("A", PASS, Collections.nCopies(20, JAB));
        Champion b = new Champion("B", PASS, Collections.nCopies(20, JAB));

        new Match(a, b, Side.A, new Random(1), log).play();

        assertEquals("=== Result: draw after 50 turns. HP A 30, B 30 ===", lastLine(log));
    }

    @Test
    void aDisabledLogIsNeverAskedToWriteALine() {
        MatchLog disabled = new MatchLog() {
            @Override
            public void line(String text) {
                throw new AssertionError("a disabled log received: " + text);
            }

            @Override
            public boolean enabled() {
                return false;
            }
        };

        playSeededMatch(disabled);
    }

    @Test
    void loggingNeverChangesTheGame() {
        MatchResult withLog = playSeededMatch(new TextMatchLog());
        MatchResult withoutLog = playSeededMatch(new NoMatchLog());

        assertEquals(withLog, withoutLog);
    }

    /** A match with random decks from the real pool, so that every kind of card gets played. */
    private static MatchResult playSeededMatch(MatchLog log) {
        Random random = new Random(99);
        Champion a = Champion.withRandomDeck("A", PLAY_FIRST_LEGAL, CardPool.standard(), random);
        Champion b = Champion.withRandomDeck("B", PLAY_FIRST_LEGAL, CardPool.standard(), random);
        return new Match(a, b, Side.A, random, log).play();
    }

    private static String lastLine(TextMatchLog log) {
        List<String> lines = log.lines();
        return lines.get(lines.size() - 1);
    }
}
