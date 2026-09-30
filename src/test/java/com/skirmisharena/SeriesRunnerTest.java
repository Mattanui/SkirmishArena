package com.skirmisharena;

import com.skirmisharena.bot.BotType;
import com.skirmisharena.engine.Side;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeriesRunnerTest {

    private static Options options(long seed) {
        return new Options(BotType.AGGRESSIVE, BotType.BALANCED, 50, seed, Path.of("unused.log"));
    }

    @Test
    void theSameOptionsGiveTheSameStatsAndTheSameLog() {
        SeriesRunner.Outcome first = SeriesRunner.run(options(42));
        SeriesRunner.Outcome second = SeriesRunner.run(options(42));

        assertEquals(first.stats(), second.stats());
        assertEquals(first.sampleLog(), second.sampleLog());
    }

    @Test
    void anotherSeedGivesAnotherRun() {
        assertNotEquals(SeriesRunner.run(options(42)).sampleLog(), SeriesRunner.run(options(43)).sampleLog());
    }

    @Test
    void theStatsCoverEveryMatch() {
        assertEquals(50, SeriesRunner.run(options(42)).stats().matches());
    }

    @Test
    void theSampleLogIsMatch1FromHeaderToResult() {
        List<String> log = SeriesRunner.run(options(42)).sampleLog();

        assertEquals("=== Match 1: Aggressive (A) vs Balanced (B), seed 42. A starts ===", log.get(0));
        assertTrue(log.get(log.size() - 1).startsWith("=== Result: "), log.get(log.size() - 1));
    }

    @Test
    void theFirstPlayerAlternatesAStartsOddMatches() {
        assertEquals(List.of(Side.A, Side.B, Side.A, Side.B),
                List.of(SeriesRunner.firstSide(1), SeriesRunner.firstSide(2),
                        SeriesRunner.firstSide(3), SeriesRunner.firstSide(4)));
    }
}
