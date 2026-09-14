package ru.spectra.client.render.culling;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Current world-render arguments, written and consumed on the render thread. */
public final class TerrainFrameView {
    private static final Matrix4f CLIP = new Matrix4f().zero();
    private static final TerrainActivity ACTIVITY = new TerrainActivity();
    private static boolean allowCaching;
    private TerrainFrameView() { }

    public static void set(Matrix4fc view, Matrix4fc projection, double x, double y, double z) {
        CLIP.set(projection).mul(view);
        allowCaching = ACTIVITY.update(CLIP, x, y, z, System.nanoTime());
    }
    public static Matrix4fc clip() { return CLIP; }
    public static boolean allowCaching() { return allowCaching; }
}
