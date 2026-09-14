package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class TriangleShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Vec3d first = Vec3d.ZERO;
    private Vec3d second = Vec3d.ZERO;
    private Vec3d third = Vec3d.ZERO;
    private int color;
    private boolean cameraRelative;

    public TriangleShape set(Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, int i, boolean z) {
        return set(matrix4f, vec3d, vec3d2, vec3d3, i, true, z);
    }

    public TriangleShape set(Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3,
                             int i, boolean depthTest, boolean cameraRelative) {
        this.matrix.set(matrix4f);
        this.first = vec3d;
        this.second = vec3d2;
        this.third = vec3d3;
        this.color = i;
        this.cameraRelative = cameraRelative;
        this.renderState.set(false, true, false, depthTest, VertexFormats.POSITION_COLOR, 1.0f,
                VertexFormat.DrawMode.TRIANGLES, ShaderProgramKeys.POSITION_COLOR, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitTriangle(this.matrix, bufferBuilder, this.first, this.second,
                this.third, this.color, this.cameraRelative);
    }
}
