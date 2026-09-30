package com.skirmisharena.bot;

import com.skirmisharena.card.Card;

import java.util.List;
import java.util.Optional;

import static com.skirmisharena.bot.CardChoices.amplifyCombo;
import static com.skirmisharena.bot.CardChoices.bestAttackInHand;
import static com.skirmisharena.bot.CardChoices.bestPlayableAttack;

/** Always plays the highest-damage playable card (brief); the full rule list is DESIGN.md §7. */
public final class AggressiveStrategy implements Strategy {

    private static final List<Rule> RULES = List.of(
            // 1. Amplify pending -> best playable attack
            view -> view.amplifyPending() ? bestPlayableAttack(view) : Optional.empty(),
            // 2. Amplify combo with the best attack in hand
            view -> amplifyCombo(view, bestAttackInHand(view)),
            // 3. Best playable attack
            CardChoices::bestPlayableAttack,
            // 4. No attack playable -> useful Resource card (it may make an attack affordable)
            CardChoices::bestUsefulResource,
            // 5. Useful Insight, then useful Pickpocket (they may bring an attack)
            CardChoices::usefulInsight,
            CardChoices::usefulPickpocket,
            // 6. Best useful heal
            CardChoices::bestUsefulHeal,
            // 7. Best playable defense
            CardChoices::bestPlayableDefense);

    @Override
    public Optional<Card> nextCard(BotView view) {
        return Rule.firstMatch(RULES, view);
    }
}
