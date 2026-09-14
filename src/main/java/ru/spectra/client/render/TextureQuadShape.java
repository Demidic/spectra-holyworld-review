package ru.spectra.client.render;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class TextureQuadShape extends RenderShape {
    private final Matrix4f matrix = new Matrix4f();
    private float x;
    private float y;
    private float width;
    private float height;
    private int color;

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2) {
        return set(matrix4f, f, f2, f3, f4, identifier, i, z, z2, false);
    }

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2, boolean z3) {
        this.matrix.set(matrix4f);
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.color = i;
        this.renderState.set(z2, true, z3, z, VertexFormats.POSITION_TEXTURE_COLOR, 1.0f,
                VertexFormat.DrawMode.QUADS, ShaderProgramKeys.POSITION_TEX_COLOR, identifier, true);
        return this;
    }

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4,
                                int textureId, int color, boolean depthTest,
                                boolean additiveBlend, boolean cameraRelative) {
        this.matrix.set(matrix4f);
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.color = color;
        this.renderState.set(additiveBlend, true, cameraRelative, depthTest,
                VertexFormats.POSITION_TEXTURE_COLOR, 1.0f, VertexFormat.DrawMode.QUADS,
                ShaderProgramKeys.POSITION_TEX_COLOR, textureId, true);
        return this;
    }

    @Override
    public void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
        ShapeRenderer.INSTANCE.emitTextureQuad(this.matrix, bufferBuilder, this.x, this.y, this.width, this.height, this.color);
    }
}
