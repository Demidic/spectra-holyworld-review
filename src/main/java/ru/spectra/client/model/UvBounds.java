package ru.spectra.client.model;

public class UvBounds {
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;

    public UvBounds() {
        this(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public UvBounds(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    public float u0() {
        return this.u0;
    }

    public float u1() {
        return this.u1;
    }

    public float v0() {
        return this.v0;
    }

    public float v1() {
        return this.v1;
    }
}
