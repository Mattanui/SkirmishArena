package com.skirmisharena.bot;

import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.HealCard;

import java.util.List;
import java.util.Optional;

import static com.skirmisharena.bot.CardChoices.amplifyCombo;
import static com.skirmisharena.bot.CardChoices.bestAttackInHand;
import static com.skirmisharena.bot.CardChoices.bestPlayableAttack;
import static com.skirmisharena.bot.CardChoices.bestUsefulHeal;
import static com.skirmisharena.bot.CardChoices.playedThisTurn;

/** One attack, one defense, one heal, then more attacks: the team's third bot (DESIGN.md §7). */
public final class BalancedStrategy implements Strategy {

    private static final List<Rule> RULES = List.of(
            // 1. Amplify pending -> best playable attack
            view -> view.amplifyPending() ? bestPlayableAttack(view) : Optional.empty(),
            // 2. No attack played yet this turn -> Amplify combo with the best attack in hand...
            view -> noAttackYet(view) ? amplifyCombo(view, bestAttackInHand(view)) : Optional.empty(),
            // ... otherwise best playable attack
            view -> noAttackYet(view) ? bestPlayableAttack(view) : Optional.empty(),
            // 3. Best playable defense
            CardChoices::bestPlayableDefense,
            // 4. No heal played yet this turn -> best useful heal
            view -> playedThisTurn(view, HealCard.class) ? Optional.empty() : bestUsefulHeal(view),
            // 5. Useful Resource card
            CardChoices::bestUsefulResource,
            // 6. Useful Insight, then useful Pickpocket
            CardChoices::usefulInsight,
            CardChoices::usefulPickpocket,
            // 7. Best playable attack
            CardChoices::bestPlayableAttack);

    @Override
    public Optional<Card> nextCard(BotView view) {
        return Rule.firstMatch(RULES, view);
    }

    private static boolean noAttackYet(BotView view) {
        return !playedThisTurn(view, AttackCard.class);
    }
}
