package ru.spectra.client.resource;

import ru.spectra.client.Spectra;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers a vault-backed image as a runtime texture for Minecraft APIs that
 * require an Identifier (for example DrawContext and SkinTextures).
 */
public final class ProtectedTextureRegistry {
    private static final Set<String> PENDING_OR_REGISTERED = ConcurrentHashMap.newKeySet();

    private ProtectedTextureRegistry() {
    }

    public static Identifier get(String resourcePath) {
        String normalized = normalize(resourcePath);
        Identifier runtimeId = Identifier.of("spectra_runtime", normalized);
        if (!PENDING_OR_REGISTERED.add(normalized)) {
            return runtimeId;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Runnable registration = () -> register(client, runtimeId, normalized);
        if (RenderSystem.isOnRenderThread()) {
            registration.run();
        } else {
            client.execute(registration);
        }
        return runtimeId;
    }

    private static void register(MinecraftClient client, Identifier runtimeId, String normalized) {
        try (InputStream input = new ClasspathResource("/" + normalized).stream()) {
            NativeImage image = NativeImage.read(input);
            boolean ownedByTexture = false;
            try {
                client.getTextureManager().registerTexture(
                        runtimeId,
                        new NativeImageBackedTexture(image)
                );
                ownedByTexture = true;
            } finally {
                if (!ownedByTexture) {
                    image.close();
                }
            }
        } catch (Exception exception) {
            PENDING_OR_REGISTERED.remove(normalized);
            Spectra.LOGGER.error("Failed to register protected texture {}", normalized, exception);
        }
    }

    private static String normalize(String resourcePath) {
        if (resourcePath == null) {
            throw new IllegalArgumentException("Protected texture path is absent");
        }
        String normalized = resourcePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()
                || normalized.startsWith("assets/")
                || normalized.contains("../")
                || normalized.equals("..")) {
            throw new IllegalArgumentException("Invalid protected texture path: " + resourcePath);
        }
        return normalized;
    }
}
