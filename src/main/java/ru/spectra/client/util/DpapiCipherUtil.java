package ru.spectra.client.util;

import com.sun.jna.platform.win32.Crypt32Util;
import com.sun.jna.platform.win32.WinCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/** Windows user-scoped DPAPI storage for non-Spectra game credentials. */
public final class DpapiCipherUtil {
    private static final byte[] CONTEXT =
            "spectra:auto-auth:v2".getBytes(StandardCharsets.UTF_8);

    private DpapiCipherUtil() {
    }

    public static String protect(String plaintext, String binding) {
        requireWindows();
        byte[] value = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] entropy = entropy(binding);
        byte[] protectedValue = null;
        try {
            protectedValue = Crypt32Util.cryptProtectData(
                    value,
                    entropy,
                    WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                    "Spectra auto-auth credential",
                    null
            );
            return Base64.getEncoder().encodeToString(protectedValue);
        } finally {
            Arrays.fill(value, (byte) 0);
            Arrays.fill(entropy, (byte) 0);
            if (protectedValue != null) {
                Arrays.fill(protectedValue, (byte) 0);
            }
        }
    }

    public static String unprotect(String encoded, String binding) {
        requireWindows();
        byte[] protectedValue = Base64.getDecoder().decode(encoded);
        byte[] entropy = entropy(binding);
        byte[] plaintext = null;
        try {
            plaintext = Crypt32Util.cryptUnprotectData(
                    protectedValue,
                    entropy,
                    WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                    null
            );
            return new String(plaintext, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(protectedValue, (byte) 0);
            Arrays.fill(entropy, (byte) 0);
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private static byte[] entropy(String binding) {
        byte[] scoped = binding.getBytes(StandardCharsets.UTF_8);
        byte[] result = Arrays.copyOf(CONTEXT, CONTEXT.length + scoped.length);
        System.arraycopy(scoped, 0, result, CONTEXT.length, scoped.length);
        Arrays.fill(scoped, (byte) 0);
        return result;
    }

    private static void requireWindows() {
        if (!System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("windows")) {
            throw new IllegalStateException(
                    "Spectra credential storage requires Windows DPAPI"
            );
        }
    }
}
