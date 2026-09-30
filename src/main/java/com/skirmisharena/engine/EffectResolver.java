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

/** Applies the effect of a played card, immediately (DESIGN.md §3 and §5), and logs it. */
final class EffectResolver {

    private final MatchLog log;

    EffectResolver(MatchLog log) {
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
            case DefenseCard _, ResourceCard _, DrawCard _, StealCard _, HealCard _, AmplifyCard _ ->
                    throw new UnsupportedOperationException(
                            card.name() + " is not supported yet: PLAN.md steps 5 and 6 add the other effects");
        };
    }

    private int attack(AttackCard attack, Champion caster, Champion opponent) {
        int damage = attack.damage();
        opponent.takeDamage(damage);
        log.line(playPrefix(caster, attack) + damage + " damage. " + opponent.name() + ": " + opponent.hp() + " HP");
        return damage;
    }

    /** "A plays Strike (2 mana): ", or "A plays Focus: " for a card that costs nothing (DESIGN.md §9). */
    private static String playPrefix(Champion caster, Card card) {
        String cost = card.cost() > 0 ? " (" + card.cost() + " mana)" : "";
        return caster.name() + " plays " + card.name() + cost + ": ";
    }
}
