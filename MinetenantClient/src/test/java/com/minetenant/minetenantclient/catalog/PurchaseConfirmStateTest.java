package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PurchaseConfirmStateTest {

    private static class Recorder implements PurchaseConfirmState.Listener {
        final List<String> confirmed = new ArrayList<>();
        int backs;

        @Override
        public void onConfirm(String productId) {
            this.confirmed.add(productId);
        }

        @Override
        public void onBack() {
            this.backs++;
        }
    }

    private static PurchaseConfirmState availableState(Recorder recorder) {
        return PurchaseConfirmState
                .open(ProductDetailViewModel.from(ProductDetailFixtures.available()), recorder)
                .orElseThrow();
    }

    @Test
    void soldProductDoesNotOpenConfirm() {
        Recorder recorder = new Recorder();

        assertTrue(PurchaseConfirmState
                .open(ProductDetailViewModel.from(ProductDetailFixtures.sold()), recorder).isEmpty());
    }

    @Test
    void confirmNotifiesOnceAndMovesToLoading() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);

        assertTrue(state.confirm());

        assertEquals(List.of("p-1"), recorder.confirmed);
        assertEquals(PurchaseConfirmState.Phase.LOADING, state.phase());
        assertFalse(state.isInteractive());
    }

    @Test
    void repeatedConfirmDoesNotSendTwice() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);

        state.confirm();
        assertFalse(state.confirm());
        assertFalse(state.confirm());

        assertEquals(1, recorder.confirmed.size());
    }

    @Test
    void backSendsNothing() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);

        assertTrue(state.back());

        assertEquals(1, recorder.backs);
        assertTrue(recorder.confirmed.isEmpty());
    }

    @Test
    void cannotGoBackOrConfirmAfterConfirm() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);
        state.confirm();

        assertFalse(state.back());
        assertEquals(0, recorder.backs);
    }

    @Test
    void focusStartsOnBackSoEnterDoesNotConfirmByAccident() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);

        assertEquals(PurchaseConfirmState.Button.BACK, state.focus());
        state.activateFocused();

        assertEquals(1, recorder.backs);
        assertTrue(recorder.confirmed.isEmpty());
    }

    @Test
    void focusMovesAndEnterActivatesFocusedButton() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);

        state.moveFocus();
        assertEquals(PurchaseConfirmState.Button.CONFIRM, state.focus());

        assertTrue(state.activateFocused());
        assertFalse(state.activateFocused());
        assertEquals(1, recorder.confirmed.size());
    }

    @Test
    void focusDoesNotMoveAfterConfirm() {
        Recorder recorder = new Recorder();
        PurchaseConfirmState state = availableState(recorder);
        state.moveFocus();
        state.confirm();

        state.moveFocus();

        assertEquals(PurchaseConfirmState.Button.CONFIRM, state.focus());
    }
}
