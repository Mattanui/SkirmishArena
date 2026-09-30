package com.skirmisharena.bot;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** The bots a run can use, by their command-line name (README.md: --bot-a, --bot-b). */
public enum BotType {
    AGGRESSIVE("aggressive", "Aggressive"),
    DEFENSIVE("defensive", "Defensive"),
    BALANCED("balanced", "Balanced");

    private final String commandLineName;
    private final String label;

    BotType(String commandLineName, String label) {
        this.commandLineName = commandLineName;
        this.label = label;
    }

    /** "aggressive", as typed on the command line. */
    public String commandLineName() {
        return commandLineName;
    }

    /** "Aggressive", as shown in the log header. */
    public String label() {
        return label;
    }

    public Strategy newStrategy() {
        return switch (this) {
            case AGGRESSIVE -> new AggressiveStrategy();
            case DEFENSIVE -> new DefensiveStrategy();
            case BALANCED -> new BalancedStrategy();
        };
    }

    /** Case does not matter: "Aggressive" and "aggressive" both work. */
    public static BotType fromCommandLineName(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.commandLineName.equals(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown bot '" + name + "', expected one of: "
                        + Arrays.stream(values()).map(BotType::commandLineName).collect(Collectors.joining(", "))));
    }
}
