package ru.spectra.client.render;

/**
 * Shared motion curve for HUD elements that fade between an empty and a
 * populated state. Keeping the values here makes the render path predictable
 * and lets an interrupted entrance continue smoothly as an exit (and back).
 */
public final class SoftVisibilityTransition {
    /** Matches the element-layer blur used by the Wexside HUD reference. */
    public static final float MAX_BLUR_RADIUS = 16.0f;
    /** Prevents the blur kernel from clipping outside the captured widget. */
    public static final float LAYER_PADDING = 32.0f;

    private SoftVisibilityTransition() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static float progress(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static float blurRadius(float value) {
        return MAX_BLUR_RADIUS * (1.0f - progress(value));
    }

    public static boolean target(boolean enabled, boolean contentVisible,
                                 boolean editorVisible) {
        return enabled && (contentVisible || editorVisible);
    }

    public static boolean needsComposite(float value) {
        float progress = progress(value);
        return progress > 0.01f && progress < 0.999f;
    }
}
