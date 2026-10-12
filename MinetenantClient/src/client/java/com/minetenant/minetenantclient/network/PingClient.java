package com.minetenant.minetenantclient.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.util.StringUtils;

import com.minetenant.minetenantclient.debug.DebugLog;
import com.minetenant.minetenantclient.debug.DebugLog.Level;

/**
 * サーバーMODとの疎通確認(Ping送信 / Pong受信 / タイムアウト判定)を担当する。
 */
public final class PingClient {

    private static final int TIMEOUT_TICKS = 60;

    private static boolean waiting;
    private static int remainingTicks;

    private PingClient() {
    }

    public static void sendPing() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.getNetworkHandler() == null) {
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.not_connected"));
            return;
        }

        if (!ClientPlayNetworking.canSend(PingPayload.ID)) {
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.server_mod_not_found"));
            return;
        }

        ClientPlayNetworking.send(new PingPayload(System.nanoTime()));
        waiting = true;
        remainingTicks = TIMEOUT_TICKS;
        DebugLog.add(Level.INFO, StringUtils.translate("minetenantclient.debug.log.ping_sent"));
    }

    public static void onPong(PongPayload payload) {
        if (!waiting) {
            return;
        }

        waiting = false;
        long millis = (System.nanoTime() - payload.sentAt()) / 1_000_000L;
        DebugLog.add(Level.SUCCESS, StringUtils.translate("minetenantclient.debug.log.success", millis));
    }

    /** クライアントのtickごとに呼ぶ。応答が来ないまま時間が過ぎたら失敗扱いにする。 */
    public static void onClientTick() {
        if (!waiting) {
            return;
        }

        if (--remainingTicks <= 0) {
            waiting = false;
            DebugLog.add(Level.ERROR, StringUtils.translate("minetenantclient.debug.log.timeout"));
        }
    }

    /** 切断時などに待機状態を捨てる。 */
    public static void reset() {
        waiting = false;
    }
}
