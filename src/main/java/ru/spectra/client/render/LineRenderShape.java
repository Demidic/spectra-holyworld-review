package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class LineRenderShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Vec3d from = Vec3d.ZERO;
    private Vec3d to = Vec3d.ZERO;
    private int color;
    private boolean cameraRelative;

    public LineRenderShape set(Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, int i, float f, boolean z) {
        this.matrix.set(matrix4f);
        this.from = vec3d;
        this.to = vec3d2;
        this.color = i;
        this.cameraRelative = z;
        this.renderState.set(false, true, false, true, VertexFormats.LINES, f,
                VertexFormat.DrawMode.LINES, ShaderProgramKeys.RENDERTYPE_LINES, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitLine(this.matrix, bufferBuilder, this.from, this.to, this.color, this.cameraRelative);
    }
}
