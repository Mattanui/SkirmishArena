package com.skirmisharena;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The runnable deliverable, end to end: arguments in, stats printed, sample log written. */
class MainTest {

    @TempDir
    Path folder;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void runsTheMatchesPrintsTheStatsAndWritesTheSampleLog() throws IOException {
        Path logFile = folder.resolve("sample.log");

        int exitCode = run("--matches", "20", "--seed", "3", "--log", logFile.toString());

        assertEquals(Main.OK, exitCode);
        String printed = text(out);
        assertTrue(printed.startsWith("Skirmish Arena: 20 matches, Aggressive (A) vs Defensive (B), seed 3"), printed);
        assertTrue(printed.contains("Win rate:"), printed);
        assertTrue(printed.contains("Match length:"), printed);
        assertTrue(printed.contains("Damage per match:"), printed);
        assertTrue(printed.contains("Endings:"), printed);
        List<String> log = Files.readAllLines(logFile);
        assertEquals("=== Match 1: Aggressive (A) vs Defensive (B), seed 3. A starts ===", log.get(0));
        assertTrue(log.get(log.size() - 1).startsWith("=== Result: "));
    }

    @Test
    void theSameOptionsTwiceGiveTheSameStatsAndTheSameLog() throws IOException {
        Path firstLog = folder.resolve("first.log");
        Path secondLog = folder.resolve("second.log");

        run("--matches", "30", "--seed", "11", "--log", firstLog.toString());
        String firstStats = statsLines(text(out));
        out.reset();
        run("--matches", "30", "--seed", "11", "--log", secondLog.toString());
        String secondStats = statsLines(text(out));

        assertEquals(firstStats, secondStats);
        assertEquals(Files.readString(firstLog), Files.readString(secondLog));
    }

    @Test
    void anUnknownOptionPrintsTheErrorAndTheUsage() {
        int exitCode = run("--bots", "aggressive");

        assertEquals(Main.BAD_ARGUMENTS, exitCode);
        assertTrue(text(err).contains("Error: unknown option: --bots"), text(err));
        assertTrue(text(err).contains("Usage:"), text(err));
        assertEquals("", text(out));
    }

    @Test
    void helpPrintsTheUsage() {
        int exitCode = run("--help");

        assertEquals(Main.OK, exitCode);
        assertTrue(text(out).contains("--bot-a <aggressive|defensive|balanced>"), text(out));
    }

    @Test
    void aLogThatCannotBeWrittenIsReported() {
        Path notAFile = folder; // a folder cannot be written as a file

        int exitCode = run("--matches", "1", "--log", notAFile.toString());

        assertEquals(Main.CANNOT_WRITE_LOG, exitCode);
        assertTrue(text(err).contains("cannot write the sample log"), text(err));
    }

    private int run(String... args) {
        return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8);
    }

    /** The printed output without its last line, which names the log file (different on purpose here). */
    private static String statsLines(String printed) {
        return printed.substring(0, printed.lastIndexOf("Sample log"));
    }
}
