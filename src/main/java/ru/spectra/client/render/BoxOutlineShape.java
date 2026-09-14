package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class BoxOutlineShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private Box box = Box.from(Vec3d.ZERO);
    private int outlineColor;
    private int wireframeColor;
    private boolean drawOutline;
    private boolean drawWireframe;
    private boolean cameraRelative;

    public BoxOutlineShape set(Matrix4f matrix4f, Box box, int i, int i2, boolean z, boolean z2, float f, boolean z3) {
        return set(matrix4f, box, i, i2, z, z2, f, true, z3);
    }

    public BoxOutlineShape set(Matrix4f matrix4f, Box box, int i, int i2, boolean z, boolean z2,
                               float f, boolean depthTest, boolean cameraRelative) {
        return set(matrix4f, box, i, i2, z, z2, f, depthTest, cameraRelative, false);
    }

    public BoxOutlineShape set(Matrix4f matrix4f, Box box, int i, int i2, boolean z, boolean z2,
                               float f, boolean depthTest, boolean cameraRelative, boolean additiveBlend) {
        this.matrix.set(matrix4f);
        this.box = box;
        this.outlineColor = i;
        this.wireframeColor = i2;
        this.drawOutline = z;
        this.drawWireframe = z2;
        this.cameraRelative = cameraRelative;
        this.renderState.set(additiveBlend, true, false, depthTest, VertexFormats.LINES, f,
                VertexFormat.DrawMode.LINES, ShaderProgramKeys.RENDERTYPE_LINES, null, false);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitBoxOutline(this.matrix, bufferBuilder, this.box, this.outlineColor,
                this.wireframeColor, this.drawOutline, this.drawWireframe, this.cameraRelative);
    }
}
