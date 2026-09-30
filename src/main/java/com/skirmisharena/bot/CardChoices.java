package com.skirmisharena.bot;

import com.skirmisharena.card.AmplifyCard;
import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.card.DrawCard;
import com.skirmisharena.card.HealCard;
import com.skirmisharena.card.ResourceCard;
import com.skirmisharena.card.StealCard;
import com.skirmisharena.engine.GameRules;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/** The shared vocabulary of DESIGN.md §7: playable, useful, best, Amplify combo. */
final class CardChoices {

    /** Aegis (BLOCK), then Barrier (HALVE), then Shield and Guard (REDUCE, bigger amount first). */
    private static final Comparator<DefenseCard> DEFENSE_STRENGTH = Comparator
            .comparingInt((DefenseCard defense) -> switch (defense.kind()) {
                case REDUCE -> 0;
                case HALVE -> 1;
                case BLOCK -> 2;
            })
            .thenComparingInt(DefenseCard::amount)
            .thenComparingInt(DefenseCard::duration);

    private CardChoices() {
    }

    /** Legal (DESIGN.md §5): affordable, and no Defense card while an own defense is active. */
    static boolean playable(BotView view, Card card) {
        return card.cost() <= view.mana() && !(card instanceof DefenseCard && view.ownDefenseActive());
    }

    static Optional<Card> bestPlayableAttack(BotView view) {
        return inHand(view, AttackCard.class).filter(card -> playable(view, card))
                .max(Comparator.comparingInt(AttackCard::damage)).map(Card.class::cast);
    }

    /** The highest-damage attack held, affordable or not: the Amplify combo target of Aggressive and Balanced. */
    static Optional<Card> bestAttackInHand(BotView view) {
        return inHand(view, AttackCard.class)
                .max(Comparator.comparingInt(AttackCard::damage)).map(Card.class::cast);
    }

    static Optional<Card> bestPlayableDefense(BotView view) {
        return inHand(view, DefenseCard.class).filter(card -> playable(view, card))
                .max(DEFENSE_STRENGTH).map(Card.class::cast);
    }

    static Optional<Card> bestPlayableHeal(BotView view) {
        return inHand(view, HealCard.class).filter(card -> playable(view, card))
                .max(Comparator.comparingInt(HealCard::amount)).map(Card.class::cast);
    }

    /** A heal is useful when HP is below 30. */
    static Optional<Card> bestUsefulHeal(BotView view) {
        return view.hp() < GameRules.MAX_HP ? bestPlayableHeal(view) : Optional.empty();
    }

    /** The biggest heal held, affordable or not: the Amplify combo target of Defensive. */
    static Optional<Card> bestHealInHand(BotView view) {
        return inHand(view, HealCard.class)
                .max(Comparator.comparingInt(HealCard::amount)).map(Card.class::cast);
    }

    /**
     * Useful when mana is below capacity. Best = restores the most given what is missing;
     * on a tie, the smaller card, so the bigger one is kept for later.
     */
    static Optional<Card> bestUsefulResource(BotView view) {
        int missing = view.capacity() - view.mana();
        if (missing <= 0) {
            return Optional.empty();
        }
        Comparator<ResourceCard> restoresMost = Comparator.comparingInt(card -> Math.min(card.mana(), missing));
        Comparator<ResourceCard> thenSmallerCard = Comparator.comparingInt(ResourceCard::mana).reversed();
        return inHand(view, ResourceCard.class).filter(card -> playable(view, card))
                .max(restoresMost.thenComparing(thenSmallerCard)).map(Card.class::cast);
    }

    /** Insight is useful when the own pile is not empty. */
    static Optional<Card> usefulInsight(BotView view) {
        if (view.ownPileSize() == 0) {
            return Optional.empty();
        }
        return inHand(view, DrawCard.class).filter(card -> playable(view, card)).findFirst().map(Card.class::cast);
    }

    /** Pickpocket is useful when the opponent's hand is not empty. */
    static Optional<Card> usefulPickpocket(BotView view) {
        if (view.opponentHandSize() == 0) {
            return Optional.empty();
        }
        return inHand(view, StealCard.class).filter(card -> playable(view, card)).findFirst().map(Card.class::cast);
    }

    /**
     * Amplify combo (DESIGN.md §7): Amplify only if, after paying it, the bot can still afford its
     * combo target. Never while an Amplify is already pending: a second one adds nothing.
     */
    static Optional<Card> amplifyCombo(BotView view, Optional<Card> target) {
        if (view.amplifyPending() || target.isEmpty()) {
            return Optional.empty();
        }
        int targetCost = target.get().cost();
        return inHand(view, AmplifyCard.class).filter(amplify -> amplify.cost() + targetCost <= view.mana())
                .findFirst().map(Card.class::cast);
    }

    static boolean playedThisTurn(BotView view, Class<? extends Card> type) {
        return view.cardsPlayedThisTurn().stream().anyMatch(type::isInstance);
    }

    private static <T extends Card> Stream<T> inHand(BotView view, Class<T> type) {
        return view.hand().stream().filter(type::isInstance).map(type::cast);
    }
}
