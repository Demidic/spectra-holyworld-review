package ru.spectra.client.util;

import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.type.Mc;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PvPModeDetector {
    public static final Stopwatch pvpStopwatch = new Stopwatch();
    private static final Pattern PVP_PATTERN = Pattern.compile(
            "(pvp|pvp-\\s*режим|пвп|режим\\s*(pvp|пвп|боя)|(^|[^a-z0-9а-яё])бой($|[^a-z0-9а-яё]))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern SPOOKYTIME_DUEL_PATTERN = Pattern.compile(
            "вы\\s+(сейчас\\s+)?играете\\s+на\\s+сервере\\s+.*spo+kytime",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static boolean pvpRecentlySeen;

    private PvPModeDetector() {
    }

    public static boolean isPvPMode() {
        if (hasPvpBossBar()) {
            pvpRecentlySeen = true;
            pvpStopwatch.reset();
            return true;
        }
        if (!pvpRecentlySeen) {
            return false;
        }
        if (pvpStopwatch.hasElapsed(500L)) {
            pvpRecentlySeen = false;
            return false;
        }
        return true;
    }

    public static void tick() {
        if (hasPvpBossBar()) {
            pvpRecentlySeen = true;
            pvpStopwatch.reset();
        }
    }

    public static boolean hasPvpBossBar() {
        if (!Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getInGameHud() == null
                || Mc.INSTANCE.getInGameHud().getBossBarHud() == null) {
            return false;
        }
        return Mc.INSTANCE.getInGameHud().getBossBarHud().bossBars.values().stream()
                .anyMatch(bar -> isPvPBossbarText(bar.getName().getString()));
    }

    public static boolean isPvPBossbarText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (PVP_PATTERN.matcher(text).find()) {
            return true;
        }
        return isSpookyTimeServer() && SPOOKYTIME_DUEL_PATTERN.matcher(text).find();
    }

    private static boolean isSpookyTimeServer() {
        final net.minecraft.client.network.ServerInfo server;
        try {
            server = Mc.INSTANCE.getServerInfo();
        } catch (RuntimeException ignored) {
            return false;
        }
        if (server == null || server.address == null) return false;
        String address = server.address.toLowerCase(Locale.ROOT);
        return address.contains("spookytime") || address.contains("spoookytime");
    }
}
