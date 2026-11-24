package com.minetenant.minetenantclient.gui;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.catalog.PurchaseConfirmState;
import com.minetenant.minetenantclient.catalog.PurchaseConfirmState.Button;
import com.minetenant.minetenantclient.catalog.PurchaseConfirmState.Phase;
import com.minetenant.minetenantclient.catalog.ProductDetailViewModel;
import com.minetenant.minetenantclient.catalog.TextLayout;

/**
 * 購入確認画面。商品名・円価格・一点物であることを見せ、「購入を確定」か「戻る」を選ばせる。
 * 確定の受け付けと二重送信の防止は PurchaseConfirmState に任せ、ここでは描画とボタン操作だけを担当する。
 * 左右・Tabでfocusを動かし、Enterでfocus中のボタンを押す。最初のfocusは「戻る」。
 */
public class PurchaseConfirmGui extends BaseScreen {

    private static final int BUTTON_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int MAX_TEXT_WIDTH = 320;

    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFAAAAAA;
    private static final int COLOR_PRICE = 0xFFFFD27F;
    private static final int COLOR_FOCUS = 0xFFFFFFFF;

    private final PurchaseConfirmState state;

    private ButtonGeneric confirmButton;
    private ButtonGeneric backButton;
    private Phase lastPhase;

    public PurchaseConfirmGui(Screen parent, PurchaseConfirmState state) {
        this.state = state;
        this.lastPhase = state.phase();
        this.setTitle(StringUtils.translate("minetenantclient.gui.title.purchase_confirm"));
        this.setParent(parent);
    }

    public PurchaseConfirmState state() {
        return this.state;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearElements();

        int totalWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int x = this.getScreenWidth() / 2 - totalWidth / 2;
        int y = this.getButtonY();

        this.confirmButton = new ButtonGeneric(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.purchase_confirm"));
        this.addButton(this.confirmButton, (btn, mouseButton) -> this.state.confirm());

        this.backButton = new ButtonGeneric(x + BUTTON_WIDTH + BUTTON_GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.back"));
        this.addButton(this.backButton, (btn, mouseButton) -> this.state.back());

        this.syncButtons();
    }

    private int getButtonY() {
        return this.getScreenHeight() / 2 + 28;
    }

    /** 確定後(LOADING)は両方のボタンを押せなくする。 */
    private void syncButtons() {
        boolean interactive = this.state.isInteractive();

        this.confirmButton.setEnabled(interactive);
        this.backButton.setEnabled(interactive);
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        if (this.state.phase() != this.lastPhase) {
            this.lastPhase = this.state.phase();
            this.syncButtons();
        }

        super.drawContents(context, mouseX, mouseY, partialTicks);

        this.drawProduct(context);
        this.drawFocus(context);
    }

    private void drawProduct(GuiContext context) {
        ProductDetailViewModel product = this.state.product();
        int centerX = this.getScreenWidth() / 2;
        int y = this.getScreenHeight() / 2 - 48;

        this.drawCentered(context, StringUtils.translate("minetenantclient.gui.purchase_confirm.question"), centerX, y,
                COLOR_INFO);
        y += LINE_HEIGHT + 8;

        // 長い商品名は2行まで
        for (String line : TextLayout.wrap(product.name(), MAX_TEXT_WIDTH, 2, this::getStringWidth)) {
            this.drawCentered(context, line, centerX, y, COLOR_INFO);
            y += LINE_HEIGHT;
        }

        this.drawCentered(context, product.priceText(), centerX, y + 2, COLOR_PRICE);
        y += LINE_HEIGHT + 6;

        String note = this.state.phase() == Phase.LOADING
                ? StringUtils.translate("minetenantclient.gui.purchase_confirm.loading")
                : StringUtils.translate("minetenantclient.gui.purchase_confirm.unique_note");
        this.drawCentered(context, note, centerX, y, COLOR_HINT);
    }

    /** focus中のボタンを枠で示す。確定後は枠を出さない。 */
    private void drawFocus(GuiContext context) {
        if (!this.state.isInteractive()) {
            return;
        }

        ButtonGeneric focused = this.state.focus() == Button.CONFIRM ? this.confirmButton : this.backButton;
        RenderUtils.drawOutline(context, focused.getX() - 2, focused.getY() - 2, focused.getWidth() + 4,
                focused.getHeight() + 4, COLOR_FOCUS);
    }

    private void drawCentered(GuiContext context, String text, int centerX, int y, int color) {
        this.drawStringWithShadow(context, text, centerX - this.getStringWidth(text) / 2, y, color);
    }

    @Override
    public boolean onKeyTyped(KeyInput input) {
        int key = input.key();

        // 確定後は画面を閉じさせない
        if (!this.state.isInteractive()) {
            return key == GLFW.GLFW_KEY_ESCAPE || super.onKeyTyped(input);
        }

        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_TAB) {
            this.state.moveFocus();
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.state.activateFocused();
            return true;
        }

        return super.onKeyTyped(input);
    }
}
