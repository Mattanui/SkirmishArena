package com.skirmisharena.card;

import java.util.Objects;

/** Argument checks shared by the card records, so a malformed card can never exist. */
final class CardValidation {

    private CardValidation() {
    }

    static void requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    static void requireCost(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be >= 0, was " + cost);
        }
    }

    static void requirePositive(String field, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0, was " + value);
        }
    }
}
