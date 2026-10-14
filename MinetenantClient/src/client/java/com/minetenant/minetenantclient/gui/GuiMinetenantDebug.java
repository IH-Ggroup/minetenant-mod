package com.minetenant.minetenantclient.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.debug.DebugLog;
import com.minetenant.minetenantclient.network.ApiClient;
import com.minetenant.minetenantclient.network.PingClient;

/**
 * デバッグ画面。疎通確認ボタンと、そのログを表示する。
 * ログは DebugLog から読むだけなので、描画部分(drawLog)だけ
 * WidgetListBase に差し替えることもできる。
 */
public class GuiMinetenantDebug extends BaseScreen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int LOG_TOP = 70;

    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final int COLOR_SUCCESS = 0xFF55FF55;
    private static final int COLOR_ERROR = 0xFFFF5555;

    public GuiMinetenantDebug(Screen parent) {
        this.setTitle(StringUtils.translate("minetenantclient.gui.title.debug"));
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearElements();

        ButtonGeneric pingButton = new ButtonGeneric(MARGIN, 30, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.ping"));
        this.addButton(pingButton, (btn, mouseButton) -> PingClient.sendPing());

        ButtonGeneric apiButton = new ButtonGeneric(MARGIN + BUTTON_WIDTH + 4, 30, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.api_ping"));
        this.addButton(apiButton, (btn, mouseButton) -> ApiClient.sendRequest());

        ButtonGeneric clearButton = new ButtonGeneric(MARGIN + (BUTTON_WIDTH + 4) * 2, 30, BUTTON_WIDTH, BUTTON_HEIGHT,
                StringUtils.translate("minetenantclient.gui.button.clear_log"));
        this.addButton(clearButton, (btn, mouseButton) -> DebugLog.clear());
    }

    @Override
    protected void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawContents(context, mouseX, mouseY, partialTicks);

        this.drawLog(context);
    }

    /** 画面に収まる分だけ、新しいログを下に並べて表示する。長い行(APIのJSONなど)は折り返す。 */
    private void drawLog(GuiContext context) {
        int maxWidth = this.getScreenWidth() - MARGIN * 2;
        int maxLines = Math.max(1, (this.getScreenHeight() - LOG_TOP - MARGIN) / LINE_HEIGHT);

        List<DebugLog.Entry> entries = DebugLog.getAll();
        List<Map.Entry<String, DebugLog.Level>> lines = new ArrayList<>();

        for (DebugLog.Entry entry : entries) {
            for (String line : this.wrap(entry.format(), maxWidth)) {
                lines.add(Map.entry(line, entry.level()));
            }
        }

        int start = Math.max(0, lines.size() - maxLines);
        int y = LOG_TOP;

        for (int i = start; i < lines.size(); i++) {
            Map.Entry<String, DebugLog.Level> line = lines.get(i);
            this.drawStringWithShadow(context, line.getKey(), MARGIN, y, this.getColor(line.getValue()));
            y += LINE_HEIGHT;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (current.length() > 0 && this.getStringWidth(current.toString() + c) > maxWidth) {
                result.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }

        result.add(current.toString());
        return result;
    }

    private int getColor(DebugLog.Level level) {
        return switch (level) {
            case SUCCESS -> COLOR_SUCCESS;
            case ERROR -> COLOR_ERROR;
            default -> COLOR_INFO;
        };
    }
}
