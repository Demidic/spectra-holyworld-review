package ru.spectra.client.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.util.DrawEngine;

/** Renders the brand mark from a signed-distance field at any UI scale. */
public final class SpectraLogoRenderer {
    private static final float VECTOR_WIDTH = 580.0f;
    private static final float VECTOR_HEIGHT = 493.0f;
    private static final float SDF_MARK_WIDTH = 96.0f;
    private static final float SDF_MARK_HEIGHT = 82.0f;
    private static final float SDF_PADDING = 12.0f;
    private static final float TEXTURE_WIDTH = SDF_MARK_WIDTH + SDF_PADDING * 2.0f;
    private static final float TEXTURE_HEIGHT = SDF_MARK_HEIGHT + SDF_PADDING * 2.0f;
    private static final GlTexture TEXTURE = new GlTexture(
            new ClasspathResource("/icons/brand/spectra_mark_sdf.png"))
            .magFilter(GL11.GL_LINEAR)
            .minFilter(GL11.GL_LINEAR)
            .wrapX(GL12.GL_CLAMP_TO_EDGE)
            .wrapY(GL12.GL_CLAMP_TO_EDGE);

    private SpectraLogoRenderer() {
    }

    public static float heightForWidth(float width) {
        return width * VECTOR_HEIGHT / VECTOR_WIDTH;
    }

    public static void drawCentered(DrawEngine draw, Matrix4f matrix, float x, float centerY,
                                    float width, int color) {
        float height = heightForWidth(width);
        float padX = width * SDF_PADDING / SDF_MARK_WIDTH;
        float padY = height * SDF_PADDING / SDF_MARK_HEIGHT;
        draw.textureSdf(matrix,
                x - padX,
                centerY - height * 0.5f - padY,
                width * TEXTURE_WIDTH / SDF_MARK_WIDTH,
                height * TEXTURE_HEIGHT / SDF_MARK_HEIGHT,
                0.0f, 0.0f, 1.0f, 1.0f,
                draw.bindTexture(TEXTURE.textureWithSTB()), color);
    }

    public static void drawCentered(DrawCtx ctx, float x, float centerY, float width, int color) {
        drawCentered(ctx.drawEngine(), ctx.matrixStack().peek().getPositionMatrix(),
                ctx.layoutContext().toPhysical(x),
                ctx.layoutContext().toPhysical(centerY),
                ctx.layoutContext().toPhysical(width), color);
    }
}
