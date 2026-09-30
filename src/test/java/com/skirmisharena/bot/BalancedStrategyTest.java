package com.skirmisharena.bot;

import com.skirmisharena.card.Card;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.skirmisharena.bot.TestViews.AMPLIFY;
import static com.skirmisharena.bot.TestViews.FOCUS;
import static com.skirmisharena.bot.TestViews.GUARD;
import static com.skirmisharena.bot.TestViews.INSIGHT;
import static com.skirmisharena.bot.TestViews.JAB;
import static com.skirmisharena.bot.TestViews.METEOR;
import static com.skirmisharena.bot.TestViews.PICKPOCKET;
import static com.skirmisharena.bot.TestViews.POTION;
import static com.skirmisharena.bot.TestViews.STRIKE;
import static com.skirmisharena.bot.TestViews.view;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** DESIGN.md §7, Balanced: one attack, one defense, one heal, then more attacks. */
class BalancedStrategyTest {

    private final Strategy balanced = new BalancedStrategy();

    @Test
    void rule1_withAmplifyPendingPlaysTheBestPlayableAttack() {
        BotView view = view().amplifyPending().mana(5, 10).hand(GUARD, STRIKE, AMPLIFY, METEOR).played(AMPLIFY).build();

        assertEquals(Optional.of(METEOR), balanced.nextCard(view));
    }

    @Test
    void rule2_firstAttackOfTheTurnWithAnAmplifyCombo() {
        BotView view = view().mana(10, 10).hand(GUARD, METEOR, AMPLIFY).build();

        assertEquals(Optional.of(AMPLIFY), balanced.nextCard(view));
    }

    @Test
    void rule2_theFirstAttackOfTheTurnComesBeforeTheDefense() {
        BotView view = view().mana(5, 5).hand(GUARD, STRIKE).build();

        assertEquals(Optional.of(STRIKE), balanced.nextCard(view));
    }

    @Test
    void rule3_afterOneAttackTheDefenseComesBeforeMoreAttacks() {
        BotView view = view().mana(3, 5).hand(STRIKE, GUARD).played(JAB).build();

        assertEquals(Optional.of(GUARD), balanced.nextCard(view));
    }

    @Test
    void rule4_oneHealComesBeforeResourcesAndMoreAttacks() {
        BotView view = view().hp(25).defenseActive().mana(2, 5).hand(STRIKE, FOCUS, POTION).played(JAB).build();

        assertEquals(Optional.of(POTION), balanced.nextCard(view));
    }

    @Test
    void rule4_onlyOneHealPerTurnSoTheResourceComesNext() {
        BotView view = view().hp(25).defenseActive().mana(1, 5).hand(STRIKE, POTION, FOCUS).played(JAB, POTION).build();

        assertEquals(Optional.of(FOCUS), balanced.nextCard(view));
    }

    @Test
    void rule6_insightThenPickpocketComeBeforeMoreAttacks() {
        BotView view = view().defenseActive().mana(3, 3).hand(STRIKE, PICKPOCKET, INSIGHT).played(JAB).build();

        assertEquals(Optional.of(INSIGHT), balanced.nextCard(view));
    }

    @Test
    void rule7_moreAttacksComeLast() {
        BotView view = view().defenseActive().mana(3, 3).hand(JAB, STRIKE).played(JAB).build();

        assertEquals(Optional.of(STRIKE), balanced.nextCard(view));
    }

    @Test
    void passesWhenNothingIsUseful() {
        BotView view = view().hp(30).mana(3, 3).hand(POTION, FOCUS).build();

        assertEquals(Optional.<Card>empty(), balanced.nextCard(view));
    }
}
