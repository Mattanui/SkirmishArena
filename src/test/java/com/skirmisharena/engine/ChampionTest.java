package com.skirmisharena.engine;

import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.CardPool;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.card.ResourceCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.skirmisharena.engine.TestStrategies.PASS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChampionTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card STRIKE = new AttackCard("Strike", 2, 2);
    private static final Card METEOR = new AttackCard("Meteor", 5, 8);
    private static final Card GUARD = DefenseCard.reduce("Guard", 1, 1, 2);
    private static final Card FOCUS = new ResourceCard("Focus", 0, 1);

    @Test
    void newChampionHasFullHpNoManaNoCardsInHandAndNoDefense() {
        Champion champion = new Champion("A", PASS, List.of(JAB));

        assertAll(
                () -> assertEquals(30, champion.hp()),
                () -> assertEquals(0, champion.capacity()),
                () -> assertEquals(0, champion.mana()),
                () -> assertEquals(List.of(), champion.hand()),
                () -> assertEquals(Optional.empty(), champion.activeDefense()));
    }

    @Test
    void drawPileHas20CardsAllTakenFromThePool() {
        Champion champion = Champion.withRandomDeck("A", PASS, CardPool.standard(), new Random(42));

        List<Card> pile = champion.drawPile();
        assertEquals(20, pile.size());
        Map<Card, Long> inPool = countEach(CardPool.standard());
        countEach(pile).forEach((card, copies) -> assertTrue(copies <= inPool.getOrDefault(card, 0L),
                card.name() + ": " + copies + " in the pile, " + inPool.getOrDefault(card, 0L) + " in the pool"));
    }

    @Test
    void sameSeedGivesTheSamePile() {
        List<Card> first = Champion.withRandomDeck("A", PASS, CardPool.standard(), new Random(7)).drawPile();
        List<Card> second = Champion.withRandomDeck("B", PASS, CardPool.standard(), new Random(7)).drawPile();

        assertEquals(first, second);
    }

    @Test
    void differentSeedsGiveDifferentPiles() {
        List<Card> first = Champion.withRandomDeck("A", PASS, CardPool.standard(), new Random(7)).drawPile();
        List<Card> second = Champion.withRandomDeck("B", PASS, CardPool.standard(), new Random(8)).drawPile();

        assertNotEquals(first, second);
    }

    @Test
    void poolSmallerThanTheDeckIsRejected() {
        List<Card> tooSmall = Collections.nCopies(19, JAB);

        assertThrows(IllegalArgumentException.class, () -> Champion.withRandomDeck("A", PASS, tooSmall, new Random(1)));
    }

    @Test
    void startingHandIsTheTop3CardsOfThePile() {
        Champion champion = new Champion("A", PASS, List.of(JAB, STRIKE, METEOR, GUARD, FOCUS));

        champion.drawStartingHand();

        assertEquals(List.of(JAB, STRIKE, METEOR), champion.hand());
        assertEquals(List.of(GUARD, FOCUS), champion.drawPile());
    }

    @Test
    void drawTakesTheTopCardOfThePile() {
        Champion champion = new Champion("A", PASS, List.of(JAB, STRIKE));

        Optional<Card> drawn = champion.draw();

        assertEquals(Optional.of(JAB), drawn);
        assertEquals(List.of(JAB), champion.hand());
        assertEquals(List.of(STRIKE), champion.drawPile());
    }

    @Test
    void drawIsSkippedWhenTheHandHolds7Cards() {
        Champion champion = new Champion("A", PASS, Collections.nCopies(9, JAB));
        for (int i = 0; i < 7; i++) {
            champion.draw();
        }

        Optional<Card> drawn = champion.draw();

        assertEquals(Optional.empty(), drawn);
        assertEquals(7, champion.hand().size());
        assertEquals(2, champion.drawPileSize());
    }

    @Test
    void drawIsSkippedOnAnEmptyPile() {
        Champion champion = new Champion("A", PASS, List.of());

        Optional<Card> drawn = champion.draw();

        assertEquals(Optional.empty(), drawn);
        assertEquals(List.of(), champion.hand());
    }

    @Test
    void aPlayedCardGoesToTheBottomOfThePile() {
        Champion champion = new Champion("A", PASS, List.of(JAB, STRIKE));

        champion.putAtBottom(METEOR);

        assertEquals(List.of(JAB, STRIKE, METEOR), champion.drawPile());
    }

    @Test
    void handCannotBeModifiedFromOutside() {
        Champion champion = new Champion("A", PASS, List.of(JAB));
        champion.draw();

        assertThrows(UnsupportedOperationException.class, () -> champion.hand().clear());
    }

    @Test
    void manaPhaseSetsCapacityToTheOwnTurnCountCappedAt10AndRefillsMana() {
        Champion champion = new Champion("A", PASS, List.of());
        List<Integer> capacities = new ArrayList<>();
        for (int turn = 1; turn <= 12; turn++) {
            champion.startOwnTurnMana();
            assertEquals(champion.capacity(), champion.mana(), "mana refilled on own turn " + turn);
            capacities.add(champion.capacity());
            champion.loseUnspentMana();
        }

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10), capacities);
    }

    @Test
    void payingSpendsManaAndCannotGoBelowZero() {
        Champion champion = new Champion("A", PASS, List.of());
        champion.startOwnTurnMana();
        champion.startOwnTurnMana();

        champion.pay(2);

        assertEquals(0, champion.mana());
        assertThrows(IllegalStateException.class, () -> champion.pay(1));
    }

    @Test
    void unspentManaIsLostAtTheEndOfTheTurn() {
        Champion champion = new Champion("A", PASS, List.of());
        champion.startOwnTurnMana();

        champion.loseUnspentMana();

        assertEquals(0, champion.mana());
        assertEquals(1, champion.capacity());
    }

    @Test
    void hpNeverGoesBelowZeroAndZeroHpIsAKo() {
        Champion champion = new Champion("A", PASS, List.of());

        champion.takeDamage(29);
        assertEquals(1, champion.hp());
        assertFalse(champion.isKo());

        champion.takeDamage(8);
        assertEquals(0, champion.hp());
        assertTrue(champion.isKo());
    }

    @Test
    void removingACardThatIsNotInTheHandIsRejected() {
        Champion champion = new Champion("A", PASS, List.of(JAB));
        champion.draw();

        assertThrows(IllegalStateException.class, () -> champion.removeFromHand(METEOR));
    }

    private static Map<Card, Long> countEach(List<Card> cards) {
        return cards.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
