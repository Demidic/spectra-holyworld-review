package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class HemisphereShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Vec3d center = Vec3d.ZERO;
    private float radius;
    private int segments;
    private int color;
    private boolean cameraRelative;

    public HemisphereShape set(Matrix4f matrix4f, Vec3d vec3d, float f, int i, int i2, boolean z) {
        this.matrix.set(matrix4f);
        this.center = vec3d;
        this.radius = f;
        this.segments = i;
        this.color = i2;
        this.cameraRelative = z;
        this.renderState.set(false, true, false, true, VertexFormats.POSITION_COLOR, 1.0f,
                VertexFormat.DrawMode.TRIANGLES, ShaderProgramKeys.POSITION_COLOR, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitHemisphere(this.matrix, bufferBuilder, this.center, this.radius,
                this.segments, this.color, this.cameraRelative);
    }
}
