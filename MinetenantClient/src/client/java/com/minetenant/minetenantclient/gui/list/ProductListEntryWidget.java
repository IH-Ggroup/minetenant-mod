package com.minetenant.minetenantclient.gui.list;

import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.ProductSummaryViewModel;

/**
 * 一覧の1行。商品名・円価格・状態を表示する。
 * soldの行は薄く表示し、選べないことを見た目でも示す。
 */
public class ProductListEntryWidget extends WidgetListEntryBase<ProductSummaryViewModel> {

    private static final int PADDING = 4;
    private static final int GAP = 8;

    private static final int COLOR_ROW_ODD = 0x70FFFFFF;
    private static final int COLOR_ROW_EVEN = 0x30FFFFFF;
    private static final int COLOR_HOVER = 0x70FFFFFF;
    private static final int COLOR_SELECTED = 0x90FFFFFF;
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_NAME_SOLD = 0xFF777777;
    private static final int COLOR_PRICE = 0xFFFFD27F;
    private static final int COLOR_AVAILABLE = 0xFF55FF55;
    private static final int COLOR_SOLD = 0xFFAA5555;

    private static final String ELLIPSIS = "...";

    private final boolean isOdd;

    public ProductListEntryWidget(int x, int y, int width, int height, boolean isOdd,
            ProductSummaryViewModel entry, int listIndex) {
        super(x, y, width, height, entry, listIndex);
        this.isOdd = isOdd;
    }

    @Override
    public void render(GuiContext context, int mouseX, int mouseY, boolean selected) {
        int background = selected ? COLOR_SELECTED
                : this.isMouseOver(mouseX, mouseY) && this.entry.selectable() ? COLOR_HOVER
                        : this.isOdd ? COLOR_ROW_ODD : COLOR_ROW_EVEN;

        RenderUtils.drawRect(context, this.x, this.y, this.width, this.height, background);

        String status = StringUtils.translate(this.entry.statusKey());
        String price = this.entry.priceText();
        int statusWidth = this.getStringWidth(status);
        int priceWidth = this.getStringWidth(price);

        int textY = this.y + (this.height - this.fontHeight) / 2 + 1;
        int right = this.x + this.width - PADDING;

        // 右から 状態 → 価格 の順に置き、残った幅に商品名を収める
        int statusX = right - statusWidth;
        int priceX = statusX - GAP - priceWidth;
        int nameMaxWidth = priceX - GAP - (this.x + PADDING);

        boolean sold = !this.entry.selectable();

        this.drawStringWithShadow(context, this.x + PADDING, textY, sold ? COLOR_NAME_SOLD : COLOR_NAME,
                this.clip(this.entry.name(), nameMaxWidth));
        this.drawStringWithShadow(context, priceX, textY, sold ? COLOR_NAME_SOLD : COLOR_PRICE, price);
        this.drawStringWithShadow(context, statusX, textY, sold ? COLOR_SOLD : COLOR_AVAILABLE, status);

        super.render(context, mouseX, mouseY, selected);
    }

    /** 幅に収まらない商品名は末尾を省略する。 */
    private String clip(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }

        if (this.getStringWidth(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = this.getStringWidth(ELLIPSIS);
        int end = text.length();

        while (end > 0 && this.getStringWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }

        return text.substring(0, end) + ELLIPSIS;
    }
}
