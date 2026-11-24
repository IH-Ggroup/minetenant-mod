package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProductDetailUiStateTest {

    private static class Recorder implements ProductDetailUiState.Listener {
        final List<String> purchased = new ArrayList<>();
        int backs;
        int retries;

        @Override
        public void onBack() {
            this.backs++;
        }

        @Override
        public void onRetry() {
            this.retries++;
        }

        @Override
        public void onPurchase(String productId) {
            this.purchased.add(productId);
        }
    }

    @Test
    void startsLoadingAndCannotPurchase() {
        ProductDetailUiState state = new ProductDetailUiState(new Recorder());
        assertEquals(ProductDetailUiState.Phase.LOADING, state.phase());
        assertFalse(state.canPurchase());
    }

    @Test
    void availableCanPurchase() {
        Recorder recorder = new Recorder();
        ProductDetailUiState state = new ProductDetailUiState(recorder);
        state.showDetail(ProductDetailFixtures.available());

        assertEquals(ProductDetailUiState.Phase.AVAILABLE, state.phase());
        assertTrue(state.purchase());
        assertEquals(List.of("p-1"), recorder.purchased);
    }

    @Test
    void soldNotFoundErrorAndLoadingCannotPurchase() {
        Recorder recorder = new Recorder();
        ProductDetailUiState state = new ProductDetailUiState(recorder);

        assertFalse(state.purchase());

        state.showDetail(ProductDetailFixtures.sold());
        assertEquals(ProductDetailUiState.Phase.SOLD, state.phase());
        assertFalse(state.purchase());

        state.showNotFound();
        assertFalse(state.purchase());

        state.showError();
        assertFalse(state.purchase());

        assertTrue(recorder.purchased.isEmpty());
    }

    @Test
    void detailIsClearedWhenNotShowingOne() {
        ProductDetailUiState state = new ProductDetailUiState(new Recorder());
        state.showDetail(ProductDetailFixtures.available());
        state.showError();
        assertNull(state.detail());
    }

    @Test
    void retryNotifiesOnlyOnceEvenIfClickedRepeatedly() {
        Recorder recorder = new Recorder();
        ProductDetailUiState state = new ProductDetailUiState(recorder);
        state.showError();

        assertTrue(state.retry());
        assertFalse(state.retry());
        assertFalse(state.retry());
        assertEquals(1, recorder.retries);
        assertEquals(ProductDetailUiState.Phase.LOADING, state.phase());
    }

    @Test
    void retryIsIgnoredWhenNotError() {
        Recorder recorder = new Recorder();
        ProductDetailUiState state = new ProductDetailUiState(recorder);
        state.showNotFound();

        assertFalse(state.retry());
        assertEquals(0, recorder.retries);
    }

    @Test
    void backNotifies() {
        Recorder recorder = new Recorder();
        new ProductDetailUiState(recorder).back();
        assertEquals(1, recorder.backs);
    }

    @Test
    void viewModelFormatsPriceAndStatus() {
        ProductDetailViewModel vm = ProductDetailViewModel.from(ProductDetailFixtures.longText());

        assertEquals("¥1,234,567", vm.priceText());
        assertEquals(ProductSummaryViewModel.STATUS_AVAILABLE_KEY, vm.statusKey());
        assertTrue(vm.purchasable());
    }
}
