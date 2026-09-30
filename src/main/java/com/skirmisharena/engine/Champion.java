package com.skirmisharena.engine;

import com.skirmisharena.card.Card;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * The mutable state of one champion during a match: HP, mana, hand and draw pile (DESIGN.md §2 and §3).
 * The top of the draw pile is the first element; played cards go to the bottom.
 */
public final class Champion {

    private final String name;
    private final List<Card> hand = new ArrayList<>();
    private final Deque<Card> drawPile;
    private int hp = GameRules.STARTING_HP;
    private int capacity;
    private int mana;
    private ActiveDefense activeDefense;

    /** A champion with a given draw pile, top card first. Tests use it to build exact situations. */
    Champion(String name, Collection<Card> drawPile) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;
        this.drawPile = new ArrayDeque<>(drawPile);
    }

    /** A champion whose draw pile is 20 random cards of its own copy of {@code pool} (DESIGN.md §2). */
    public static Champion withRandomDeck(String name, List<Card> pool, Random random) {
        return new Champion(name, buildDrawPile(pool, random));
    }

    static List<Card> buildDrawPile(List<Card> pool, Random random) {
        if (pool.size() < GameRules.DECK_SIZE) {
            throw new IllegalArgumentException(
                    "pool needs at least " + GameRules.DECK_SIZE + " cards, has " + pool.size());
        }
        List<Card> ownCopy = new ArrayList<>(pool);
        // Shuffling the whole copy, then keeping the first 20, picks 20 random cards
        // and leaves them in random order: both parts of DESIGN.md §2 in one step.
        Collections.shuffle(ownCopy, random);
        return List.copyOf(ownCopy.subList(0, GameRules.DECK_SIZE));
    }

    /** Draws the starting hand of 3 cards (DESIGN.md §2). */
    public void drawStartingHand() {
        for (int i = 0; i < GameRules.STARTING_HAND_SIZE; i++) {
            draw();
        }
    }

    /**
     * Takes the top card of the pile into the hand. Skipped, with no penalty, when the hand
     * already holds 7 cards or the pile is empty (DESIGN.md §3).
     *
     * @return the card drawn, or empty when the draw was skipped
     */
    public Optional<Card> draw() {
        if (isHandFull() || drawPile.isEmpty()) {
            return Optional.empty();
        }
        Card card = drawPile.removeFirst();
        hand.add(card);
        return Optional.of(card);
    }

    /** Played cards go to the bottom of the own draw pile (DESIGN.md §1). */
    public void putAtBottom(Card card) {
        drawPile.addLast(Objects.requireNonNull(card, "card"));
    }

    public boolean isHandFull() {
        return hand.size() >= GameRules.HAND_LIMIT;
    }

    public String name() {
        return name;
    }

    public int hp() {
        return hp;
    }

    public int capacity() {
        return capacity;
    }

    public int mana() {
        return mana;
    }

    /** Read-only view of the hand. */
    public List<Card> hand() {
        return Collections.unmodifiableList(hand);
    }

    /** Copy of the draw pile, top card first. */
    public List<Card> drawPile() {
        return List.copyOf(drawPile);
    }

    public int drawPileSize() {
        return drawPile.size();
    }

    public Optional<ActiveDefense> activeDefense() {
        return Optional.ofNullable(activeDefense);
    }
}
