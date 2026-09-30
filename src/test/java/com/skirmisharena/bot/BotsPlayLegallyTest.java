package com.skirmisharena.bot;

import com.skirmisharena.card.CardPool;
import com.skirmisharena.engine.Champion;
import com.skirmisharena.engine.Match;
import com.skirmisharena.engine.MatchResult;
import com.skirmisharena.engine.Side;
import com.skirmisharena.log.NoMatchLog;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Every pairing plays 200 full matches with real random decks: the engine throws on any illegal
 * play (unaffordable card, second defense, card not in hand), so this catches a bot that breaks a rule.
 */
class BotsPlayLegallyTest {

    static Stream<Arguments> pairings() {
        return Arrays.stream(BotType.values())
                .flatMap(a -> Arrays.stream(BotType.values()).map(b -> Arguments.of(a, b)));
    }

    @ParameterizedTest(name = "{0} vs {1}")
    @MethodSource("pairings")
    void botsOnlyMakeLegalPlays(BotType botA, BotType botB) {
        Random random = new Random(2026);
        for (int match = 1; match <= 200; match++) {
            Champion a = Champion.withRandomDeck("A", botA.newStrategy(), CardPool.standard(), random);
            Champion b = Champion.withRandomDeck("B", botB.newStrategy(), CardPool.standard(), random);
            Side first = match % 2 == 1 ? Side.A : Side.B;
            Match game = new Match(a, b, first, random, new NoMatchLog());

            MatchResult result = assertDoesNotThrow(game::play, "match " + match);
            assertDoesNotThrow(result::turnsPlayed);
        }
    }
}
