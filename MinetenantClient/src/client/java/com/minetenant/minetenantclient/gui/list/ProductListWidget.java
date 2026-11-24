package com.minetenant.minetenantclient.gui.list;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import com.minetenant.minetenantclient.catalog.ProductSummaryViewModel;

/** 商品一覧のスクロール付きリスト。行のクリックだけを通知し、選べるかどうかの判断は状態側に任せる。 */
public class ProductListWidget extends WidgetListBase<ProductSummaryViewModel, ProductListEntryWidget> {

    private static final int ENTRY_HEIGHT = 22;

    private final List<ProductSummaryViewModel> items;
    private final Consumer<ProductSummaryViewModel> onEntryClick;

    public ProductListWidget(int x, int y, int width, int height, List<ProductSummaryViewModel> items,
            ISelectionListener<ProductSummaryViewModel> selectionListener,
            Consumer<ProductSummaryViewModel> onEntryClick) {
        super(x, y, width, height, selectionListener);

        this.items = items;
        this.onEntryClick = onEntryClick;
        this.browserEntryHeight = ENTRY_HEIGHT;
        this.allowKeyboardNavigation = true;
        this.allowMultiSelection = false;
        this.shouldSortList = false;
    }

    @Override
    protected Collection<ProductSummaryViewModel> getAllEntries() {
        return this.items;
    }

    @Override
    protected ProductListEntryWidget createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
            ProductSummaryViewModel entry) {
        return new ProductListEntryWidget(x, y, this.browserEntryWidth, this.browserEntryHeight, isOdd, entry,
                listIndex);
    }

    @Override
    protected boolean onEntryClicked(ProductSummaryViewModel entry, int mouseButton) {
        this.onEntryClick.accept(entry);
        return super.onEntryClicked(entry, mouseButton);
    }
}
