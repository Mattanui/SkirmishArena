package com.skirmisharena.engine;

import com.skirmisharena.card.DefenseKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveDefenseTest {

    @Test
    void invalidValuesAreRejected() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new ActiveDefense(null, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActiveDefense(DefenseKind.REDUCE, -1, 2)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActiveDefense(DefenseKind.BLOCK, 0, 0)));
    }
}
