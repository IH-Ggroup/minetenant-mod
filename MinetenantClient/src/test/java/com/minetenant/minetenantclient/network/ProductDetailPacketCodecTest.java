package com.minetenant.minetenantclient.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantclient.catalog.ProductDetail;
import com.minetenant.minetenantclient.catalog.ProductStatus;

/** Serverが送る通信上の並びを、Clientが読めることを確認する。 */
class ProductDetailPacketCodecTest {

    /** 通信上の並びを手で組み立てる。Server/Clientの両方のテストで同じ並びを使う。 */
    private static RegistryByteBuf wire() {
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
        buf.writeVarLong(7L);
        buf.writeVarInt(0);
        buf.writeBoolean(true);
        buf.writeString("p1");
        buf.writeString("store-a");
        buf.writeString("Name");
        buf.writeString("Description");
        buf.writeVarLong(6800L);
        buf.writeVarInt(1);
        buf.writeString("fashion");
        buf.writeString("ocean");
        buf.writeString("e");
        return buf;
    }

    @Test
    void decodesTheWireFormatSentByServer() {
        ProductDetailResponse decoded = ProductDetailResponse.CODEC.decode(wire());

        assertEquals(new ProductDetailResponse(7L, ProductDetailResultCode.SUCCESS,
                Optional.of(new ProductDetail("p1", "store-a", "Name", "Description", 6800,
                        ProductStatus.SOLD, "fashion", "ocean", "e"))), decoded);
    }

    @Test
    void requestRoundTrips() {
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
        ProductDetailRequest.CODEC.encode(buf, new ProductDetailRequest(42L, "product-1"));

        assertEquals(new ProductDetailRequest(42L, "product-1"), ProductDetailRequest.CODEC.decode(buf));
    }
}
