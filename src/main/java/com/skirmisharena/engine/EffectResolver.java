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
import com.skirmisharena.log.MatchLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.skirmisharena.engine.LogText.names;
import static com.skirmisharena.engine.LogText.nextTurns;
import static com.skirmisharena.engine.LogText.playPrefix;

/**
 * Applies the effect of a played card, immediately (DESIGN.md §3 and §5), and logs it.
 * When an Amplify bonus is pending, the card uses its "Amplified" effect (DESIGN.md §4).
 */
final class EffectResolver {

    /** The run's single Random (DESIGN.md §8): only Pickpocket needs chance here. */
    private final Random random;
    private final MatchLog log;

    EffectResolver(Random random, MatchLog log) {
        this.random = random;
        this.log = log;
    }

    /**
     * Applies {@code card}, already paid and removed from the caster's hand.
     *
     * @param amplifyPending whether an Amplify played earlier this turn is waiting for a card
     * @return the damage dealt, and whether an Amplify bonus is still pending afterwards
     */
    PlayOutcome apply(Card card, Champion caster, Champion opponent, boolean amplifyPending) {
        boolean amplified = amplifyPending; // any card but Amplify itself uses the pending bonus
        int factor = amplified ? 2 : 1;
        return switch (card) {
            case AttackCard attack -> new PlayOutcome(attack(attack, factor, caster, opponent), false);
            case DefenseCard defense -> {
                defend(defense, amplified, caster, opponent);
                yield PlayOutcome.NOTHING_PENDING;
            }
            case HealCard heal -> {
                heal(heal, factor, caster);
                yield PlayOutcome.NOTHING_PENDING;
            }
            case ResourceCard resource -> {
                restoreMana(resource, factor, caster);
                yield PlayOutcome.NOTHING_PENDING;
            }
            case DrawCard draw -> {
                draw(draw, factor, caster);
                yield PlayOutcome.NOTHING_PENDING;
            }
            case StealCard steal -> {
                steal(steal, factor, caster, opponent);
                yield PlayOutcome.NOTHING_PENDING;
            }
            case AmplifyCard amplify -> {
                amplify(amplify, amplifyPending, caster);
                yield PlayOutcome.AMPLIFY_PENDING;
            }
        };
    }

    /** Double first, then the defender's active defense applies to this card (DESIGN.md §5). */
    private int attack(AttackCard attack, int factor, Champion caster, Champion opponent) {
        int raw = attack.damage() * factor;
        Optional<ActiveDefense> defense = opponent.activeDefense();
        int damage = defense.map(active -> active.absorb(raw)).orElse(raw);
        opponent.takeDamage(damage);

        String throughDefense = defense.isPresent()
                ? ", " + opponent.name() + "'s " + defense.get().cardName() + " absorbs " + (raw - damage)
                        + " -> " + damage + " damage"
                : "";
        log.line(playPrefix(caster, attack, factor > 1) + raw + " damage" + throughDefense + ". "
                + opponent.name() + ": " + opponent.hp() + " HP");
        return damage;
    }

    /** One active defense per champion; it covers the opponent's next turns (DESIGN.md §5). */
    private void defend(DefenseCard card, boolean amplified, Champion caster, Champion opponent) {
        ActiveDefense defense = amplified ? amplifiedDefense(card) : defense(card);
        caster.raiseDefense(defense);

        String protection = switch (defense.kind()) {
            case REDUCE -> "incoming attack cards deal " + defense.amount() + " less";
            case HALVE -> "incoming attack cards deal half damage";
            case BLOCK -> "every incoming attack card is blocked";
        };
        log.line(playPrefix(caster, card, amplified) + protection + " during " + opponent.name() + "'s "
                + nextTurns(defense.turnsLeft()));
    }

    private static ActiveDefense defense(DefenseCard card) {
        return new ActiveDefense(card.name(), card.kind(), card.amount(), card.duration());
    }

    /** DESIGN.md §4: Guard and Shield reduce twice as much, Barrier blocks everything, Aegis lasts twice as long. */
    private static ActiveDefense amplifiedDefense(DefenseCard card) {
        return switch (card.kind()) {
            case REDUCE -> new ActiveDefense(card.name(), DefenseKind.REDUCE, card.amount() * 2, card.duration());
            case HALVE -> new ActiveDefense(card.name(), DefenseKind.BLOCK, 0, card.duration());
            case BLOCK -> new ActiveDefense(card.name(), DefenseKind.BLOCK, 0, card.duration() * 2);
        };
    }

    /** HP never goes above 30 (DESIGN.md §5). */
    private void heal(HealCard heal, int factor, Champion caster) {
        int amount = heal.amount() * factor;
        int healed = caster.heal(amount);
        String effect = healed == amount
                ? "heals " + healed
                : "heals " + healed + " of " + amount + " (max " + GameRules.MAX_HP + " HP)";
        log.line(playPrefix(caster, heal, factor > 1) + effect + ". " + caster.name() + ": " + caster.hp() + " HP");
    }

    /** Restores mana up to the current capacity, never raises the capacity (DESIGN.md §3). */
    private void restoreMana(ResourceCard resource, int factor, Champion caster) {
        int amount = resource.mana() * factor;
        int restored = caster.restoreMana(amount);
        String gained = restored == amount ? "+" + restored : "+" + restored + " of " + amount;
        log.line(playPrefix(caster, resource, factor > 1) + gained + " mana (" + caster.mana() + "/"
                + caster.capacity() + ")");
    }

    /** Draws from the own pile; stops at 7 cards in hand or on an empty pile (DESIGN.md §5). */
    private void draw(DrawCard draw, int factor, Champion caster) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < draw.count() * factor; i++) {
            Optional<Card> card = caster.draw();
            if (card.isEmpty()) {
                break;
            }
            drawn.add(card.get());
        }
        String effect = drawn.isEmpty()
                ? "draws nothing (" + (caster.isHandFull() ? "hand full" : "draw pile empty") + ")"
                : "draws " + names(drawn) + ". Hand: " + names(caster.hand());
        log.line(playPrefix(caster, draw, factor > 1) + effect);
    }

    /**
     * Takes random cards from the opponent's hand; the stolen cards now belong to the caster.
     * Nothing if their hand is empty; stops at 7 cards in the caster's hand (DESIGN.md §5).
     */
    private void steal(StealCard steal, int factor, Champion caster, Champion opponent) {
        List<Card> taken = new ArrayList<>();
        for (int i = 0; i < steal.count() * factor && !caster.isHandFull() && !opponent.hand().isEmpty(); i++) {
            Card card = opponent.loseRandomCard(random);
            caster.addToHand(card);
            taken.add(card);
        }
        String effect;
        if (!taken.isEmpty()) {
            effect = "takes " + names(taken) + " from " + opponent.name() + ". Hand: " + names(caster.hand());
        } else if (opponent.hand().isEmpty()) {
            effect = "takes nothing (" + opponent.name() + "'s hand is empty)";
        } else {
            effect = "takes nothing (hand full)";
        }
        log.line(playPrefix(caster, steal, factor > 1) + effect);
    }

    /** The next card of this turn is doubled; a second Amplify adds nothing, no x4 (DESIGN.md §5). */
    private void amplify(AmplifyCard amplify, boolean alreadyPending, Champion caster) {
        String effect = alreadyPending
                ? "no effect, the next card is already doubled"
                : "the next card this turn is doubled";
        log.line(playPrefix(caster, amplify, false) + effect);
    }
}
