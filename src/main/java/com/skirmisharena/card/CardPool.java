package com.skirmisharena.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The 27-card pool of DESIGN.md §4. Keep this list and the table identical. */
public final class CardPool {

    private static final List<Card> STANDARD = buildStandard();

    private CardPool() {
    }

    /** The 27 cards, in the order of the DESIGN.md §4 table, copies next to each other. Unmodifiable. */
    public static List<Card> standard() {
        return STANDARD;
    }

    private static List<Card> buildStandard() {
        List<Card> pool = new ArrayList<>();
        // Attack
        addCopies(pool, 2, new AttackCard("Jab", 1, 1));
        addCopies(pool, 2, new AttackCard("Strike", 2, 2));
        addCopies(pool, 1, new AttackCard("Heavy Blow", 3, 4));
        addCopies(pool, 1, new AttackCard("Meteor", 5, 8));
        // Defense
        addCopies(pool, 2, DefenseCard.reduce("Guard", 1, 1, 2));
        addCopies(pool, 2, DefenseCard.reduce("Shield", 2, 2, 2));
        addCopies(pool, 1, DefenseCard.halve("Barrier", 3, 2));
        addCopies(pool, 1, DefenseCard.block("Aegis", 5, 1));
        // Resource
        addCopies(pool, 3, new ResourceCard("Focus", 0, 1));
        addCopies(pool, 2, new ResourceCard("Surge", 0, 2));
        // Utility
        addCopies(pool, 2, new DrawCard("Insight", 1, 1));
        addCopies(pool, 2, new StealCard("Pickpocket", 2, 1));
        addCopies(pool, 2, new HealCard("Bandage", 1, 1));
        addCopies(pool, 2, new HealCard("Potion", 2, 2));
        addCopies(pool, 1, new HealCard("Elixir", 3, 4));
        addCopies(pool, 1, new AmplifyCard("Amplify", 5));
        return List.copyOf(pool);
    }

    private static void addCopies(List<Card> pool, int copies, Card card) {
        pool.addAll(Collections.nCopies(copies, card));
    }
}
