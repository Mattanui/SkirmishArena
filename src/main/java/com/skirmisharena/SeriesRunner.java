package com.skirmisharena;

import com.skirmisharena.card.CardPool;
import com.skirmisharena.engine.Champion;
import com.skirmisharena.engine.Match;
import com.skirmisharena.engine.MatchResult;
import com.skirmisharena.engine.Side;
import com.skirmisharena.log.MatchLog;
import com.skirmisharena.log.NoMatchLog;
import com.skirmisharena.log.TextMatchLog;
import com.skirmisharena.stats.SeriesStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Runs the N matches of a run (DESIGN.md §2, §8 and §9). */
final class SeriesRunner {

    /** The stats of the run, and the full log of match 1. */
    record Outcome(SeriesStats stats, List<String> sampleLog) {
    }

    private SeriesRunner() {
    }

    /**
     * One Random, created from the seed, drives every random choice of every match, one match after
     * the other (DESIGN.md §8): the same options always give the same outcome.
     */
    static Outcome run(Options options) {
        Random random = new Random(options.seed());
        List<MatchResult> results = new ArrayList<>(options.matches());
        TextMatchLog sampleLog = new TextMatchLog();
        MatchLog noLog = new NoMatchLog();

        for (int matchNumber = 1; matchNumber <= options.matches(); matchNumber++) {
            Side first = firstSide(matchNumber);
            MatchLog log = noLog;
            if (matchNumber == 1) {
                log = sampleLog;
                log.line(Match.header(matchNumber, options.botA().label(), options.botB().label(), options.seed(), first));
            }
            Champion a = Champion.withRandomDeck("A", options.botA().newStrategy(), CardPool.standard(), random);
            Champion b = Champion.withRandomDeck("B", options.botB().newStrategy(), CardPool.standard(), random);
            results.add(new Match(a, b, first, random, log).play());
        }
        return new Outcome(SeriesStats.of(results), sampleLog.lines());
    }

    /** The first player alternates: side A on odd matches, side B on even matches (DESIGN.md §2). */
    static Side firstSide(int matchNumber) {
        return matchNumber % 2 == 1 ? Side.A : Side.B;
    }
}
