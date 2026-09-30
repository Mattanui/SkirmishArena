package com.skirmisharena.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotTypeTest {

    @Test
    void eachCommandLineNameGivesItsStrategy() {
        assertAll(
                () -> assertInstanceOf(AggressiveStrategy.class,
                        BotType.fromCommandLineName("aggressive").newStrategy()),
                () -> assertInstanceOf(DefensiveStrategy.class,
                        BotType.fromCommandLineName("defensive").newStrategy()),
                () -> assertInstanceOf(BalancedStrategy.class,
                        BotType.fromCommandLineName("balanced").newStrategy()));
    }

    @Test
    void caseDoesNotMatter() {
        assertEquals(BotType.AGGRESSIVE, BotType.fromCommandLineName("Aggressive"));
    }

    @Test
    void anUnknownNameListsTheValidOnes() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> BotType.fromCommandLineName("random"));

        assertTrue(error.getMessage().contains("aggressive, defensive, balanced"), error.getMessage());
    }
}
