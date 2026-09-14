package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public class BoxRenderShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Box box = Box.from(net.minecraft.util.math.Vec3d.ZERO);
    private int color;
    private boolean cameraRelative;

    public BoxRenderShape set(Matrix4f matrix4f, Box box, int i, boolean z, boolean z2, boolean z3) {
        this.matrix.set(matrix4f);
        this.box = box;
        this.color = i;
        this.cameraRelative = z3;
        this.renderState.set(z, true, false, z2, VertexFormats.POSITION_COLOR, 1.0f,
                VertexFormat.DrawMode.QUADS, ShaderProgramKeys.POSITION_COLOR, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitBox(this.matrix, bufferBuilder, this.box, this.color, this.cameraRelative);
    }
}
