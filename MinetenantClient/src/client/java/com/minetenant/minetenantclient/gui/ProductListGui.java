package com.minetenant.minetenantclient.gui;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.ProductListUiState;
import com.minetenant.minetenantclient.catalog.ProductListUiState.Phase;
import com.minetenant.minetenantclient.catalog.ProductSummaryViewModel;
import com.minetenant.minetenantclient.gui.list.ProductListEntryWidget;
import com.minetenant.minetenantclient.gui.list.ProductListWidget;

/**
 * 一点物の商品一覧画面。状態(loading・0件・一覧・失敗)の描画と操作の受け付けだけを担当する。
 * 何を表示するかと、選べる商品かの判断は ProductListUiState に任せる。
 */
public class ProductListGui extends GuiListBase<ProductSummaryViewModel, ProductListEntryWidget, ProductListWidget>
        implements ISelectionListener<ProductSummaryViewModel> {

    private static final int LIST_X = 12;
    private static final int LIST_Y = 30;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_GAP = 4;
    private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + 12;

    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFAAAAAA;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private final ProductListUiState state;
    private Phase lastPhase;
    /** 矢印キーやクリックで直近に選んだ行 */
    private ProductSummaryViewModel highlighted;

    public ProductListGui(Screen parent, ProductListUiState state) {
        super(LIST_X, LIST_Y);

        this.state = state;
        this.lastPhase = state.phase();
        this.setTitle(StringUtils.translate("minetenantclient.gui.title.product_list"));
        this.setParent(parent);
    }

    public ProductListUiState state() {
        return this.state;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearElements();

        int y = this.getScreenHeight() - FOOTER_HEIGHT + 6;
        int x = LIST_X;

        if (this.state.phase() == Phase.ERROR) {
            ButtonGeneric retry = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    StringUtils.translate("minetenantclient.gui.button.retry"));
            this.addButton(retry, (btn, mouseButton) -> this.state.retry());
            x += BUTTON_WIDTH + BUTTON_GAP;
        }

        ButtonGeneric close = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.close"));
        this.addButton(close, (btn, mouseButton) -> this.state.close());
    }

    @Override
    protected ProductListWidget createListWidget(int listX, int listY) {
        return new ProductListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
                this.state.items(), this, entry -> this.state.select(entry.id()));
    }

    @Override
    protected int getBrowserWidth() {
        return this.getScreenWidth() - LIST_X * 2;
    }

    @Override
    protected int getBrowserHeight() {
        return this.getScreenHeight() - LIST_Y - FOOTER_HEIGHT;
    }

    @Override
    protected ISelectionListener<ProductSummaryViewModel> getSelectionListener() {
        return this;
    }

    /** 矢印キーで選んだだけでは遷移しない。選んだ行を覚えておき、Enterかクリックで確定する。 */
    @Override
    public void onSelectionChange(ProductSummaryViewModel entry) {
        this.highlighted = entry;
    }

    /** 矢印キーで行を選び、Enterで確定する。 */
    @Override
    public boolean onKeyTyped(KeyInput input) {
        boolean enter = input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER;

        if (enter && this.state.phase() == Phase.LIST && this.highlighted != null) {
            this.state.select(this.highlighted.id());
            return true;
        }

        return super.onKeyTyped(input);
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        // 状態が変わったら、ボタンと一覧を作り直す
        if (this.state.phase() != this.lastPhase) {
            this.lastPhase = this.state.phase();
            this.highlighted = null;
            this.initGui();
        }

        super.drawContents(context, mouseX, mouseY, partialTicks);

        if (this.state.phase() != Phase.LIST) {
            this.drawMessage(context);
        }
    }

    private void drawMessage(GuiContext context) {
        String key;
        int color;

        switch (this.state.phase()) {
            case LOADING -> {
                key = "minetenantclient.gui.product_list.loading";
                color = COLOR_INFO;
            }
            case EMPTY -> {
                key = "minetenantclient.gui.product_list.empty";
                color = COLOR_HINT;
            }
            default -> {
                key = "minetenantclient.gui.product_list.error";
                color = COLOR_ERROR;
            }
        }

        String text = StringUtils.translate(key);
        int x = this.getScreenWidth() / 2 - this.getStringWidth(text) / 2;
        int y = LIST_Y + this.getBrowserHeight() / 2 - 4;
        this.drawStringWithShadow(context, text, x, y, color);
    }
}
