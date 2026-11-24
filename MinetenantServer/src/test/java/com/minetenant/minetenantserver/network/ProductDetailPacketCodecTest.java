package com.minetenant.minetenantserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.catalog.ProductDetail;
import com.minetenant.minetenantserver.catalog.ProductDetailPacketLimits;
import com.minetenant.minetenantserver.catalog.ProductDetailResult;
import com.minetenant.minetenantserver.catalog.ProductStatus;

/** packetの符号化と復号が対称で、長い入力でも送信前に収まることを確認する。 */
class ProductDetailPacketCodecTest {

    private static RegistryByteBuf buffer() {
        return new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    }

    private static ProductDetailResponse roundTrip(ProductDetailResponse response) {
        RegistryByteBuf buf = buffer();
        ProductDetailResponse.CODEC.encode(buf, response);
        return ProductDetailResponse.CODEC.decode(buf);
    }

    @Test
    void requestRoundTrips() {
        RegistryByteBuf buf = buffer();
        ProductDetailRequest.CODEC.encode(buf, new ProductDetailRequest(42L, "product-1"));

        assertEquals(new ProductDetailRequest(42L, "product-1"), ProductDetailRequest.CODEC.decode(buf));
    }

    @Test
    void successResponseRoundTrips() {
        ProductDetail detail = new ProductDetail("p1", "store-a", "Name", "Description", 6800,
                ProductStatus.SOLD, "fashion", "ocean", "🧥");

        ProductDetailResponse decoded = roundTrip(ProductDetailResponse.of(7L, ProductDetailResult.success(detail)));

        assertEquals(7L, decoded.requestId());
        assertEquals(ProductDetailResultCode.SUCCESS, decoded.code());
        assertEquals(Optional.of(detail), decoded.detail());
    }

    @Test
    void failureResponseRoundTripsWithoutDetail() {
        ProductDetailResponse decoded = roundTrip(ProductDetailResponse.of(8L,
                ProductDetailResult.failure(ProductDetailResultCode.PRODUCT_NOT_FOUND)));

        assertEquals(ProductDetailResultCode.PRODUCT_NOT_FOUND, decoded.code());
        assertTrue(decoded.detail().isEmpty());
    }

    @Test
    void overlongTextIsFittedBeforeEncoding() {
        ProductDetail detail = new ProductDetail("p1", "store-a", "n".repeat(5000), "d".repeat(5000), 1,
                ProductStatus.AVAILABLE, "c", "t", "e");

        ProductDetailResponse decoded = roundTrip(ProductDetailResponse.of(1L, ProductDetailResult.success(detail)));

        assertEquals(ProductDetailPacketLimits.MAX_DESCRIPTION_LENGTH, decoded.detail().orElseThrow().description().length());
    }

    @Test
    void unsendableDetailBecomesUnknownFailure() {
        ProductDetail detail = new ProductDetail("a".repeat(ProductDetailPacketLimits.MAX_ID_LENGTH + 1),
                "store-a", "n", "d", 1, ProductStatus.AVAILABLE, "c", "t", "e");

        ProductDetailResponse response = ProductDetailResponse.of(1L, ProductDetailResult.success(detail));

        assertEquals(ProductDetailResultCode.UNKNOWN, response.code());
        assertTrue(response.detail().isEmpty());
    }

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
    void encodedBytesMatchTheWireFormat() {
        RegistryByteBuf actual = buffer();
        ProductDetailResponse.CODEC.encode(actual, new ProductDetailResponse(7L, ProductDetailResultCode.SUCCESS,
                Optional.of(new ProductDetail("p1", "store-a", "Name", "Description", 6800,
                        ProductStatus.SOLD, "fashion", "ocean", "e"))));

        RegistryByteBuf expected = wire();
        byte[] actualBytes = new byte[actual.readableBytes()];
        byte[] expectedBytes = new byte[expected.readableBytes()];
        actual.readBytes(actualBytes);
        expected.readBytes(expectedBytes);

        org.junit.jupiter.api.Assertions.assertArrayEquals(expectedBytes, actualBytes);
    }
}
