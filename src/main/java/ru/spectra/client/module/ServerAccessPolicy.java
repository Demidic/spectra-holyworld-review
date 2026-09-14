package ru.spectra.client.module;

import ru.spectra.client.util.ClientLocalization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Fail-closed server policy for modules whose use is allowed only on selected
 * networks, or forbidden on selected networks.
 */
public final class ServerAccessPolicy {
    private static final ServerAccessPolicy ALLOW_ALL =
            new ServerAccessPolicy(true, true, true, List.of());

    private final boolean unrestricted;
    private final boolean allowOutsideMultiplayer;
    private final boolean defaultMultiplayerAccess;
    private final List<Rule> rules;

    private ServerAccessPolicy(boolean unrestricted, boolean allowOutsideMultiplayer,
                               boolean defaultMultiplayerAccess, List<Rule> rules) {
        this.unrestricted = unrestricted;
        this.allowOutsideMultiplayer = allowOutsideMultiplayer;
        this.defaultMultiplayerAccess = defaultMultiplayerAccess;
        this.rules = rules;
    }

    public static ServerAccessPolicy allowAll() {
        return ALLOW_ALL;
    }

    public static ServerAccessPolicy onlyOnServers(String... addressKeywords) {
        Builder builder = builder(false).allowOutsideMultiplayer(true);
        for (String keyword : addressKeywords) {
            builder.rule(keyword, true);
        }
        return builder.build();
    }

    public static ServerAccessPolicy denyOnServers(String... addressKeywords) {
        Builder builder = builder(true).allowOutsideMultiplayer(true);
        for (String keyword : addressKeywords) {
            builder.rule(keyword, false);
        }
        return builder.build();
    }

    public static Builder builder(boolean defaultMultiplayerAccess) {
        return new Builder(defaultMultiplayerAccess);
    }

    public boolean allowsCurrentContext() {
        if (this.unrestricted) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.isInSingleplayer()) {
            return this.allowOutsideMultiplayer;
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null || server.address == null || server.address.isBlank()) {
            // A restricted module must never become usable merely because the
            // multiplayer address could not be identified.
            return false;
        }
        String normalizedAddress = normalize(server.address);
        for (Rule rule : this.rules) {
            if (normalizedAddress.contains(rule.normalizedKeyword())) {
                return rule.allowed();
            }
        }
        return this.defaultMultiplayerAccess;
    }

    public boolean isRestricted() {
        return !this.unrestricted;
    }

    public AvailabilityNotice availabilityNotice() {
        if (this.unrestricted) {
            return AvailabilityNotice.NONE;
        }
        List<String> serverNames = this.rules.stream()
                .filter(rule -> rule.allowed() != this.defaultMultiplayerAccess)
                .map(Rule::displayKeyword)
                .toList();
        if (serverNames.isEmpty()) {
            return new AvailabilityNotice(
                    this.defaultMultiplayerAccess
                            ? ClientLocalization.text(
                            "Available on multiplayer servers",
                            "\u0414\u043E\u0441\u0442\u0443\u043F\u043D\u043E \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0430\u0445")
                            : ClientLocalization.text(
                            "Unavailable on multiplayer servers",
                            "\u041D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0430\u0445"),
                    List.of()
            );
        }
        return new AvailabilityNotice(
                this.defaultMultiplayerAccess
                        ? ClientLocalization.text(
                        "Unavailable on servers:",
                        "\u041D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0430\u0445:")
                        : ClientLocalization.text(
                        "Supported only on servers:",
                        "\u041F\u043E\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044F \u0442\u043E\u043B\u044C\u043A\u043E \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0430\u0445:"),
                serverNames
        );
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static final class Builder {
        private final boolean defaultMultiplayerAccess;
        private boolean allowOutsideMultiplayer = true;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(boolean defaultMultiplayerAccess) {
            this.defaultMultiplayerAccess = defaultMultiplayerAccess;
        }

        public Builder allowOutsideMultiplayer(boolean allowed) {
            this.allowOutsideMultiplayer = allowed;
            return this;
        }

        public Builder rule(String addressKeyword, boolean allowed) {
            String normalized = normalize(Objects.requireNonNull(addressKeyword, "addressKeyword"));
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Server address keyword cannot be empty");
            }
            if (this.rules.stream().anyMatch(rule -> rule.normalizedKeyword().equals(normalized))) {
                throw new IllegalArgumentException("Duplicate server address keyword: " + addressKeyword);
            }
            this.rules.add(new Rule(addressKeyword.strip(), normalized, allowed));
            return this;
        }

        public ServerAccessPolicy build() {
            // More specific keywords win over shorter overlapping keywords.
            this.rules.sort(Comparator.comparingInt((Rule rule) ->
                    rule.normalizedKeyword().length()).reversed());
            return new ServerAccessPolicy(
                    false,
                    this.allowOutsideMultiplayer,
                    this.defaultMultiplayerAccess,
                    List.copyOf(this.rules)
            );
        }
    }

    public record AvailabilityNotice(String title, List<String> servers) {
        private static final AvailabilityNotice NONE = new AvailabilityNotice("", List.of());

        public AvailabilityNotice {
            servers = List.copyOf(servers);
        }
    }

    private record Rule(String displayKeyword, String normalizedKeyword, boolean allowed) {
    }
}

