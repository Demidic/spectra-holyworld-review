package ru.spectra.client.render;

import net.minecraft.client.render.VertexConsumer;

/** Applies the Target HUD portrait opacity without a relocatable Mixin inner class. */
public final class PortraitAlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity;

    public PortraitAlphaVertexConsumer(VertexConsumer delegate, float opacity) {
        this.delegate = delegate;
        this.opacity = opacity;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(red, green, blue, Math.round(alpha * this.opacity));
        return this;
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        this.delegate.color(red, green, blue, alpha * this.opacity);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        this.delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        this.delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }
}
