package com.skirmisharena.card;

/** How an active defense changes each incoming attack card (DESIGN.md §5). */
public enum DefenseKind {
    /** The attack card deals {@code amount} less, never below 0. */
    REDUCE,
    /** The attack card deals half its damage, rounded down. */
    HALVE,
    /** The attack card deals no damage. */
    BLOCK
}
