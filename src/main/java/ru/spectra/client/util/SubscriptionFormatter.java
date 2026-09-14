package ru.spectra.client.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SubscriptionFormatter {
    private static final DateTimeFormatter SHORT_DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yy/HH:mm", Locale.ROOT);

    private SubscriptionFormatter() {
    }

    public static String display(String rawExpiry) {
        return format(rawExpiry, ClientLocalization.isRussian(), ZoneId.systemDefault());
    }

    static String format(String rawExpiry, boolean russian, ZoneId zone) {
        if (rawExpiry == null || rawExpiry.isBlank()) {
            return russian ? "Срок не указан" : "Expiry unavailable";
        }
        if ("development".equalsIgnoreCase(rawExpiry)) {
            return russian ? "Разработка" : "Development";
        }
        if (rawExpiry.startsWith("9999-")) {
            return russian ? "Бессрочно" : "Lifetime";
        }
        try {
            String formatted = SHORT_DATE_TIME.format(
                    Instant.parse(rawExpiry).atZone(zone)
            );
            return (russian ? "До " : "Until ") + formatted;
        } catch (RuntimeException ignored) {
            return russian ? "Срок не указан" : "Expiry unavailable";
        }
    }
}
