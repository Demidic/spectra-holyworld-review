package ru.spectra.client.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Connection-scoped policy. Only a matching, completely valid response opens access. */
public final class HolyWorldFeaturePolicy {
    public static final int MAX_PAYLOAD_BYTES = 16_384;
    private Object connection;
    private String requestId;
    private Set<String> submitted = Set.of();
    private Set<String> blocked = Set.of();
    private boolean verified;

    public static String featureId(String moduleName) {
        String id = moduleName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (id.isEmpty() || id.length() > 80) {
            throw new IllegalArgumentException("Invalid feature name");
        }
        return id;
    }

    public static boolean isHolyWorldHost(String address) {
        if (address == null) {
            return false;
        }
        String host = address.strip().toLowerCase(Locale.ROOT);
        int colon = host.lastIndexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host.equals("holyworld.me") || host.endsWith(".holyworld.me")
                || host.equals("holyworld.ru") || host.endsWith(".holyworld.ru");
    }

    public String begin(Object connection, List<String> features) {
        if (connection == null || features.isEmpty() || features.size() > 256
                || new HashSet<>(features).size() != features.size()
                || features.stream().anyMatch(id -> !id.matches("[a-z0-9_]{1,80}"))) {
            throw new IllegalArgumentException("Invalid feature request");
        }
        this.connection = connection;
        this.submitted = Set.copyOf(features);
        this.blocked = Set.of();
        this.verified = false;
        this.requestId = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject();
        payload.addProperty("client", "spectra");
        com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
        features.forEach(ids::add);
        payload.add("features", ids);
        JsonObject request = new JsonObject();
        request.addProperty("id", this.requestId);
        request.addProperty("method", "checkFeatures");
        request.add("payload", payload);
        return request.toString();
    }

    /** Returns true only when this response belongs to the outstanding request. */
    public boolean receive(Object connection, String json) {
        if (connection != this.connection || this.requestId == null || json == null
                || json.length() > MAX_PAYLOAD_BYTES) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonElement id = root.get("id");
            if (!isString(id) || !this.requestId.equals(id.getAsString())) {
                return false;
            }
            this.verified = false;
            JsonElement ok = root.get("ok");
            if (ok == null || !ok.isJsonPrimitive()
                    || !ok.getAsJsonPrimitive().isBoolean() || !ok.getAsBoolean()) {
                this.requestId = null;
                return true;
            }
            JsonElement payload = root.get("payload");
            if (payload == null || !payload.isJsonObject()) {
                this.requestId = null;
                return true;
            }
            JsonElement blocklist = payload.getAsJsonObject().get("blocklist");
            if (blocklist == null || !blocklist.isJsonArray()
                    || blocklist.getAsJsonArray().size() > this.submitted.size()) {
                this.requestId = null;
                return true;
            }
            Set<String> result = new HashSet<>();
            for (JsonElement feature : blocklist.getAsJsonArray()) {
                if (!isString(feature) || !this.submitted.contains(feature.getAsString())
                        || !result.add(feature.getAsString())) {
                    this.requestId = null;
                    return true;
                }
            }
            this.blocked = Set.copyOf(result);
            this.verified = true;
            this.requestId = null;
            return true;
        } catch (RuntimeException invalid) {
            // Malformed or unrelated messages never make any feature available.
            return false;
        }
    }

    private static boolean isString(JsonElement element) {
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString();
    }

    public boolean allows(String feature) {
        return this.verified && this.submitted.contains(feature) && !this.blocked.contains(feature);
    }

    public boolean isVerified() {
        return this.verified;
    }

    public void fail(Object connection) {
        if (connection == this.connection) {
            this.verified = false;
            this.requestId = null;
        }
    }

    public void disconnect() {
        this.connection = null;
        this.requestId = null;
        this.submitted = Set.of();
        this.blocked = Set.of();
        this.verified = false;
    }
}
