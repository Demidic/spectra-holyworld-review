package ru.spectra.client.resource;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public interface ResourceSource {
    InputStream stream();

    default void writeToByteBuffer(ByteBuffer byteBuffer) {
        byte[] value = bytes();
        try {
            byteBuffer.put(value);
        } finally {
            Arrays.fill(value, (byte) 0);
        }
    }

    default ByteBuffer asDirectByteBuffer() {
        byte[] value = bytes();
        try {
            ByteBuffer direct = ByteBuffer.allocateDirect(value.length);
            direct.put(value);
            return direct.flip();
        } finally {
            Arrays.fill(value, (byte) 0);
        }
    }

    default byte[] bytes() {
        try (InputStream inputStreamStream = stream()) {
            return inputStreamStream.readAllBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    default String utf8() {
        byte[] value = bytes();
        try {
            return new String(value, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(value, (byte) 0);
        }
    }
}
