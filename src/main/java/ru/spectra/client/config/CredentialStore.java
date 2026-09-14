package ru.spectra.client.config;

import ru.spectra.client.model.CredentialKey;
import ru.spectra.client.util.AtomicFileWriter;
import ru.spectra.client.util.DpapiCipherUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict, versioned DPAPI storage for optional Minecraft server passwords. */
public final class CredentialStore {
    private static final String FORMAT = "spectra-auto-auth-v2";
    private static final int MAX_CREDENTIALS = 128;
    private static final long MAX_FILE_BYTES = 1024L * 1024L;
    private static final int MAX_FIELD_CHARS = 512;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private final Path path;
    private final Protector protector;

    public CredentialStore(Path path) {
        this(path, new DpapiProtector());
    }

    CredentialStore(Path path, Protector protector) {
        this.path = path.toAbsolutePath().normalize();
        this.protector = java.util.Objects.requireNonNull(protector);
    }

    public Path path() {
        return path;
    }

    public Map<CredentialKey, String> loadCredentials() throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return Map.of();
        }
        require(
                Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(path)
                        && Files.size(path) > 0
                        && Files.size(path) <= MAX_FILE_BYTES,
                "Auto-auth credential file is unsafe"
        );
        CredentialDocument document;
        try {
            document = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8),
                    CredentialDocument.class
            );
        } catch (RuntimeException exception) {
            throw new IOException("Invalid auto-auth credential file", exception);
        }
        require(document != null
                        && FORMAT.equals(document.format)
                        && document.credentials != null
                        && document.credentials.size() <= MAX_CREDENTIALS,
                "Unsupported auto-auth credential format");
        Map<CredentialKey, String> result = new LinkedHashMap<>();
        for (StoredCredential stored : document.credentials) {
            require(stored != null
                            && validField(stored.serverAddress)
                            && validField(stored.username)
                            && validProtectedValue(stored.protectedPassword),
                    "Invalid auto-auth credential entry");
            String binding = binding(stored.serverAddress, stored.username);
            String password;
            try {
                password = protector.unprotect(
                        stored.protectedPassword,
                        binding
                );
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Auto-auth credential cannot be unprotected",
                        exception
                );
            }
            require(password.length() <= MAX_FIELD_CHARS,
                    "Auto-auth password is oversized");
            CredentialKey key = new CredentialKey(
                    stored.serverAddress,
                    stored.username
            );
            require(result.putIfAbsent(key, password) == null,
                    "Duplicate auto-auth credential entry");
        }
        return Map.copyOf(result);
    }

    public void saveCredentials(Map<CredentialKey, String> credentials)
            throws IOException {
        require(credentials != null
                        && credentials.size() <= MAX_CREDENTIALS,
                "Too many auto-auth credentials");
        List<StoredCredential> stored = new ArrayList<>(credentials.size());
        for (Map.Entry<CredentialKey, String> entry : credentials.entrySet()) {
            CredentialKey key = entry.getKey();
            String password = entry.getValue();
            require(key != null
                            && validField(key.serverAddress())
                            && validField(key.username())
                            && password != null
                            && password.length() <= MAX_FIELD_CHARS,
                    "Invalid auto-auth credential");
            String binding = binding(key.serverAddress(), key.username());
            String protectedPassword;
            try {
                protectedPassword = protector.protect(password, binding);
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Auto-auth credential cannot be protected",
                        exception
                );
            }
            require(validProtectedValue(protectedPassword),
                    "Invalid protected auto-auth credential");
            stored.add(new StoredCredential(
                    key.serverAddress(),
                    key.username(),
                    protectedPassword
            ));
        }
        CredentialDocument document = new CredentialDocument();
        document.format = FORMAT;
        document.credentials = stored;
        byte[] encoded = GSON.toJson(document)
                .getBytes(StandardCharsets.UTF_8);
        try {
            require(encoded.length <= MAX_FILE_BYTES,
                    "Auto-auth credential document is oversized");
            AtomicFileWriter.writeBytes(
                    path,
                    encoded,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
            );
        } finally {
            java.util.Arrays.fill(encoded, (byte) 0);
        }
    }

    private static String binding(String server, String username) {
        return server + "\0" + username;
    }

    private static boolean validField(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= MAX_FIELD_CHARS
                && value.indexOf('\0') < 0
                && value.indexOf('\n') < 0
                && value.indexOf('\r') < 0;
    }

    private static boolean validProtectedValue(String value) {
        return value != null
                && value.length() >= 16
                && value.length() <= 16 * 1024
                && value.matches("[A-Za-z0-9+/]+={0,2}");
    }

    private static void require(boolean condition, String message)
            throws IOException {
        if (!condition) {
            throw new IOException(message);
        }
    }

    interface Protector {
        String protect(String plaintext, String binding);

        String unprotect(String protectedValue, String binding);
    }

    private static final class DpapiProtector implements Protector {
        @Override
        public String protect(String plaintext, String binding) {
            return DpapiCipherUtil.protect(plaintext, binding);
        }

        @Override
        public String unprotect(String protectedValue, String binding) {
            return DpapiCipherUtil.unprotect(protectedValue, binding);
        }
    }

    private static final class CredentialDocument {
        @SerializedName("format")
        String format;

        @SerializedName("credentials")
        List<StoredCredential> credentials;
    }
}
