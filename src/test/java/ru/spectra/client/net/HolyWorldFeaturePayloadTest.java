package ru.spectra.client.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class HolyWorldFeaturePayloadTest {
    @Test
    void utf8JsonRoundTripsWithoutMinecraftWriteStringLengthPrefix() {
        String json = "{\"id\":\"test\",\"text\":\"Перезарядка завершена\"}";
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            HolyWorldFeatureControl.FeaturePayload.CODEC.encode(buffer,
                    new HolyWorldFeatureControl.FeaturePayload(json));
            assertEquals('{', buffer.getUnsignedByte(0));
            assertEquals(json.getBytes(StandardCharsets.UTF_8).length, buffer.readableBytes());
            assertEquals(json, HolyWorldFeatureControl.FeaturePayload.CODEC.decode(buffer).json());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void oversizedInboundPayloadIsRejectedBeforeAllocatingAnUnboundedArray() {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            buffer.writeZero(HolyWorldFeaturePolicy.MAX_PAYLOAD_BYTES + 1);
            assertThrows(IllegalArgumentException.class,
                    () -> HolyWorldFeatureControl.FeaturePayload.CODEC.decode(buffer));
            assertEquals(0, buffer.readerIndex());
        } finally {
            buffer.release();
        }
    }

    @Test
    void outboundLimitIsMeasuredInUtf8BytesNotJavaCharacters() {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        try {
            String json = "я".repeat(10_000);
            assertThrows(IllegalArgumentException.class,
                    () -> HolyWorldFeatureControl.FeaturePayload.CODEC.encode(buffer,
                            new HolyWorldFeatureControl.FeaturePayload(json)));
            assertEquals(0, buffer.writerIndex());
        } finally {
            buffer.release();
        }
    }
}
