package com.skirmisharena.bot;

import com.skirmisharena.card.Card;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.skirmisharena.bot.TestViews.AMPLIFY;
import static com.skirmisharena.bot.TestViews.FOCUS;
import static com.skirmisharena.bot.TestViews.GUARD;
import static com.skirmisharena.bot.TestViews.HEAVY_BLOW;
import static com.skirmisharena.bot.TestViews.INSIGHT;
import static com.skirmisharena.bot.TestViews.JAB;
import static com.skirmisharena.bot.TestViews.METEOR;
import static com.skirmisharena.bot.TestViews.PICKPOCKET;
import static com.skirmisharena.bot.TestViews.POTION;
import static com.skirmisharena.bot.TestViews.STRIKE;
import static com.skirmisharena.bot.TestViews.SURGE;
import static com.skirmisharena.bot.TestViews.view;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** DESIGN.md §7, Aggressive: each test shows one rule winning over the rules below it. */
class AggressiveStrategyTest {

    private final Strategy aggressive = new AggressiveStrategy();

    @Test
    void rule1_withAmplifyPendingPlaysTheBestPlayableAttackNotASecondAmplify() {
        BotView view = view().amplifyPending().mana(5, 10).hand(AMPLIFY, STRIKE, METEOR).build();

        assertEquals(Optional.of(METEOR), aggressive.nextCard(view));
    }

    @Test
    void rule2_with10ManaPlaysAmplifyThenMeteor() {
        BotView first = view().mana(10, 10).hand(METEOR, AMPLIFY).build();
        assertEquals(Optional.of(AMPLIFY), aggressive.nextCard(first));

        BotView second = view().amplifyPending().mana(5, 10).hand(METEOR).played(AMPLIFY).build();
        assertEquals(Optional.of(METEOR), aggressive.nextCard(second));
    }

    @Test
    void rule2_noComboWhenTheBestAttackWouldNotBeAffordableAfterAmplify() {
        // 7 mana: Amplify (5) would leave 2, not enough for Meteor (5): Meteor first.
        BotView view = view().mana(7, 7).hand(METEOR, AMPLIFY).build();

        assertEquals(Optional.of(METEOR), aggressive.nextCard(view));
    }

    @Test
    void rule3_theHighestDamagePlayableAttackBeatsEveryOtherCard() {
        // Meteor is not affordable with 3 mana: Heavy Blow (4 damage) beats Jab and all non-attacks.
        BotView view = view().hp(20).mana(3, 5).hand(FOCUS, INSIGHT, POTION, GUARD, JAB, METEOR, HEAVY_BLOW).build();

        assertEquals(Optional.of(HEAVY_BLOW), aggressive.nextCard(view));
    }

    @Test
    void rule4_noAttackPlayableThenTheResourceRestoringTheMost() {
        // 1 of 5 mana: 4 missing, Surge restores 2, Focus 1. Resource beats Insight, heal and defense.
        BotView view = view().hp(20).mana(1, 5).hand(METEOR, FOCUS, SURGE, INSIGHT, POTION, GUARD).build();

        assertEquals(Optional.of(SURGE), aggressive.nextCard(view));
    }

    @Test
    void rule4_onATieTheSmallerResourceIsPlayedAndTheBiggerOneKept() {
        // 4 of 5 mana: 1 missing, Focus and Surge both restore 1.
        BotView view = view().mana(4, 5).hand(METEOR, SURGE, FOCUS).build();

        assertEquals(Optional.of(FOCUS), aggressive.nextCard(view));
    }

    @Test
    void rule4_aResourceIsNotPlayedAtFullMana() {
        BotView view = view().mana(3, 3).hand(METEOR, FOCUS, INSIGHT).build();

        assertEquals(Optional.of(INSIGHT), aggressive.nextCard(view));
    }

    @Test
    void rule5_insightComesBeforePickpocketHealAndDefense() {
        BotView view = view().hp(20).mana(3, 3).hand(PICKPOCKET, INSIGHT, POTION, GUARD).build();

        assertEquals(Optional.of(INSIGHT), aggressive.nextCard(view));
    }

    @Test
    void rule5_insightIsUselessOnAnEmptyPileSoPickpocketComesNext() {
        BotView view = view().hp(20).pileSize(0).mana(3, 3).hand(INSIGHT, PICKPOCKET, POTION, GUARD).build();

        assertEquals(Optional.of(PICKPOCKET), aggressive.nextCard(view));
    }

    @Test
    void rule6_theHealComesBeforeTheDefenseWhenHurt() {
        BotView view = view().hp(25).pileSize(0).opponentHandSize(0).mana(3, 3)
                .hand(INSIGHT, PICKPOCKET, GUARD, POTION).build();

        assertEquals(Optional.of(POTION), aggressive.nextCard(view));
    }

    @Test
    void rule7_atFullHpTheDefenseIsPlayed() {
        BotView view = view().hp(30).mana(3, 3).hand(POTION, GUARD).build();

        assertEquals(Optional.of(GUARD), aggressive.nextCard(view));
    }

    @Test
    void passesWhenNoRuleApplies() {
        BotView view = view().hp(30).defenseActive().mana(2, 5).hand(METEOR, GUARD, POTION).build();

        assertEquals(Optional.<Card>empty(), aggressive.nextCard(view));
    }

    @Test
    void strikeBeatsJabWhenBothAreAffordable() {
        BotView view = view().mana(2, 2).hand(JAB, STRIKE).build();

        assertEquals(Optional.of(STRIKE), aggressive.nextCard(view));
    }
}
