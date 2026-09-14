package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class QuadRenderShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Vec3d first = Vec3d.ZERO;
    private Vec3d second = Vec3d.ZERO;
    private Vec3d third = Vec3d.ZERO;
    private Vec3d fourth = Vec3d.ZERO;
    private int color;
    private boolean cameraRelative;

    public QuadRenderShape set(Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, Vec3d vec3d4, int i, boolean z) {
        this.matrix.set(matrix4f);
        this.first = vec3d;
        this.second = vec3d2;
        this.third = vec3d3;
        this.fourth = vec3d4;
        this.color = i;
        this.cameraRelative = z;
        this.renderState.set(false, true, false, true, VertexFormats.POSITION_COLOR, 1.0f,
                VertexFormat.DrawMode.QUADS, ShaderProgramKeys.POSITION_COLOR, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitQuad(this.matrix, bufferBuilder, this.first, this.second,
                this.third, this.fourth, this.color, this.cameraRelative);
    }
}
