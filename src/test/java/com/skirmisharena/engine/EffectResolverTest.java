package com.skirmisharena.engine;

import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DrawCard;
import com.skirmisharena.card.HealCard;
import com.skirmisharena.card.ResourceCard;
import com.skirmisharena.card.StealCard;
import com.skirmisharena.log.TextMatchLog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.skirmisharena.engine.TestStrategies.PASS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** DESIGN.md §3 and §5, one rule per test. Cards are applied directly, as the engine does once they are paid. */
class EffectResolverTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card STRIKE = new AttackCard("Strike", 2, 2);
    private static final Card METEOR = new AttackCard("Meteor", 5, 8);
    private static final Card POTION = new HealCard("Potion", 2, 2);
    private static final Card FOCUS = new ResourceCard("Focus", 0, 1);
    private static final Card SURGE = new ResourceCard("Surge", 0, 2);
    private static final Card INSIGHT = new DrawCard("Insight", 1, 1);
    private static final Card PICKPOCKET = new StealCard("Pickpocket", 2, 1);
    private static final long SEED = 42;

    private final TextMatchLog log = new TextMatchLog();
    private final EffectResolver resolver = new EffectResolver(new Random(SEED), log);

    // --- Heal ---

    @Test
    void healAddsItsAmount() {
        Champion a = champion("A", List.of(), 0);
        a.takeDamage(5);

        resolver.apply(POTION, a, champion("B", List.of(), 0));

        assertEquals(27, a.hp());
        assertEquals("A plays Potion (2 mana): heals 2. A: 27 HP", lastLine());
    }

    @Test
    void healNeverGoesAbove30Hp() {
        Champion a = champion("A", List.of(), 0);
        a.takeDamage(1);

        resolver.apply(POTION, a, champion("B", List.of(), 0));

        assertEquals(30, a.hp());
        assertEquals("A plays Potion (2 mana): heals 1 of 2 (max 30 HP). A: 30 HP", lastLine());
    }

    // --- Resource ---

    @Test
    void resourceRestoresManaWithinTheCapacity() {
        Champion a = championWithCapacity(5);
        a.pay(2);

        resolver.apply(FOCUS, a, champion("B", List.of(), 0));

        assertEquals(4, a.mana());
        assertEquals(5, a.capacity());
        assertEquals("A plays Focus: +1 mana (4/5)", lastLine());
    }

    @Test
    void resourceNeverGoesAboveTheCapacity() {
        Champion a = championWithCapacity(5);
        a.pay(1);

        resolver.apply(SURGE, a, champion("B", List.of(), 0));

        assertEquals(5, a.mana());
        assertEquals("A plays Surge: +1 of 2 mana (5/5)", lastLine());
    }

    @Test
    void resourceAtFullManaChangesNothing() {
        Champion a = championWithCapacity(5);

        resolver.apply(SURGE, a, champion("B", List.of(), 0));

        assertEquals(5, a.mana());
        assertEquals(5, a.capacity(), "a Resource card never raises the capacity");
        assertEquals("A plays Surge: +0 of 2 mana (5/5)", lastLine());
    }

    // --- Insight ---

    @Test
    void insightDrawsTheTopCardOfTheOwnPile() {
        Champion a = champion("A", List.of(STRIKE, JAB), 0);

        resolver.apply(INSIGHT, a, champion("B", List.of(), 0));

        assertEquals(List.of(STRIKE), a.hand());
        assertEquals(List.of(JAB), a.drawPile());
        assertEquals("A plays Insight (1 mana): draws Strike. Hand: Strike", lastLine());
    }

    @Test
    void insightDrawsNothingWhenTheHandHolds7Cards() {
        Champion a = champion("A", Collections.nCopies(8, JAB), 7);

        resolver.apply(INSIGHT, a, champion("B", List.of(), 0));

        assertEquals(7, a.hand().size());
        assertEquals(1, a.drawPileSize());
        assertEquals("A plays Insight (1 mana): draws nothing (hand full)", lastLine());
    }

    @Test
    void insightDrawsNothingFromAnEmptyPile() {
        Champion a = champion("A", List.of(), 0);

        resolver.apply(INSIGHT, a, champion("B", List.of(), 0));

        assertEquals(List.of(), a.hand());
        assertEquals("A plays Insight (1 mana): draws nothing (draw pile empty)", lastLine());
    }

    // --- Pickpocket ---

    @Test
    void pickpocketTakesTheCardPickedByTheMatchRandom() {
        List<Card> handOfB = List.of(JAB, STRIKE, METEOR);
        Champion a = champion("A", List.of(), 0);
        Champion b = champion("B", handOfB, 3);
        int pickedIndex = new Random(SEED).nextInt(handOfB.size()); // what the resolver's Random will pick
        Card expected = handOfB.get(pickedIndex);
        List<Card> expectedHandOfB = new ArrayList<>(handOfB);
        expectedHandOfB.remove(pickedIndex);

        resolver.apply(PICKPOCKET, a, b);

        assertEquals(List.of(expected), a.hand());
        assertEquals(expectedHandOfB, b.hand());
        assertEquals("A plays Pickpocket (2 mana): takes " + expected.name() + " from B. Hand: " + expected.name(),
                lastLine());
    }

    @Test
    void pickpocketOnAnEmptyHandDoesNothing() {
        Champion a = champion("A", List.of(), 0);
        Champion b = champion("B", List.of(), 0);

        resolver.apply(PICKPOCKET, a, b);

        assertEquals(List.of(), a.hand());
        assertEquals("A plays Pickpocket (2 mana): takes nothing (B's hand is empty)", lastLine());
    }

    @Test
    void pickpocketTakesNothingWhenTheThiefHolds7Cards() {
        Champion a = champion("A", Collections.nCopies(7, JAB), 7);
        Champion b = champion("B", List.of(METEOR), 1);

        resolver.apply(PICKPOCKET, a, b);

        assertEquals(7, a.hand().size());
        assertEquals(List.of(METEOR), b.hand());
        assertEquals("A plays Pickpocket (2 mana): takes nothing (hand full)", lastLine());
    }

    // --- Helpers ---

    /** A champion whose first {@code cardsInHand} cards of {@code pile} are already in hand. */
    private static Champion champion(String name, List<Card> pile, int cardsInHand) {
        Champion champion = new Champion(name, PASS, pile);
        for (int i = 0; i < cardsInHand; i++) {
            champion.draw();
        }
        return champion;
    }

    private static Champion championWithCapacity(int capacity) {
        Champion champion = champion("A", List.of(), 0);
        for (int turn = 0; turn < capacity; turn++) {
            champion.startOwnTurnMana();
        }
        return champion;
    }

    private String lastLine() {
        List<String> lines = log.lines();
        return lines.get(lines.size() - 1);
    }
}
