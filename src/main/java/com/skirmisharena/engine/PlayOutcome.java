package com.skirmisharena.engine;

/**
 * What playing one card changed for the rest of the turn: the damage it dealt (after defense,
 * overkill included) and whether an Amplify bonus is now waiting for the next card.
 */
record PlayOutcome(int damageDealt, boolean amplifyPending) {

    static final PlayOutcome NOTHING_PENDING = new PlayOutcome(0, false);
    static final PlayOutcome AMPLIFY_PENDING = new PlayOutcome(0, true);
}
