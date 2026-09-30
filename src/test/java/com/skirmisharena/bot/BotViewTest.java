package com.skirmisharena.bot;

import com.skirmisharena.card.AttackCard;
import com.skirmisharena.card.Card;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BotViewTest {

    private static final Card JAB = new AttackCard("Jab", 1, 1);

    @Test
    void aBotCannotChangeTheHandThroughTheView() {
        List<Card> realHand = new ArrayList<>(List.of(JAB));
        BotView view = new BotView(realHand, 30, 1, 1, false, 19, 30, 3, false, new ArrayList<>());

        assertThrows(UnsupportedOperationException.class, () -> view.hand().clear());
        assertThrows(UnsupportedOperationException.class, () -> view.cardsPlayedThisTurn().add(JAB));
    }

    @Test
    void theViewIsASnapshotNotALiveWindow() {
        List<Card> realHand = new ArrayList<>(List.of(JAB));
        BotView view = new BotView(realHand, 30, 1, 1, false, 19, 30, 3, false, List.of());

        realHand.clear();

        assertEquals(List.of(JAB), view.hand());
    }
}
