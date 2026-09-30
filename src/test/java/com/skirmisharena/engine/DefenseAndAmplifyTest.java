package com.skirmisharena.engine;

import com.skirmisharena.card.AmplifyCard;
import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.card.DefenseKind;
import com.skirmisharena.card.DrawCard;
import com.skirmisharena.card.HealCard;
import com.skirmisharena.card.ResourceCard;
import com.skirmisharena.card.StealCard;
import com.skirmisharena.log.TextMatchLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static com.skirmisharena.engine.TestStrategies.PASS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DESIGN.md §4 ("Amplified" column) and §5 (Defense, Amplify), applied directly to champions. */
class DefenseAndAmplifyTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);
    private static final Card STRIKE = new AttackCard("Strike", 2, 2);
    private static final Card HEAVY_BLOW = new AttackCard("Heavy Blow", 3, 4);
    private static final Card METEOR = new AttackCard("Meteor", 5, 8);
    private static final Card GUARD = DefenseCard.reduce("Guard", 1, 1, 2);
    private static final Card SHIELD = DefenseCard.reduce("Shield", 2, 2, 2);
    private static final Card BARRIER = DefenseCard.halve("Barrier", 3, 2);
    private static final Card AEGIS = DefenseCard.block("Aegis", 5, 1);
    private static final Card FOCUS = new ResourceCard("Focus", 0, 1);
    private static final Card SURGE = new ResourceCard("Surge", 0, 2);
    private static final Card INSIGHT = new DrawCard("Insight", 1, 1);
    private static final Card PICKPOCKET = new StealCard("Pickpocket", 2, 1);
    private static final Card BANDAGE = new HealCard("Bandage", 1, 1);
    private static final Card POTION = new HealCard("Potion", 2, 2);
    private static final Card ELIXIR = new HealCard("Elixir", 3, 4);
    private static final Card AMPLIFY = new AmplifyCard("Amplify", 5);

    private final TextMatchLog log = new TextMatchLog();
    private final EffectResolver resolver = new EffectResolver(new Random(42), log);
    private final Champion a = new Champion("A", PASS, List.of());
    private final Champion b = new Champion("B", PASS, List.of());

    // --- Defense ---

    @Test
    void playingADefenseRaisesItOnTheCaster() {
        resolver.apply(GUARD, b, a, false);

        assertEquals(Optional.of(new ActiveDefense("Guard", DefenseKind.REDUCE, 1, 2)), b.activeDefense());
        assertEquals("B plays Guard (1 mana): incoming attack cards deal 1 less during A's next 2 turns", lastLine());
    }

    @Test
    void guardReducesEachIncomingAttackCardBy1() {
        resolver.apply(GUARD, b, a, false);

        PlayOutcome first = resolver.apply(STRIKE, a, b, false);
        PlayOutcome second = resolver.apply(STRIKE, a, b, false);

        assertEquals(1, first.damageDealt());
        assertEquals(1, second.damageDealt());
        assertEquals(28, b.hp());
        assertEquals("A plays Strike (2 mana): 2 damage, B's Guard absorbs 1 -> 1 damage. B: 28 HP", lastLine());
    }

    @Test
    void aReductionNeverTurnsDamageNegative() {
        resolver.apply(SHIELD, b, a, false);

        resolver.apply(JAB, a, b, false);

        assertEquals(30, b.hp());
    }

    @Test
    void barrierHalvesEachAttackCardRoundedDown() {
        resolver.apply(BARRIER, b, a, false);

        resolver.apply(JAB, a, b, false);
        assertEquals(30, b.hp(), "Jab: 1 halved is 0");

        resolver.apply(HEAVY_BLOW, a, b, false);
        assertEquals(28, b.hp(), "Heavy Blow: 4 halved is 2");
    }

    @Test
    void aegisBlocksEveryAttackCard() {
        resolver.apply(AEGIS, b, a, false);

        PlayOutcome outcome = resolver.apply(METEOR, a, b, false);

        assertEquals(0, outcome.damageDealt());
        assertEquals(30, b.hp());
        assertEquals("A plays Meteor (5 mana): 8 damage, B's Aegis absorbs 8 -> 0 damage. B: 30 HP", lastLine());
    }

    @Test
    void aChampionCannotRaiseASecondDefenseWhileOneIsActive() {
        resolver.apply(GUARD, b, a, false);

        assertThrows(IllegalStateException.class, () -> resolver.apply(SHIELD, b, a, false));
    }

    @Test
    void theDefenseCountsDownOneOpponentTurnAtATime() {
        resolver.apply(GUARD, b, a, false);

        assertEquals(1, b.countDownDefense().orElseThrow().turnsLeft());
        assertEquals(Optional.empty(), b.countDownDefense());
        assertEquals(Optional.empty(), b.activeDefense());
    }

    // --- Amplify ---

    @Test
    void amplifyLeavesABonusPendingForTheNextCard() {
        PlayOutcome outcome = resolver.apply(AMPLIFY, a, b, false);

        assertTrue(outcome.amplifyPending());
        assertEquals("A plays Amplify (5 mana): the next card this turn is doubled", lastLine());
    }

    @Test
    void theNextCardUsesTheBonusUp() {
        PlayOutcome outcome = resolver.apply(JAB, a, b, true);

        assertEquals(2, outcome.damageDealt());
        assertFalse(outcome.amplifyPending());
    }

    @Test
    void aSecondAmplifyAddsNothing() {
        PlayOutcome second = resolver.apply(AMPLIFY, a, b, true);
        assertTrue(second.amplifyPending());
        assertEquals("A plays Amplify (5 mana): no effect, the next card is already doubled", lastLine());

        PlayOutcome jab = resolver.apply(JAB, a, b, true);
        assertEquals(2, jab.damageDealt(), "doubled once, not x4");
    }

    @Test
    void theAttackIsDoubledFirstThenTheDefenseApplies() {
        resolver.apply(SHIELD, b, a, false);

        PlayOutcome outcome = resolver.apply(METEOR, a, b, true);

        assertEquals(14, outcome.damageDealt());
        assertEquals(16, b.hp());
        assertEquals("A plays Meteor (5 mana, amplified): 16 damage, B's Shield absorbs 2 -> 14 damage. B: 16 HP",
                lastLine());
    }

    // --- The "Amplified" column of DESIGN.md §4, one row per card ---

    static Stream<Arguments> amplifiedAttacks() {
        return Stream.of(
                Arguments.of(JAB, 2),
                Arguments.of(STRIKE, 4),
                Arguments.of(HEAVY_BLOW, 8),
                Arguments.of(METEOR, 16));
    }

    @ParameterizedTest(name = "amplified {0} deals {1}")
    @MethodSource("amplifiedAttacks")
    void amplifiedAttackDealsDoubleDamage(Card attack, int expectedDamage) {
        resolver.apply(attack, a, b, true);

        assertEquals(30 - expectedDamage, b.hp());
    }

    static Stream<Arguments> amplifiedDefenses() {
        return Stream.of(
                Arguments.of(GUARD, new ActiveDefense("Guard", DefenseKind.REDUCE, 2, 2)),
                Arguments.of(SHIELD, new ActiveDefense("Shield", DefenseKind.REDUCE, 4, 2)),
                Arguments.of(BARRIER, new ActiveDefense("Barrier", DefenseKind.BLOCK, 0, 2)),
                Arguments.of(AEGIS, new ActiveDefense("Aegis", DefenseKind.BLOCK, 0, 2)));
    }

    @ParameterizedTest(name = "amplified {0} raises {1}")
    @MethodSource("amplifiedDefenses")
    void amplifiedDefenseMatchesTheDesignTable(Card defense, ActiveDefense expected) {
        resolver.apply(defense, b, a, true);

        assertEquals(Optional.of(expected), b.activeDefense());
    }

    static Stream<Arguments> amplifiedHeals() {
        return Stream.of(
                Arguments.of(BANDAGE, 22),
                Arguments.of(POTION, 24),
                Arguments.of(ELIXIR, 28));
    }

    @ParameterizedTest(name = "amplified {0} from 20 HP gives {1} HP")
    @MethodSource("amplifiedHeals")
    void amplifiedHealHealsTwice(Card heal, int expectedHp) {
        a.takeDamage(10);

        resolver.apply(heal, a, b, true);

        assertEquals(expectedHp, a.hp());
    }

    @Test
    void amplifiedHealStillStopsAt30() {
        a.takeDamage(1);

        resolver.apply(ELIXIR, a, b, true);

        assertEquals(30, a.hp());
        assertEquals("A plays Elixir (3 mana, amplified): heals 1 of 8 (max 30 HP). A: 30 HP", lastLine());
    }

    static Stream<Arguments> amplifiedResources() {
        return Stream.of(
                Arguments.of(FOCUS, 3),
                Arguments.of(SURGE, 5));
    }

    @ParameterizedTest(name = "amplified {0} from 1 mana gives {1}")
    @MethodSource("amplifiedResources")
    void amplifiedResourceRestoresTwiceAsMuch(Card resource, int expectedMana) {
        Champion rich = championWithCapacity(10);
        rich.pay(9);

        resolver.apply(resource, rich, b, true);

        assertEquals(expectedMana, rich.mana());
    }

    @Test
    void amplifiedInsightDraws2Cards() {
        Champion drawer = new Champion("A", PASS, List.of(JAB, STRIKE, METEOR));

        resolver.apply(INSIGHT, drawer, b, true);

        assertEquals(List.of(JAB, STRIKE), drawer.hand());
    }

    @Test
    void amplifiedPickpocketTakes2Cards() {
        Champion victim = new Champion("B", PASS, List.of(JAB, STRIKE, METEOR));
        victim.drawStartingHand();

        resolver.apply(PICKPOCKET, a, victim, true);

        assertAll(
                () -> assertEquals(2, a.hand().size()),
                () -> assertEquals(1, victim.hand().size()));
    }

    @Test
    void amplifiedPickpocketStopsWhenTheThiefHolds7Cards() {
        Champion thief = new Champion("A", PASS, Collections.nCopies(6, JAB));
        for (int i = 0; i < 6; i++) {
            thief.draw();
        }
        Champion victim = new Champion("B", PASS, List.of(STRIKE, METEOR, HEAVY_BLOW));
        victim.drawStartingHand();

        resolver.apply(PICKPOCKET, thief, victim, true);

        assertEquals(7, thief.hand().size());
        assertEquals(2, victim.hand().size());
    }

    // --- Helpers ---

    private static Champion championWithCapacity(int capacity) {
        Champion champion = new Champion("A", PASS, List.of());
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
