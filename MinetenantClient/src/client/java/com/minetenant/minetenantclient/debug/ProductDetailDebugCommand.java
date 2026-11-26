package com.minetenant.minetenantclient.debug;

import java.util.concurrent.atomic.AtomicLong;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

import com.minetenant.minetenantclient.catalog.ProductDetail;
import com.minetenant.minetenantclient.network.ProductDetailRequest;
import com.minetenant.minetenantclient.network.ProductDetailResponse;

/**
 * 商品詳細packetの実機確認用コマンド。 /minetenantdetail &lt;productId&gt; でServerへ要求し、結果をチャットに出す。
 * 商品詳細GUIができたら削除する。
 * 不正な文字のIDの確認ができるよう、文字の種類はClient側では検証せずそのまま送る。長さだけはpacketの上限で止める。
 */
public final class ProductDetailDebugCommand {

    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1);

    private ProductDetailDebugCommand() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ProductDetailResponse.ID, (payload, context) -> {
            if (context.client().player != null) {
                context.client().player.sendMessage(Text.literal(format(payload)), false);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("minetenantdetail")
                        .then(ClientCommandManager.argument("productId", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String productId = StringArgumentType.getString(context, "productId");
                                    // packetは上限を超える文字列を符号化できず、送ると切断されるため送らない
                                    if (productId.length() > ProductDetailRequest.MAX_PRODUCT_ID_LENGTH) {
                                        context.getSource().sendFeedback(Text.literal("[detail] productId is longer than "
                                                + ProductDetailRequest.MAX_PRODUCT_ID_LENGTH + " characters, not sent"));
                                        return 0;
                                    }
                                    if (!ClientPlayNetworking.canSend(ProductDetailRequest.ID)) {
                                        context.getSource().sendFeedback(
                                                Text.literal("[detail] Server does not accept product detail requests"));
                                        return 0;
                                    }

                                    long requestId = NEXT_REQUEST_ID.getAndIncrement();
                                    ClientPlayNetworking.send(new ProductDetailRequest(requestId, productId));
                                    context.getSource().sendFeedback(
                                            Text.literal("[detail] sent #" + requestId + " productId=" + productId));
                                    return 1;
                                }))));
    }

    private static String format(ProductDetailResponse response) {
        String head = "[detail] #" + response.requestId() + " " + response.code();
        if (response.detail().isEmpty()) {
            return head;
        }

        ProductDetail d = response.detail().get();
        return head + " id=" + d.id() + " store=" + d.storeId() + " name=" + d.name() + " price=" + d.price()
                + " status=" + d.status() + " category=" + d.category() + " theme=" + d.theme() + " emoji=" + d.emoji()
                + " description=" + d.description();
    }
}
