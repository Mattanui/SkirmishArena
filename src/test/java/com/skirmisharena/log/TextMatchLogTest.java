package com.skirmisharena.log;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextMatchLogTest {

    @Test
    void keepsLinesInOrderAndJoinsThemWithNewlines() {
        TextMatchLog log = new TextMatchLog();
        log.line("first");
        log.line("second");

        assertEquals(List.of("first", "second"), log.lines());
        assertEquals("first\nsecond", log.text());
    }
}
