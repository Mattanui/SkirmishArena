package com.skirmisharena;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Entry point: runs N matches between two bots, prints the stats and writes the full log of
 * match 1 (DESIGN.md §9, README.md for the options).
 */
public final class Main {

    static final int OK = 0;
    static final int CANNOT_WRITE_LOG = 1;
    static final int BAD_ARGUMENTS = 2;

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != OK) {
            System.exit(exitCode);
        }
    }

    /** Does the whole run and returns the process exit code. Separate from main() so tests can call it. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (Options.isHelp(args)) {
            out.println(Options.USAGE);
            return OK;
        }
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.println(Options.USAGE);
            return BAD_ARGUMENTS;
        }

        SeriesRunner.Outcome outcome = SeriesRunner.run(options);

        try {
            // "\n" on every system, so the log file is identical on Windows, macOS and Linux.
            Files.writeString(options.logFile(), String.join("\n", outcome.sampleLog()) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            err.println("Error: cannot write the sample log to " + options.logFile() + ": " + e.getMessage());
            return CANNOT_WRITE_LOG;
        }

        out.println("Skirmish Arena: " + options.matches() + " matches, " + options.botA().label() + " (A) vs "
                + options.botB().label() + " (B), seed " + options.seed());
        outcome.stats().reportLines().forEach(out::println);
        out.println("Sample log (match 1): " + options.logFile());
        return OK;
    }
}
