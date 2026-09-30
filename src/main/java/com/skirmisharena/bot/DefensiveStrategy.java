package com.skirmisharena.bot;

import com.skirmisharena.card.Card;
import com.skirmisharena.engine.GameRules;

import java.util.List;
import java.util.Optional;

import static com.skirmisharena.bot.CardChoices.amplifyCombo;
import static com.skirmisharena.bot.CardChoices.bestHealInHand;
import static com.skirmisharena.bot.CardChoices.bestPlayableHeal;

/**
 * Prioritizes defense and healing once HP drops below 15 (brief). At 15 HP or more it plays
 * exactly like Balanced. The threshold is checked again before every card (DESIGN.md §7).
 */
public final class DefensiveStrategy implements Strategy {

    private static final List<Rule> LOW_HP_RULES = List.of(
            // 1. Amplify pending -> best playable heal
            view -> view.amplifyPending() ? bestPlayableHeal(view) : Optional.empty(),
            // 2. Best playable defense
            CardChoices::bestPlayableDefense,
            // 3. Amplify combo with the best heal in hand, if HP < 30
            view -> view.hp() < GameRules.MAX_HP ? amplifyCombo(view, bestHealInHand(view)) : Optional.empty(),
            // 4. Best useful heal
            CardChoices::bestUsefulHeal,
            // 5. Useful Resource card
            CardChoices::bestUsefulResource,
            // 6. Useful Insight, then useful Pickpocket
            CardChoices::usefulInsight,
            CardChoices::usefulPickpocket,
            // 7. Best playable attack
            CardChoices::bestPlayableAttack);

    private final Strategy aboveThreshold = new BalancedStrategy();

    @Override
    public Optional<Card> nextCard(BotView view) {
        if (view.hp() >= GameRules.DEFENSIVE_THRESHOLD) {
            return aboveThreshold.nextCard(view);
        }
        return Rule.firstMatch(LOW_HP_RULES, view);
    }
}
