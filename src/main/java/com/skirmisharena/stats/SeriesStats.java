package com.skirmisharena.stats;

import com.skirmisharena.engine.EndReason;
import com.skirmisharena.engine.MatchResult;
import com.skirmisharena.engine.Side;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The aggregate stats of a run of N matches (DESIGN.md §9). Averages are per match; the damage
 * is what each side actually dealt, after defense, overkill included.
 */
public record SeriesStats(
        int matches,
        int winsA,
        int winsB,
        int draws,
        int knockouts,
        double averageTurns,
        double averageDamageA,
        double averageDamageB) {

    public static SeriesStats of(List<MatchResult> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("stats need at least one match");
        }
        return new SeriesStats(
                results.size(),
                count(results, result -> result.winner().equals(Optional.of(Side.A))),
                count(results, result -> result.winner().equals(Optional.of(Side.B))),
                count(results, MatchResult::isDraw),
                count(results, result -> result.endReason() == EndReason.KO),
                average(results, MatchResult::turnsPlayed),
                average(results, result -> result.damageDealtBy(Side.A)),
                average(results, result -> result.damageDealtBy(Side.B)));
    }

    /** Percentage of matches won by {@code side}. */
    public double winRate(Side side) {
        return percent(side == Side.A ? winsA : winsB);
    }

    public double drawRate() {
        return percent(draws);
    }

    public int turnLimitEndings() {
        return matches - knockouts;
    }

    /**
     * The printed report. Numbers always use a dot and one decimal (Locale.ROOT), so the output is
     * the same on every machine, whatever its language settings.
     */
    public List<String> reportLines() {
        return List.of(
                format("Win rate:          A %.1f %%, B %.1f %%, draws %.1f %%",
                        winRate(Side.A), winRate(Side.B), drawRate()),
                format("Match length:      %.1f turns on average", averageTurns),
                format("Damage per match:  A %.1f, B %.1f on average", averageDamageA, averageDamageB),
                format("Endings:           KO %d (%.1f %%), turn limit %d (%.1f %%)",
                        knockouts, percent(knockouts), turnLimitEndings(), percent(turnLimitEndings())));
    }

    private double percent(int count) {
        return 100.0 * count / matches;
    }

    private static int count(List<MatchResult> results, Predicate<MatchResult> condition) {
        return (int) results.stream().filter(condition).count();
    }

    private static double average(List<MatchResult> results, ToIntFunction<MatchResult> value) {
        return results.stream().mapToInt(value).average().orElseThrow();
    }

    private static String format(String pattern, Object... values) {
        return String.format(Locale.ROOT, pattern, values);
    }
}
