package com.minetenant.minetenantclient.gui;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.link.DefaultLinkCodeValidator;
import com.minetenant.minetenantclient.link.FixtureLinkSubmitter;
import com.minetenant.minetenantclient.link.LinkCodeValidator;
import com.minetenant.minetenantclient.link.LinkSubmitter;
import com.minetenant.minetenantclient.link.LinkUiState;
import com.minetenant.minetenantclient.link.LinkUiState.Phase;
import com.minetenant.minetenantclient.link.TickScheduler;

/**
 * 連携コード入力画面。入力欄・送信・戻るのボタンと、状態の表示だけを担当する。
 * 状態遷移と検証は LinkUiState に任せる。
 * 操作は他のMaLiLib画面に合わせ、送信・戻るともボタンで行う(Escでも戻れる)。
 * 入力したコードはこの画面の入力欄にだけ置き、成功時と画面を閉じたときに消す。
 */
public class LinkAccountGui extends BaseScreen {

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int LINE_HEIGHT = 12;

    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFAAAAAA;
    private static final int COLOR_SUCCESS = 0xFF55FF55;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private final LinkUiState state;

    private GuiTextFieldGeneric codeField;
    private ButtonGeneric submitButton;
    /** 画面サイズ変更で再構築されても入力が消えないよう、直前の入力を持ち越す */
    private String carriedText = "";
    private Phase lastPhase = Phase.IDLE;

    public LinkAccountGui(Screen parent) {
        this(parent, new DefaultLinkCodeValidator(), new FixtureLinkSubmitter(TickScheduler.INSTANCE));
    }

    /** 検証と送信先を差し替えられる。本番では通信する送信先を渡す。 */
    public LinkAccountGui(Screen parent, LinkCodeValidator validator, LinkSubmitter submitter) {
        this.state = new LinkUiState(validator, submitter);
        this.setTitle(StringUtils.translate("minetenantclient.gui.title.link"));
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();

        if (this.codeField != null) {
            this.carriedText = this.codeField.getTextWrapper();
        }

        this.clearElements();

        int x = this.getScreenWidth() / 2 - FIELD_WIDTH / 2;
        int y = this.getScreenHeight() / 2 - 30;

        this.codeField = new GuiTextFieldGeneric(x, y, FIELD_WIDTH, FIELD_HEIGHT, this.font);
        this.codeField.setMaxLengthWrapper(DefaultLinkCodeValidator.MAX_LENGTH * 2);
        this.codeField.setTextWrapper(this.carriedText);

        TextFieldWrapper<GuiTextFieldGeneric> wrapper = this.addTextField(this.codeField, field -> false);
        wrapper.setFocused(true);

        int buttonWidth = (FIELD_WIDTH - BUTTON_GAP) / 2;
        int buttonY = y + FIELD_HEIGHT + BUTTON_GAP * 2;

        this.submitButton = new ButtonGeneric(x, buttonY, buttonWidth, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.link_submit"));
        this.addButton(this.submitButton, (btn, mouseButton) -> this.submit());

        ButtonGeneric backButton = new ButtonGeneric(x + buttonWidth + BUTTON_GAP, buttonY, buttonWidth, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.back"));
        this.addButton(backButton, (btn, mouseButton) -> this.closeGui(true));
    }

    /** 送信ボタンから呼ぶ。送信中の二重送信は LinkUiState が無視する。 */
    private void submit() {
        this.state.submit(this.codeField.getTextWrapper());
    }

    @Override
    protected void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        this.syncWithState();

        super.drawContents(context, mouseX, mouseY, partialTicks);

        int x = this.getScreenWidth() / 2 - FIELD_WIDTH / 2;
        int y = this.getScreenHeight() / 2 - 30;

        this.drawStringWithShadow(context, StringUtils.translate("minetenantclient.gui.link.label"), x, y - LINE_HEIGHT - 2, COLOR_INFO);
        this.drawStatus(context, y + FIELD_HEIGHT + BUTTON_GAP * 3 + BUTTON_HEIGHT);
    }

    /** 状態に合わせて操作可否を切り替え、成功したら入力を消す。 */
    private void syncWithState() {
        Phase phase = this.state.phase();
        boolean loading = phase == Phase.LOADING;

        this.submitButton.setEnabled(!loading);
        this.codeField.setEditable(!loading);

        if (phase == Phase.SUCCESS && this.lastPhase != Phase.SUCCESS) {
            this.codeField.setTextWrapper("");
            this.carriedText = "";
        }

        this.lastPhase = phase;
    }

    private void drawStatus(GuiContext context, int y) {
        String text;
        int color;

        switch (this.state.phase()) {
            case LOADING -> {
                text = StringUtils.translate("minetenantclient.gui.link.status.loading");
                color = COLOR_INFO;
            }
            case SUCCESS -> {
                text = StringUtils.translate("minetenantclient.gui.link.status.success");
                color = COLOR_SUCCESS;
            }
            case ERROR -> {
                text = StringUtils.translate(this.state.error().translationKey()) + " (" + this.state.error().code() + ")";
                color = COLOR_ERROR;
            }
            default -> {
                text = StringUtils.translate("minetenantclient.gui.link.status.idle");
                color = COLOR_HINT;
            }
        }

        int x = this.getScreenWidth() / 2 - this.getStringWidth(text) / 2;
        this.drawStringWithShadow(context, text, x, y, color);
    }

    /** 画面を閉じたら、入力したコードと送信中の結果を捨てる。 */
    @Override
    public void removed() {
        this.state.reset();
        this.carriedText = "";

        if (this.codeField != null) {
            this.codeField.setTextWrapper("");
        }

        super.removed();
    }
}
