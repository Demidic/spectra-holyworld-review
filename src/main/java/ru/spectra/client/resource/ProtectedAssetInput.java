package ru.spectra.client.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class ProtectedAssetInput {
    private static final String VAULT_CLASS =
            "net.fabricmc.loader.impl.launch.knot.SpectraClassVault";

    private ProtectedAssetInput() {
    }

    public static InputStream open(String path) {
        byte[] value = read(path);
        return value == null ? null : new WipingInputStream(value);
    }

    private static byte[] read(String path) {
        try {
            Class<?> vault = Class.forName(
                    VAULT_CLASS,
                    true,
                    ProtectedAssetInput.class.getClassLoader()
            );
            Method findAsset = vault.getMethod("findAsset", String.class);
            Object value = findAsset.invoke(null, path);
            return value instanceof byte[] bytes ? bytes : null;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Development runs use ordinary classpath resources.
            return null;
        } catch (IllegalAccessException exception) {
            throw new SecurityException(
                    "Cannot access the Spectra asset vault",
                    exception
            );
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw cause instanceof RuntimeException runtime
                    ? runtime
                    : new SecurityException(
                    "Cannot decrypt a Spectra asset",
                    cause
            );
        }
    }

    private static final class WipingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private WipingInputStream(byte[] value) {
            super(value);
        }

        @Override
        public synchronized int read() {
            int value = super.read();
            wipeAtEnd(value < 0 || pos >= count);
            return value;
        }

        @Override
        public synchronized int read(byte[] target, int offset, int length) {
            int read = super.read(target, offset, length);
            wipeAtEnd(read < 0 || pos >= count);
            return read;
        }

        private void wipeAtEnd(boolean endReached) {
            if (endReached && !closed) {
                closed = true;
                Arrays.fill(buf, (byte) 0);
            }
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                Arrays.fill(buf, (byte) 0);
            }
        }
    }
}
