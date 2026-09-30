package com.skirmisharena.bot;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.skirmisharena.bot.TestViews.AEGIS;
import static com.skirmisharena.bot.TestViews.AMPLIFY;
import static com.skirmisharena.bot.TestViews.BARRIER;
import static com.skirmisharena.bot.TestViews.ELIXIR;
import static com.skirmisharena.bot.TestViews.FOCUS;
import static com.skirmisharena.bot.TestViews.GUARD;
import static com.skirmisharena.bot.TestViews.INSIGHT;
import static com.skirmisharena.bot.TestViews.JAB;
import static com.skirmisharena.bot.TestViews.METEOR;
import static com.skirmisharena.bot.TestViews.PICKPOCKET;
import static com.skirmisharena.bot.TestViews.POTION;
import static com.skirmisharena.bot.TestViews.SHIELD;
import static com.skirmisharena.bot.TestViews.STRIKE;
import static com.skirmisharena.bot.TestViews.SURGE;
import static com.skirmisharena.bot.TestViews.view;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** DESIGN.md §7, Defensive: Balanced at 15 HP or more, its own rules below 15. */
class DefensiveStrategyTest {

    private final Strategy defensive = new DefensiveStrategy();
    private final Strategy balanced = new BalancedStrategy();

    @Test
    void at15HpItPlaysExactlyLikeBalanced() {
        BotView view = view().hp(15).mana(10, 10).hand(METEOR, GUARD).build();

        assertEquals(Optional.of(METEOR), defensive.nextCard(view));
        assertEquals(balanced.nextCard(view), defensive.nextCard(view));
    }

    @Test
    void at14HpItPicksADefenseOverMeteor() {
        BotView view = view().hp(14).mana(10, 10).hand(METEOR, GUARD).build();

        assertEquals(Optional.of(GUARD), defensive.nextCard(view));
    }

    @Test
    void rule1_withAmplifyPendingPlaysTheBestPlayableHealBeforeAnyDefense() {
        BotView view = view().hp(10).amplifyPending().mana(5, 10).hand(GUARD, POTION, ELIXIR, METEOR).build();

        assertEquals(Optional.of(ELIXIR), defensive.nextCard(view));
    }

    @Test
    void rule2_theStrongestPlayableDefenseComesBeforeHeals() {
        BotView view = view().hp(10).mana(3, 3).hand(ELIXIR, GUARD, SHIELD, BARRIER, AEGIS).build();

        assertEquals(Optional.of(BARRIER), defensive.nextCard(view), "Aegis costs 5: Barrier is the best affordable");
    }

    @Test
    void rule2_aegisIsTheStrongestDefense() {
        BotView view = view().hp(10).mana(5, 5).hand(GUARD, SHIELD, BARRIER, AEGIS).build();

        assertEquals(Optional.of(AEGIS), defensive.nextCard(view));
    }

    @Test
    void rule3_amplifyComboWithTheBestHealWhenBothAreAffordable() {
        // Defense already active. 8 mana = Amplify (5) + Elixir (3).
        BotView view = view().hp(10).defenseActive().mana(8, 8).hand(ELIXIR, AMPLIFY).build();

        assertEquals(Optional.of(AMPLIFY), defensive.nextCard(view));
    }

    @Test
    void rule4_withoutEnoughManaForTheComboTheHealIsPlayedAlone() {
        BotView view = view().hp(10).defenseActive().mana(7, 7).hand(ELIXIR, AMPLIFY).build();

        assertEquals(Optional.of(ELIXIR), defensive.nextCard(view));
    }

    @Test
    void rule4_theBiggestHealComesBeforeResources() {
        BotView view = view().hp(10).defenseActive().mana(2, 5).hand(SURGE, POTION).build();

        assertEquals(Optional.of(POTION), defensive.nextCard(view));
    }

    @Test
    void rule5_aResourceComesBeforeDrawingAndAttacking() {
        BotView view = view().hp(10).defenseActive().mana(0, 5).hand(INSIGHT, JAB, FOCUS).build();

        assertEquals(Optional.of(FOCUS), defensive.nextCard(view));
    }

    @Test
    void rule6_insightThenPickpocketComeBeforeAttacking() {
        BotView view = view().hp(10).defenseActive().mana(3, 3).hand(JAB, PICKPOCKET, INSIGHT).build();

        assertEquals(Optional.of(INSIGHT), defensive.nextCard(view));
    }

    @Test
    void rule7_attacksComeLast() {
        BotView view = view().hp(10).defenseActive().mana(2, 2).hand(JAB, STRIKE).build();

        assertEquals(Optional.of(STRIKE), defensive.nextCard(view));
    }
}
