package com.minetenant.minetenantclient.gui;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.ProductDetailUiState;
import com.minetenant.minetenantclient.catalog.ProductDetailUiState.Phase;
import com.minetenant.minetenantclient.catalog.ProductDetailViewModel;
import com.minetenant.minetenantclient.catalog.TextLayout;

/**
 * 商品詳細画面。商品名・説明・円価格・状態の表示と、戻る・購入確認へ進む・再試行の操作を担当する。
 * 購入確認へ進めるかの判断は ProductDetailUiState に任せ、ここではボタンの有効/無効に反映するだけ。
 */
public class ProductDetailGui extends BaseScreen {

    private static final int MARGIN = 12;
    private static final int CONTENT_TOP = 36;
    private static final int LINE_HEIGHT = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_GAP = 4;
    private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 12;

    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFAAAAAA;
    private static final int COLOR_PRICE = 0xFFFFD27F;
    private static final int COLOR_AVAILABLE = 0xFF55FF55;
    private static final int COLOR_SOLD = 0xFFAA5555;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private final ProductDetailUiState state;

    private ButtonGeneric purchaseButton;
    private Phase lastPhase;

    public ProductDetailGui(Screen parent, ProductDetailUiState state) {
        this.state = state;
        this.lastPhase = state.phase();
        this.setTitle(StringUtils.translate("minetenantclient.gui.title.product_detail"));
        this.setParent(parent);
    }

    public ProductDetailUiState state() {
        return this.state;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearElements();

        int y = this.getScreenHeight() - FOOTER_HEIGHT + 6;
        int x = MARGIN;

        ButtonGeneric back = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.back"));
        this.addButton(back, (btn, mouseButton) -> this.state.back());
        x += BUTTON_WIDTH + BUTTON_GAP;

        this.purchaseButton = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.purchase"));
        this.purchaseButton.setEnabled(this.state.canPurchase());
        this.addButton(this.purchaseButton, (btn, mouseButton) -> this.state.purchase());
        x += BUTTON_WIDTH + BUTTON_GAP;

        if (this.state.phase() == Phase.ERROR) {
            ButtonGeneric retry = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    StringUtils.translate("minetenantclient.gui.button.retry"));
            this.addButton(retry, (btn, mouseButton) -> this.state.retry());
        }
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        // 状態が変わったら、ボタンの有効/無効と再試行ボタンを作り直す
        if (this.state.phase() != this.lastPhase) {
            this.lastPhase = this.state.phase();
            this.initGui();
        }

        super.drawContents(context, mouseX, mouseY, partialTicks);

        switch (this.state.phase()) {
            case AVAILABLE, SOLD -> this.drawDetail(context, this.state.detail());
            case LOADING -> this.drawCentered(context, "minetenantclient.gui.product_detail.loading", COLOR_INFO);
            case NOT_FOUND -> this.drawCentered(context, "minetenantclient.gui.product_detail.not_found", COLOR_HINT);
            case ERROR -> this.drawCentered(context, "minetenantclient.gui.product_detail.error", COLOR_ERROR);
        }
    }

    private void drawDetail(GuiContext context, ProductDetailViewModel detail) {
        int maxWidth = this.getScreenWidth() - MARGIN * 2;
        int y = CONTENT_TOP;

        // 商品名は長くても2行まで
        for (String line : TextLayout.wrap(detail.name(), maxWidth, 2, this::getStringWidth)) {
            this.drawStringWithShadow(context, line, MARGIN, y, COLOR_INFO);
            y += LINE_HEIGHT;
        }

        y += 4;

        String price = detail.priceText();
        this.drawStringWithShadow(context, price, MARGIN, y, COLOR_PRICE);

        String status = StringUtils.translate(detail.statusKey());
        this.drawStringWithShadow(context, status, MARGIN + this.getStringWidth(price) + 12, y,
                detail.purchasable() ? COLOR_AVAILABLE : COLOR_SOLD);
        y += LINE_HEIGHT + 8;

        // 説明は残りの高さに収まる行数まで。収まらない分は末尾を省略する
        int maxLines = (this.getScreenHeight() - FOOTER_HEIGHT - y) / LINE_HEIGHT;
        List<String> lines = TextLayout.wrap(detail.description(), maxWidth, maxLines, this::getStringWidth);

        for (String line : lines) {
            this.drawStringWithShadow(context, line, MARGIN, y, COLOR_HINT);
            y += LINE_HEIGHT;
        }
    }

    private void drawCentered(GuiContext context, String key, int color) {
        String text = StringUtils.translate(key);
        int x = this.getScreenWidth() / 2 - this.getStringWidth(text) / 2;
        int y = (this.getScreenHeight() - FOOTER_HEIGHT) / 2;
        this.drawStringWithShadow(context, text, x, y, color);
    }
}
