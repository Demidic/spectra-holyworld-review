package ru.spectra.client.net;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.Module;
import ru.spectra.client.util.ServerUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Implements HolyWorld's documented raw UTF-8 JSON play-channel protocol. */
public final class HolyWorldFeatureControl {
    private static final Logger LOGGER = LoggerFactory.getLogger("Spectra/HolyWorld");
    private static final HolyWorldFeaturePolicy POLICY = new HolyWorldFeaturePolicy();
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private static ClientPlayNetworkHandler connection;
    private static long requestStartedAt;
    private static boolean requested;
    private static boolean registered;
    private static boolean transportAvailable;
    private static boolean requestTimedOut;
    private static final Set<Module> previouslyEnabled = new HashSet<>();

    private HolyWorldFeatureControl() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            PayloadTypeRegistry.playC2S().register(FeaturePayload.ID, FeaturePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(FeaturePayload.ID, FeaturePayload.CODEC);
            transportAvailable = ClientPlayNetworking.registerGlobalReceiver(
                    FeaturePayload.ID, (payload, context) -> {
                        ClientPlayNetworkHandler source = context.player().networkHandler;
                        context.client().execute(() -> {
                            if (source == connection && isHolyWorldContext()
                                    && POLICY.receive(source, payload.json())) {
                                if (POLICY.isVerified()) {
                                    for (Module module : previouslyEnabled) {
                                        if (module.isAvailable()) {
                                            module.setState(true);
                                        }
                                    }
                                    previouslyEnabled.clear();
                                }
                                enforce();
                                LOGGER.info("Feature Control response: verified={}", POLICY.isVerified());
                            }
                        });
                    });
        } catch (IllegalArgumentException conflict) {
            // Do not replace another mod's codec/receiver or prevent that mod loading.
            // HolyWorld features remain closed if this transport cannot be owned.
            LOGGER.warn("Feature Control channel is already owned by another mod; access stays closed");
        }
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            connection = handler;
            requested = false;
            requestTimedOut = false;
            POLICY.disconnect();
            previouslyEnabled.clear();
            if (Spectra.INSTANCE != null && Spectra.INSTANCE.moduleRepository() != null) {
                for (Module module : Spectra.INSTANCE.moduleRepository().getModules()) {
                    if (module.state) {
                        previouslyEnabled.add(module);
                    }
                }
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connection = handler;
            sendIfReady(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (connection == handler) {
                connection = null;
                requested = false;
                POLICY.disconnect();
                previouslyEnabled.clear();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            sendIfReady(client);
            if (requested && !requestTimedOut && !POLICY.isVerified()
                    && System.nanoTime() - requestStartedAt >= TIMEOUT_NS) {
                POLICY.fail(connection);
                requestTimedOut = true;
                LOGGER.warn("Feature Control is unverified after 5 seconds; access stays closed");
            }
        });
    }

    private static void sendIfReady(MinecraftClient client) {
        if (requested || connection == null || client.getNetworkHandler() != connection
                || !isHolyWorldContext() || Spectra.INSTANCE == null
                || Spectra.INSTANCE.moduleRepository() == null
                || Spectra.INSTANCE.commandDispatcher() == null) {
            return;
        }
        requested = true;
        requestStartedAt = System.nanoTime();
        java.util.stream.Stream<String> modules = Spectra.INSTANCE.moduleRepository().getModules()
                .stream().map(Module::getFeatureId);
        java.util.stream.Stream<String> commands = Spectra.INSTANCE.commandDispatcher().getRepository()
                .commands.values().stream().map(ru.spectra.client.command.ClientCommand::getFeatureId);
        List<String> features = java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(modules, commands),
                java.util.stream.Stream.of("dropall")).sorted().toList();
        String json = POLICY.begin(connection, features);
        enforce();
        if (!transportAvailable) {
            POLICY.fail(connection);
            return;
        }
        try {
            ClientPlayNetworking.send(new FeaturePayload(json));
            LOGGER.info("checkFeatures sent: client=spectra, featureCount={}", features.size());
        } catch (RuntimeException unavailable) {
            POLICY.fail(connection);
            LOGGER.warn("Feature Control request could not be sent; access stays closed");
        }
    }

    public static boolean allows(String featureId) {
        return !isHolyWorldContext() || POLICY.allows(featureId);
    }

    private static boolean isHolyWorldContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isInSingleplayer() || client.getNetworkHandler() == null) {
            return false;
        }
        return (client.getCurrentServerEntry() != null
                && HolyWorldFeaturePolicy.isHolyWorldHost(client.getCurrentServerEntry().address))
                // Brand fallback also covers direct-IP connections and network aliases.
                || "HolyWorld".equals(ServerUtil.getServer());
    }

    private static void enforce() {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null) {
            return;
        }
        for (Module module : Spectra.INSTANCE.moduleRepository().getModules()) {
            module.enforceServerAccessPolicy();
            if (!module.isVisibleInMenu()) {
                String key = "module:" + module.getName().toLowerCase(java.util.Locale.ROOT);
                Spectra.INSTANCE.notificationRepository().notifications.removeIf(
                        notification -> key.equals(notification.coalesceKey()));
            }
        }
        if (Spectra.INSTANCE.menuWindow() != null) {
            Spectra.INSTANCE.tabsController().render().refreshFrameVisibility();
            Spectra.INSTANCE.tabsController().misc().refreshFrameVisibility();
        }
    }

    record FeaturePayload(String json) implements CustomPayload {
        static final Id<FeaturePayload> ID =
                new Id<>(Identifier.of("liteapi", "feature-control"));
        static final PacketCodec<PacketByteBuf, FeaturePayload> CODEC = new PacketCodec<>() {
            @Override
            public FeaturePayload decode(PacketByteBuf buffer) {
                int length = buffer.readableBytes();
                if (length > HolyWorldFeaturePolicy.MAX_PAYLOAD_BYTES) {
                    throw new IllegalArgumentException("Feature Control payload too large");
                }
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                return new FeaturePayload(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void encode(PacketByteBuf buffer, FeaturePayload value) {
                byte[] bytes = value.json().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > HolyWorldFeaturePolicy.MAX_PAYLOAD_BYTES) {
                    throw new IllegalArgumentException("Feature Control payload too large");
                }
                // No writeString prefix: the upstream wire format is raw JSON bytes.
                buffer.writeBytes(bytes);
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
