package com.skirmisharena;

import com.skirmisharena.bot.BotType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The command line of README.md: names, defaults and error messages. */
class OptionsTest {

    @Test
    void withoutArgumentsTheReadmeDefaultsApply() {
        assertEquals(new Options(BotType.AGGRESSIVE, BotType.DEFENSIVE, 1000, 42, Path.of("sample-match.log")),
                Options.parse(new String[0]));
    }

    @Test
    void everyOptionIsRead() {
        Options options = Options.parse(new String[] {
                "--bot-a", "balanced", "--bot-b", "aggressive", "--matches", "250", "--seed", "7", "--log", "out.log"});

        assertEquals(new Options(BotType.BALANCED, BotType.AGGRESSIVE, 250, 7, Path.of("out.log")), options);
    }

    @Test
    void anUnknownOptionIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Options.parse(new String[] {"--bots", "aggressive"}));

        assertEquals("unknown option: --bots", error.getMessage());
    }

    @Test
    void anOptionWithoutValueIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Options.parse(new String[] {"--seed"}));

        assertEquals("missing value after --seed", error.getMessage());
    }

    @Test
    void anOptionGivenTwiceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Options.parse(new String[] {"--seed", "1", "--seed", "2"}));
    }

    @Test
    void matchesMustBeAWholeNumberOfAtLeast1() {
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--matches", "0"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--matches", "ten"}));
    }

    @Test
    void theSeedMustBeAWholeNumber() {
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seed", "4.2"}));
    }

    @Test
    void anUnknownBotIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--bot-a", "random"}));
    }

    @Test
    void helpIsRecognizedAnywhere() {
        assertTrue(Options.isHelp(new String[] {"--seed", "3", "--help"}));
        assertFalse(Options.isHelp(new String[] {"--seed", "3"}));
    }
}
