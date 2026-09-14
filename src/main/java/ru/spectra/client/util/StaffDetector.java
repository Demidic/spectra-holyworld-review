package ru.spectra.client.util;
import ru.spectra.client.math.Easings;
import ru.spectra.client.type.Mc;
import ru.spectra.client.model.StaffPlayerEntry;
import ru.spectra.client.type.StaffStatus;
import ru.spectra.client.event.TabListEntryEvent;
import ru.spectra.client.render.ToggleAnimator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;

public final class StaffDetector {
    private static final long UPDATE_INTERVAL_NANOS = 250_000_000L;
    public static Set<StaffPlayerEntry> staffPlayers = new LinkedHashSet();
    public static final Set<String> customStaffNames = new LinkedHashSet();
    public static final Pattern validNamePattern = Pattern.compile("^\\w{3,16}$");
    public static final Pattern staffKeywordPattern = Pattern.compile(".*(mod|der|adm|help|wne|хелп|адм|поддержка|кура|own|taf|curat|dev|supp|yt|сотруд).*");
    public static final Map<String, AbstractTexture> headTextureCache = new HashMap();

    public static AbstractTexture defaultHeadTexture;
    private static long nextUpdateNanos;

    public static void clearCaches() {
        headTextureCache.clear();
        defaultHeadTexture = null;
        nextUpdateNanos = 0L;
    }

    public static void onPlayerListEvent(TabListEntryEvent class038Var) {
        PlayerListEntry entry = class038Var.currentEntry();
        if (!Mc.INSTANCE.isWorldLoaded() || entry == null
                || entry.getProfile() == null
                || entry.getProfile().getName() == null) {
            return;
        }
        headTextureCache.put(
                entry.getProfile().getName(),
                Mc.INSTANCE.getTextureManager().getTexture(
                        entry.getSkinTextures().texture())
        );
    }

    public static AbstractTexture getDefaultHeadTexture() {
        if (defaultHeadTexture == null) {
            defaultHeadTexture = Mc.INSTANCE.getTextureManager().getTexture(Identifier.of("minecraft", "textures/entity/player/wide/steve.png"));
        }
        return defaultHeadTexture;
    }

    public static void update() {
        long now = System.nanoTime();
        if (now < nextUpdateNanos) {
            return;
        }
        nextUpdateNanos = now + UPDATE_INTERVAL_NANOS;

        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        ClientWorld world = Mc.INSTANCE.getWorld();
        ClientPlayNetworkHandler networkHandler = Mc.INSTANCE.getNetworkHandler();
        if (player == null || world == null || networkHandler == null) {
            return;
        }

        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            if (entry != null && entry.getProfile() != null
                    && entry.getProfile().getName() != null) {
                onlineNames.add(entry.getProfile().getName()
                        .toLowerCase(Locale.ROOT));
            }
        }

        String localName = player.getName().getString();
        Map<String, StaffPlayerEntry> detected = new HashMap<>();
        world.getScoreboard().getTeams().stream()
                .sorted(Comparator.comparing(team -> team.getName()))
                .forEach(team -> {
                    String prefix = team.getPrefix() == null
                            ? "" : team.getPrefix().getString();
                    if (StringHelper.stripTextFormat(prefix.replace("●", ""))
                            .trim().equalsIgnoreCase("d.helper")) {
                        return;
                    }
                    boolean staffPrefix = isStaffPrefix(prefix);
                    for (String name : team.getPlayerList()) {
                        if (name == null || name.equals(localName)
                                || !validNamePattern.matcher(name).matches()
                                || (!staffPrefix && !isCustomStaffName(name))) {
                            continue;
                        }
                        StaffStatus status = onlineNames.contains(
                                name.toLowerCase(Locale.ROOT))
                                ? StaffStatus.PLAYING : StaffStatus.SPEC;
                        detected.put(name, new StaffPlayerEntry(
                                headTextureCache.getOrDefault(
                                        name, getDefaultHeadTexture()),
                                prefix,
                                name,
                                status,
                                new ToggleAnimator(
                                        220, Easings.EASE_IN_OUT_CUBIC)
                        ));
                    }
                });

        for (StaffPlayerEntry existing : List.copyOf(staffPlayers)) {
            if (!detected.containsKey(existing.name())) {
                existing.stateAnimation().state(false);
            }
        }
        staffPlayers.removeIf(existing -> !detected.containsKey(existing.name())
                && existing.stateAnimation().isZero());

        for (Map.Entry<String, StaffPlayerEntry> entry : detected.entrySet()) {
            Optional<StaffPlayerEntry> previous = staffPlayers.stream()
                    .filter(existing -> existing.name().equals(entry.getKey()))
                    .findFirst();
            StaffPlayerEntry current = entry.getValue();
            if (previous.isPresent()) {
                StaffPlayerEntry existing = previous.get();
                existing.stateAnimation().state(true);
                if (existing.status() != current.status()
                        || !existing.prefix().equals(current.prefix())) {
                    staffPlayers.remove(existing);
                    staffPlayers.add(new StaffPlayerEntry(
                            current.headTexture(), current.prefix(),
                            existing.name(), current.status(),
                            existing.stateAnimation()
                    ));
                }
            } else {
                current.stateAnimation().state(true);
                staffPlayers.add(current);
            }
        }
    }

    public static boolean isStaffPrefix(String str) {
        return staffKeywordPattern.matcher(str.toLowerCase(Locale.ROOT)).matches();
    }

    public static boolean addCustomStaffName(String str) {
        return customStaffNames.add(str.toLowerCase(Locale.ROOT));
    }

    public static boolean removeCustomStaffName(String str) {
        return customStaffNames.remove(str.toLowerCase(Locale.ROOT));
    }

    public static void clearCustomStaffNames() {
        customStaffNames.clear();
    }

    public static Set<String> getCustomStaffNames() {
        return Set.copyOf(customStaffNames);
    }

    public static boolean isCustomStaffName(String str) {
        return customStaffNames.contains(str.toLowerCase(Locale.ROOT));
    }

    public static int getRankPriority(StaffPlayerEntry class118Var) {
        switch (class118Var.prefix().toLowerCase(Locale.ROOT)) {
            case "admin":
            case "админ":
                return 0;
            case "ml.admin":
                return 1;
            case "gl.moder":
                return 2;
            case "st.moder":
            case "s.moder":
                return 3;
            case "moder":
            case "модератор":
            case "куратор":
                return 4;
            case "j.moder":
                return 5;
            case "st.helper":
                return 6;
            case "helper+":
                return 7;
            case "helper":
                return 8;
            case "yt+":
                return 9;
            case "yt":
                return 10;
            default:
                return 11;
        }
    }

    public static List<StaffPlayerEntry> sortedStaff() {
        return staffPlayers.stream().sorted(Comparator.comparingInt(StaffDetector::getRankPriority).thenComparing((v0) -> {
            return v0.name();
        }, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public StaffDetector() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
