package com.minetenant.minetenantclient.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProductListUiStateTest {

    private static class Recorder implements ProductListUiState.Listener {
        final List<String> selected = new ArrayList<>();
        int retries;
        int closes;

        @Override
        public void onSelect(String productId) {
            this.selected.add(productId);
        }

        @Override
        public void onRetry() {
            this.retries++;
        }

        @Override
        public void onClose() {
            this.closes++;
        }
    }

    @Test
    void startsLoading() {
        assertEquals(ProductListUiState.Phase.LOADING, new ProductListUiState(new Recorder()).phase());
    }

    @Test
    void emptyResultBecomesEmpty() {
        ProductListUiState state = new ProductListUiState(new Recorder());
        state.showProducts(List.of());
        assertEquals(ProductListUiState.Phase.EMPTY, state.phase());
    }

    @Test
    void productsBecomeList() {
        ProductListUiState state = new ProductListUiState(new Recorder());
        state.showProducts(ProductListFixtures.withSold());
        assertEquals(ProductListUiState.Phase.LIST, state.phase());
        assertEquals(4, state.items().size());
    }

    @Test
    void onlyAvailableCanBeSelected() {
        Recorder recorder = new Recorder();
        ProductListUiState state = new ProductListUiState(recorder);
        state.showProducts(ProductListFixtures.withSold());

        assertTrue(state.select("p-1"));
        assertFalse(state.select("p-2"));
        assertFalse(state.select("unknown"));
        assertEquals(List.of("p-1"), recorder.selected);
    }

    @Test
    void cannotSelectOutsideList() {
        Recorder recorder = new Recorder();
        ProductListUiState state = new ProductListUiState(recorder);

        assertFalse(state.select("p-1"));
        state.showError();
        assertFalse(state.select("p-1"));
        assertTrue(recorder.selected.isEmpty());
    }

    @Test
    void retryNotifiesOnlyOnceEvenIfClickedRepeatedly() {
        Recorder recorder = new Recorder();
        ProductListUiState state = new ProductListUiState(recorder);
        state.showError();

        assertTrue(state.retry());
        assertFalse(state.retry());
        assertFalse(state.retry());
        assertEquals(1, recorder.retries);
        assertEquals(ProductListUiState.Phase.LOADING, state.phase());
    }

    @Test
    void retryIsIgnoredWhenNotError() {
        Recorder recorder = new Recorder();
        ProductListUiState state = new ProductListUiState(recorder);
        state.showProducts(ProductListFixtures.available());

        assertFalse(state.retry());
        assertEquals(0, recorder.retries);
    }

    @Test
    void closeNotifies() {
        Recorder recorder = new Recorder();
        new ProductListUiState(recorder).close();
        assertEquals(1, recorder.closes);
    }

    @Test
    void viewModelFormatsPriceAndStatus() {
        ProductSummaryViewModel available = ProductSummaryViewModel.from(
                new ProductSummary("a", "s", "名前", 1234567, ProductStatus.AVAILABLE));
        ProductSummaryViewModel sold = ProductSummaryViewModel.from(
                new ProductSummary("b", "s", "名前", 0, ProductStatus.SOLD));

        assertEquals("¥1,234,567", available.priceText());
        assertEquals(ProductSummaryViewModel.STATUS_AVAILABLE_KEY, available.statusKey());
        assertTrue(available.selectable());
        assertEquals("¥0", sold.priceText());
        assertEquals(ProductSummaryViewModel.STATUS_SOLD_KEY, sold.statusKey());
        assertFalse(sold.selectable());
    }
}
