package com.skirmisharena.stats;

import com.skirmisharena.engine.EndReason;
import com.skirmisharena.engine.MatchResult;
import com.skirmisharena.engine.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeriesStatsTest {

    /** 4 hand-made matches: A by KO on turn 20, B on HP, a draw, A on HP. */
    private static final List<MatchResult> FOUR_MATCHES = List.of(
            result(Optional.of(Side.A), 20, EndReason.KO, 32, 0),
            result(Optional.of(Side.B), 50, EndReason.TURN_LIMIT, 10, 20),
            result(Optional.empty(), 50, EndReason.TURN_LIMIT, 5, 5),
            result(Optional.of(Side.A), 50, EndReason.TURN_LIMIT, 21, 11));

    @Test
    void countsWinsDrawsAndEndings() {
        SeriesStats stats = SeriesStats.of(FOUR_MATCHES);

        assertAll(
                () -> assertEquals(4, stats.matches()),
                () -> assertEquals(2, stats.winsA()),
                () -> assertEquals(1, stats.winsB()),
                () -> assertEquals(1, stats.draws()),
                () -> assertEquals(1, stats.knockouts()),
                () -> assertEquals(3, stats.turnLimitEndings()));
    }

    @Test
    void winRatesArePercentagesOfAllMatches() {
        SeriesStats stats = SeriesStats.of(FOUR_MATCHES);

        assertAll(
                () -> assertEquals(50.0, stats.winRate(Side.A)),
                () -> assertEquals(25.0, stats.winRate(Side.B)),
                () -> assertEquals(25.0, stats.drawRate()));
    }

    @Test
    void averagesArePerMatch() {
        SeriesStats stats = SeriesStats.of(FOUR_MATCHES);

        assertAll(
                () -> assertEquals(42.5, stats.averageTurns(), "(20 + 50 + 50 + 50) / 4"),
                () -> assertEquals(17.0, stats.averageDamageA(), "(32 + 10 + 5 + 21) / 4"),
                () -> assertEquals(9.0, stats.averageDamageB(), "(0 + 20 + 5 + 11) / 4"));
    }

    @Test
    void theReportShowsEveryStatOfTheDesign() {
        assertEquals(List.of(
                "Win rate:          A 50.0 %, B 25.0 %, draws 25.0 %",
                "Match length:      42.5 turns on average",
                "Damage per match:  A 17.0, B 9.0 on average",
                "Endings:           KO 1 (25.0 %), turn limit 3 (75.0 %)"), SeriesStats.of(FOUR_MATCHES).reportLines());
    }

    @Test
    void theReportUsesADotEvenOnAFrenchComputer() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            assertEquals("Match length:      42.5 turns on average", SeriesStats.of(FOUR_MATCHES).reportLines().get(1));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void statsNeedAtLeastOneMatch() {
        assertThrows(IllegalArgumentException.class, () -> SeriesStats.of(List.of()));
    }

    private static MatchResult result(Optional<Side> winner, int turns, EndReason reason, int damageA, int damageB) {
        return new MatchResult(winner, turns, reason, Map.of(Side.A, damageA, Side.B, damageB));
    }
}
