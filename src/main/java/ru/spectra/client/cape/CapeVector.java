package ru.spectra.client.cape;

import net.minecraft.util.math.MathHelper;

/**
 * Mutable vector used by the original 1.16.5 cape solver.
 */
public final class CapeVector {
    public float x;
    public float y;
    public float z;

    public CapeVector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CapeVector copy() {
        return new CapeVector(x, y, z);
    }

    public void set(CapeVector other) {
        x = other.x;
        y = other.y;
        z = other.z;
    }

    public CapeVector add(CapeVector other) {
        x += other.x;
        y += other.y;
        z += other.z;
        return this;
    }

    public CapeVector subtract(CapeVector other) {
        x -= other.x;
        y -= other.y;
        z -= other.z;
        return this;
    }

    public CapeVector rotateDegrees(float degrees) {
        float oldX = x;
        float oldY = y;
        float radians = (float) Math.toRadians(degrees);
        x = MathHelper.cos(radians) * oldX - MathHelper.sin(radians) * oldY;
        y = MathHelper.sin(radians) * oldX + MathHelper.cos(radians) * oldY;
        return this;
    }
}
