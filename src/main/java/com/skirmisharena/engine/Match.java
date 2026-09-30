package com.skirmisharena.engine;

import com.skirmisharena.bot.BotView;
import com.skirmisharena.card.Card;
import com.skirmisharena.card.DefenseCard;
import com.skirmisharena.log.MatchLog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static com.skirmisharena.engine.LogText.names;

/**
 * Runs one match between two champions, turn by turn, through the five phases of DESIGN.md §3:
 * Draw, Mana, Play, Resolve, End. The match ends at once on a KO, or after 50 turns (DESIGN.md §6).
 */
public final class Match {

    /** Safety net against a turn that never ends (a bot or effect loop). Not a game rule. */
    static final int MAX_CARDS_PER_TURN = 50;

    private final Map<Side, Champion> champions = new EnumMap<>(Side.class);
    private final Side firstSide;
    private final MatchLog log;
    private final EffectResolver effects;
    private final Map<Side, Integer> damageDealt = new EnumMap<>(Side.class);
    private boolean played;
    /** Turn state: an Amplify played this turn is waiting for the next card (DESIGN.md §5). */
    private boolean amplifyPending;

    public Match(Champion championA, Champion championB, Side firstSide, Random random, MatchLog log) {
        champions.put(Side.A, Objects.requireNonNull(championA, "championA"));
        champions.put(Side.B, Objects.requireNonNull(championB, "championB"));
        this.firstSide = Objects.requireNonNull(firstSide, "firstSide");
        this.log = Objects.requireNonNull(log, "log");
        this.effects = new EffectResolver(Objects.requireNonNull(random, "random"), log);
        for (Side side : Side.values()) {
            damageDealt.put(side, 0);
        }
    }

    /** Plays the whole match. A match can be played only once. */
    public MatchResult play() {
        if (played) {
            throw new IllegalStateException("a match can be played only once");
        }
        played = true;

        for (Champion champion : champions.values()) {
            champion.drawStartingHand();
            log.line(champion.name() + " starting hand: " + names(champion.hand()));
        }

        Side active = firstSide;
        for (int turn = 1; turn <= GameRules.MAX_TURNS; turn++) {
            Champion me = champions.get(active);
            Champion opponent = champions.get(active.opponent());
            log.line("--- Turn " + turn + ": " + me.name() + " ---");

            drawPhase(me);
            manaPhase(me);
            boolean knockedOut = playPhase(active, me, opponent);
            if (knockedOut) {
                return result(Optional.of(active), turn, EndReason.KO);
            }
            resolvePhase(turn);
            endPhase(turn, me, opponent);

            active = active.opponent();
        }
        return turnLimitResult();
    }

    private void drawPhase(Champion me) {
        for (int i = 0; i < GameRules.CARDS_DRAWN_PER_TURN; i++) {
            Optional<Card> drawn = me.draw();
            if (drawn.isPresent()) {
                log.line(me.name() + " draws " + drawn.get().name() + ". Hand: " + names(me.hand()));
            } else {
                String reason = me.isHandFull() ? "hand full" : "draw pile empty";
                log.line(me.name() + " draws nothing (" + reason + ")");
            }
        }
    }

    private void manaPhase(Champion me) {
        me.startOwnTurnMana();
        log.line(me.name() + " has " + me.mana() + "/" + me.capacity() + " mana");
    }

    /** Asks the bot for cards until it passes. Returns true if the opponent was knocked out. */
    private boolean playPhase(Side active, Champion me, Champion opponent) {
        List<Card> playedThisTurn = new ArrayList<>();
        while (true) {
            Optional<Card> choice = me.strategy().nextCard(viewFor(me, opponent, playedThisTurn));
            if (choice.isEmpty()) {
                log.line(me.name() + " passes, " + me.mana() + " mana unused");
                return false;
            }
            if (playedThisTurn.size() == MAX_CARDS_PER_TURN) {
                throw new IllegalStateException(
                        me.name() + " tried to play more than " + MAX_CARDS_PER_TURN + " cards in one turn");
            }
            Card card = choice.get();
            checkPlayable(me, card);

            me.removeFromHand(card);
            me.pay(card.cost());
            PlayOutcome outcome = effects.apply(card, me, opponent, amplifyPending);
            amplifyPending = outcome.amplifyPending();
            damageDealt.merge(active, outcome.damageDealt(), Integer::sum);
            me.putAtBottom(card);
            playedThisTurn.add(card);

            if (opponent.isKo()) {
                return true;
            }
        }
    }

    /** Effects already applied: Resolve only logs where both champions stand (DESIGN.md §3). */
    private void resolvePhase(int turn) {
        Champion a = champions.get(Side.A);
        Champion b = champions.get(Side.B);
        log.line("After turn " + turn + ": " + a.name() + " " + a.hp() + " HP, " + b.name() + " " + b.hp() + " HP");
    }

    /**
     * Unspent mana and an unused Amplify are lost; the opponent's defense has protected them
     * during this turn, so it counts down (DESIGN.md §3 and §5).
     */
    private void endPhase(int turn, Champion me, Champion opponent) {
        me.loseUnspentMana();
        if (amplifyPending) {
            log.line("End of turn " + turn + ": " + me.name() + "'s Amplify is lost, no card followed it");
            amplifyPending = false;
        }
        opponent.activeDefense().ifPresent(before -> {
            String status = opponent.countDownDefense()
                    .map(after -> " has " + LogText.turnsLeft(after.turnsLeft()))
                    .orElse(" ends");
            log.line("End of turn " + turn + ": " + opponent.name() + "'s " + before.cardName() + status);
        });
    }

    /** Legal play (DESIGN.md §5): in hand, affordable, and no Defense card while one is active. */
    private static void checkPlayable(Champion me, Card card) {
        if (!me.hand().contains(card)) {
            throw new IllegalStateException(me.name() + " tried to play " + card.name() + ", which is not in its hand");
        }
        if (card.cost() > me.mana()) {
            throw new IllegalStateException(me.name() + " tried to play " + card.name() + " (" + card.cost()
                    + " mana) with only " + me.mana() + " mana");
        }
        if (card instanceof DefenseCard && me.activeDefense().isPresent()) {
            throw new IllegalStateException(me.name() + " tried to play " + card.name()
                    + " while it already has an active defense");
        }
    }

    private BotView viewFor(Champion me, Champion opponent, List<Card> playedThisTurn) {
        return new BotView(me.hand(), me.hp(), me.capacity(), me.mana(), me.activeDefense().isPresent(),
                me.drawPileSize(), opponent.hp(), opponent.hand().size(), amplifyPending, playedThisTurn);
    }

    /** After 50 turns, the higher HP wins; equal HP is a draw (DESIGN.md §6). */
    private MatchResult turnLimitResult() {
        int hpA = champions.get(Side.A).hp();
        int hpB = champions.get(Side.B).hp();
        Optional<Side> winner = hpA == hpB ? Optional.empty() : Optional.of(hpA > hpB ? Side.A : Side.B);
        return result(winner, GameRules.MAX_TURNS, EndReason.TURN_LIMIT);
    }

    private MatchResult result(Optional<Side> winner, int turnsPlayed, EndReason endReason) {
        return new MatchResult(winner, turnsPlayed, endReason, damageDealt);
    }
}
