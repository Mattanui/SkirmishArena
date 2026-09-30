package com.skirmisharena.engine;

import com.skirmisharena.card.AmplifyCard;
import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;
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
import static com.skirmisharena.engine.LogText.playPrefix;

/** Applies the effect of a played card, immediately (DESIGN.md §3 and §5), and logs it. */
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
     * @return the damage dealt to the opponent, after defense, overkill included
     */
    int apply(Card card, Champion caster, Champion opponent) {
        return switch (card) {
            case AttackCard attack -> attack(attack, caster, opponent);
            case HealCard heal -> {
                heal(heal, caster);
                yield 0;
            }
            case ResourceCard resource -> {
                restoreMana(resource, caster);
                yield 0;
            }
            case DrawCard draw -> {
                draw(draw, caster);
                yield 0;
            }
            case StealCard steal -> {
                steal(steal, caster, opponent);
                yield 0;
            }
            case DefenseCard _, AmplifyCard _ -> throw new UnsupportedOperationException(
                    card.name() + " is not supported yet: PLAN.md step 6 adds Defense and Amplify");
        };
    }

    private int attack(AttackCard attack, Champion caster, Champion opponent) {
        int damage = attack.damage();
        opponent.takeDamage(damage);
        log.line(playPrefix(caster, attack) + damage + " damage. " + opponent.name() + ": " + opponent.hp() + " HP");
        return damage;
    }

    /** HP never goes above 30 (DESIGN.md §5). */
    private void heal(HealCard heal, Champion caster) {
        int healed = caster.heal(heal.amount());
        String effect = healed == heal.amount()
                ? "heals " + healed
                : "heals " + healed + " of " + heal.amount() + " (max " + GameRules.MAX_HP + " HP)";
        log.line(playPrefix(caster, heal) + effect + ". " + caster.name() + ": " + caster.hp() + " HP");
    }

    /** Restores mana up to the current capacity, never raises the capacity (DESIGN.md §3). */
    private void restoreMana(ResourceCard resource, Champion caster) {
        int restored = caster.restoreMana(resource.mana());
        String amount = restored == resource.mana() ? "+" + restored : "+" + restored + " of " + resource.mana();
        log.line(playPrefix(caster, resource) + amount + " mana (" + caster.mana() + "/" + caster.capacity() + ")");
    }

    /** Draws from the own pile; stops at 7 cards in hand or on an empty pile (DESIGN.md §5). */
    private void draw(DrawCard draw, Champion caster) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < draw.count(); i++) {
            Optional<Card> card = caster.draw();
            if (card.isEmpty()) {
                break;
            }
            drawn.add(card.get());
        }
        String effect = drawn.isEmpty()
                ? "draws nothing (" + (caster.isHandFull() ? "hand full" : "draw pile empty") + ")"
                : "draws " + names(drawn) + ". Hand: " + names(caster.hand());
        log.line(playPrefix(caster, draw) + effect);
    }

    /**
     * Takes random cards from the opponent's hand; the stolen cards now belong to the caster.
     * Nothing if their hand is empty; stops at 7 cards in the caster's hand (DESIGN.md §5).
     */
    private void steal(StealCard steal, Champion caster, Champion opponent) {
        List<Card> taken = new ArrayList<>();
        for (int i = 0; i < steal.count() && !caster.isHandFull() && !opponent.hand().isEmpty(); i++) {
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
        log.line(playPrefix(caster, steal) + effect);
    }
}
