package com.skirmisharena;

import com.skirmisharena.bot.BotType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The command line of a run (README.md). Every option is "--name value"; all are optional. */
record Options(BotType botA, BotType botB, int matches, long seed, Path logFile) {

    static final String USAGE = """
            Usage: java -jar target/skirmish-arena.jar [options]
              --bot-a <aggressive|defensive|balanced>  bot for side A (default: aggressive)
              --bot-b <aggressive|defensive|balanced>  bot for side B (default: defensive)
              --matches <N>                            number of matches, at least 1 (default: 1000)
              --seed <number>                          same seed, same results (default: 42)
              --log <file>                             full log of match 1 (default: sample-match.log)
              --help                                   show this help""";

    private static final List<String> KNOWN_OPTIONS = List.of("--bot-a", "--bot-b", "--matches", "--seed", "--log");
    private static final String DEFAULT_BOT_A = "aggressive";
    private static final String DEFAULT_BOT_B = "defensive";
    private static final String DEFAULT_MATCHES = "1000";
    private static final String DEFAULT_SEED = "42";
    private static final String DEFAULT_LOG = "sample-match.log";

    static boolean isHelp(String[] args) {
        return Arrays.asList(args).contains("--help");
    }

    /** @throws IllegalArgumentException with a message for the user when an option is wrong */
    static Options parse(String[] args) {
        Map<String, String> given = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String name = args[i];
            if (!KNOWN_OPTIONS.contains(name)) {
                throw new IllegalArgumentException("unknown option: " + name);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("missing value after " + name);
            }
            if (given.putIfAbsent(name, args[i + 1]) != null) {
                throw new IllegalArgumentException(name + " is given twice");
            }
        }
        return new Options(
                BotType.fromCommandLineName(given.getOrDefault("--bot-a", DEFAULT_BOT_A)),
                BotType.fromCommandLineName(given.getOrDefault("--bot-b", DEFAULT_BOT_B)),
                parseMatches(given.getOrDefault("--matches", DEFAULT_MATCHES)),
                parseSeed(given.getOrDefault("--seed", DEFAULT_SEED)),
                Path.of(given.getOrDefault("--log", DEFAULT_LOG)));
    }

    private static int parseMatches(String value) {
        try {
            int matches = Integer.parseInt(value);
            if (matches >= 1) {
                return matches;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the message below
        }
        throw new IllegalArgumentException("--matches must be a whole number of at least 1, was '" + value + "'");
    }

    private static long parseSeed(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--seed must be a whole number, was '" + value + "'");
        }
    }
}
