package ru.spectra.client.render;

import ru.spectra.client.Spectra;
import java.util.Locale;
import org.lwjgl.opengl.GL11;

/**
 * Chooses render-target sizes from the actual OpenGL renderer.  Expensive
 * procedural effects stay enabled on integrated GPUs, but run on smaller
 * intermediate buffers so a config copied from a discrete GPU cannot turn
 * into a single-digit-FPS workload.
 */
final class RenderPerformanceProfile {
    private static volatile RenderPerformanceProfile detectedProfile;
    private static final RenderPerformanceProfile STANDARD =
            new RenderPerformanceProfile(2048, 1024, 4, 2, 1, 2);
    private static final RenderPerformanceProfile CONSTRAINED =
            new RenderPerformanceProfile(1024, 512, 8, 4, 2, 4);

    private final int skyCacheWidth;
    private final int skyCacheHeight;
    private final int skyCacheTiles;
    private final int handEffectDivisor;
    private final int handMaskDivisor;
    private final int screenBlurDivisor;

    private RenderPerformanceProfile(
            int skyCacheWidth,
            int skyCacheHeight,
            int skyCacheTiles,
            int handEffectDivisor,
            int handMaskDivisor,
            int screenBlurDivisor
    ) {
        this.skyCacheWidth = skyCacheWidth;
        this.skyCacheHeight = skyCacheHeight;
        this.skyCacheTiles = skyCacheTiles;
        this.handEffectDivisor = handEffectDivisor;
        this.handMaskDivisor = handMaskDivisor;
        this.screenBlurDivisor = screenBlurDivisor;
    }

    static RenderPerformanceProfile current() {
        RenderPerformanceProfile profile = detectedProfile;
        if (profile != null) {
            return profile;
        }
        synchronized (RenderPerformanceProfile.class) {
            profile = detectedProfile;
            if (profile == null) {
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                profile = forRenderer(renderer);
                detectedProfile = profile;
                Spectra.LOGGER.info(
                        "Adaptive render profile: {} (renderer={}, sky={}x{}/{}, hand=1/{}, mask=1/{}, blur=1/{})",
                        profile == CONSTRAINED ? "constrained" : "standard",
                        renderer == null ? "unknown" : renderer,
                        profile.skyCacheWidth,
                        profile.skyCacheHeight,
                        profile.skyCacheTiles,
                        profile.handEffectDivisor,
                        profile.handMaskDivisor,
                        profile.screenBlurDivisor
                );
            }
        }
        return profile;
    }

    static RenderPerformanceProfile forRenderer(String renderer) {
        String value = renderer == null
                ? ""
                : renderer.toLowerCase(Locale.ROOT);
        boolean genericAmdIntegrated = value.contains("amd radeon")
                && value.contains("graphics")
                && !value.contains(" rx ")
                && !value.contains(" pro ");
        boolean integrated = value.contains("radeon(tm) graphics")
                || genericAmdIntegrated
                || value.contains("intel(r) uhd")
                || value.contains("intel(r) hd graphics")
                || value.contains("iris(r) xe")
                || value.contains("microsoft basic render")
                || value.contains("qualcomm")
                || value.contains("adreno");
        return integrated ? CONSTRAINED : STANDARD;
    }

    int skyCacheWidth() {
        return skyCacheWidth;
    }

    int skyCacheHeight() {
        return skyCacheHeight;
    }

    int skyCacheTiles() {
        return skyCacheTiles;
    }

    int handEffectDivisor() {
        return handEffectDivisor;
    }

    int handMaskDivisor() {
        return handMaskDivisor;
    }

    int screenBlurDivisor() {
        return screenBlurDivisor;
    }
}
