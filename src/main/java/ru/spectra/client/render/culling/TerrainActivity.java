package ru.spectra.client.render.culling;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Use the additional cache during stationary-camera turns, where profiling found a benefit. */
final class TerrainActivity {
    private static final long ROTATION_GRACE_NANOS = 250_000_000L;
    private final Matrix4f previous = new Matrix4f();
    private double x, y, z;
    private long lastTurn;
    private boolean initialized, turned;

    boolean update(Matrix4fc clip, double x, double y, double z, long now) {
        boolean stationary = this.initialized && this.x == x && this.y == y && this.z == z;
        if (this.initialized && !this.previous.equals(clip)) {
            this.lastTurn = now;
            this.turned = true;
        }
        this.previous.set(clip);
        this.x = x; this.y = y; this.z = z;
        this.initialized = true;
        // The grace period bridges gaps between mouse events; it never delays visibility updates.
        return stationary && this.turned && now - this.lastTurn < ROTATION_GRACE_NANOS;
    }
}
