package com.skirmisharena.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardPoolTest {

    private final List<Card> pool = CardPool.standard();

    /** DESIGN.md §4, one row per card type: the card, its category, its number of copies. */
    static Stream<Arguments> designTable() {
        return Stream.of(
                Arguments.of(new AttackCard("Jab", 1, 1), CardCategory.ATTACK, 2),
                Arguments.of(new AttackCard("Strike", 2, 2), CardCategory.ATTACK, 2),
                Arguments.of(new AttackCard("Heavy Blow", 3, 4), CardCategory.ATTACK, 1),
                Arguments.of(new AttackCard("Meteor", 5, 8), CardCategory.ATTACK, 1),
                Arguments.of(new DefenseCard("Guard", 1, DefenseKind.REDUCE, 1, 2), CardCategory.DEFENSE, 2),
                Arguments.of(new DefenseCard("Shield", 2, DefenseKind.REDUCE, 2, 2), CardCategory.DEFENSE, 2),
                Arguments.of(new DefenseCard("Barrier", 3, DefenseKind.HALVE, 0, 2), CardCategory.DEFENSE, 1),
                Arguments.of(new DefenseCard("Aegis", 5, DefenseKind.BLOCK, 0, 1), CardCategory.DEFENSE, 1),
                Arguments.of(new ResourceCard("Focus", 0, 1), CardCategory.RESOURCE, 3),
                Arguments.of(new ResourceCard("Surge", 0, 2), CardCategory.RESOURCE, 2),
                Arguments.of(new DrawCard("Insight", 1, 1), CardCategory.UTILITY, 2),
                Arguments.of(new StealCard("Pickpocket", 2, 1), CardCategory.UTILITY, 2),
                Arguments.of(new HealCard("Bandage", 1, 1), CardCategory.UTILITY, 2),
                Arguments.of(new HealCard("Potion", 2, 2), CardCategory.UTILITY, 2),
                Arguments.of(new HealCard("Elixir", 3, 4), CardCategory.UTILITY, 1),
                Arguments.of(new AmplifyCard("Amplify", 5), CardCategory.UTILITY, 1));
    }

    @Test
    void poolHas27Cards() {
        assertEquals(27, pool.size());
    }

    @Test
    void poolHas16DistinctCardNames() {
        assertEquals(16, pool.stream().map(Card::name).distinct().count());
    }

    @Test
    void poolHas6Attack6Defense5Resource10Utility() {
        Map<CardCategory, Long> perCategory = pool.stream().collect(Collectors.groupingBy(
                Card::category, () -> new EnumMap<>(CardCategory.class), Collectors.counting()));

        assertEquals(Map.of(
                CardCategory.ATTACK, 6L,
                CardCategory.DEFENSE, 6L,
                CardCategory.RESOURCE, 5L,
                CardCategory.UTILITY, 10L), perCategory);
    }

    @ParameterizedTest(name = "{0}: {1}, {2} copies")
    @MethodSource("designTable")
    void everyRowOfTheDesignTableIsInThePool(Card expected, CardCategory category, int copies) {
        List<Card> sameName = pool.stream().filter(card -> card.name().equals(expected.name())).toList();

        assertEquals(copies, sameName.size(), "copies of " + expected.name());
        sameName.forEach(card -> assertEquals(expected, card));
        assertEquals(category, expected.category());
    }

    @Test
    void cardsComeInTableOrderWithCopiesNextToEachOther() {
        List<Card> expected = designTable()
                .flatMap(row -> Collections.nCopies((int) row.get()[2], (Card) row.get()[0]).stream())
                .toList();

        assertEquals(expected, pool);
    }

    @Test
    void poolCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> pool.add(new AttackCard("Extra", 1, 1)));
    }
}
