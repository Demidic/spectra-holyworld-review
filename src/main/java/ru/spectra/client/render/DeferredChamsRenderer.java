package ru.spectra.client.render;

import ru.spectra.client.util.ColorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/**
 * Keeps Chams out of the framebuffer sampled by blurred fog.
 *
 * <p>The entity pass still writes the model depth at its original point in
 * the vanilla pipeline, so later world layers preserve their old ordering.
 * Its color is retained in a pooled GPU buffer and drawn after the fog
 * composite. This avoids both a blurred model and the halo produced by
 * drawing a sharp copy over an already blurred one.</p>
 */
public final class DeferredChamsRenderer {
    private final List<VertexBuffer> bufferPool = new ArrayList<>();
    private final List<DeferredDraw> draws = new ArrayList<>();
    private final ByteBuffer colorMaskScratch = BufferUtils.createByteBuffer(4);

    private boolean deferColor;
    private int usedBuffers;

    public void beginFrame(boolean deferColor) {
        this.deferColor = deferColor;
        this.usedBuffers = 0;
        this.draws.clear();
    }

    public boolean shouldDeferColor() {
        return this.deferColor;
    }

    public void drawDepthAndDefer(BuiltBuffer builtBuffer, int color, boolean additiveBlend) {
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        VertexBuffer vertexBuffer = acquireBuffer();

        BufferRenderer.reset();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);

        readColorMask();
        GL11.glColorMask(false, false, false, false);
        try {
            vertexBuffer.draw(modelView, projection, RenderSystem.getShader());
        } finally {
            restoreColorMask();
            VertexBuffer.unbind();
        }

        this.draws.add(new DeferredDraw(
                vertexBuffer, modelView, projection, color, additiveBlend, cull
        ));
    }

    public void drawDeferredColor() {
        if (this.draws.isEmpty()) {
            this.deferColor = false;
            return;
        }

        SavedState state = SavedState.capture();
        try {
            BufferRenderer.reset();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            GL11.glColorMask(true, true, true, true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION);

            for (DeferredDraw draw : this.draws) {
                if (draw.cull()) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
                RenderSystem.enableBlend();
                if (draw.additiveBlend()) {
                    RenderSystem.blendFunc(
                            GlStateManager.SrcFactor.SRC_ALPHA,
                            GlStateManager.DstFactor.ONE
                    );
                } else {
                    RenderSystem.defaultBlendFunc();
                }
                int color = draw.color();
                RenderSystem.setShaderColor(
                        ColorUtil.red(color) / 255.0f,
                        ColorUtil.green(color) / 255.0f,
                        ColorUtil.blue(color) / 255.0f,
                        ColorUtil.alpha(color) / 255.0f
                );
                draw.buffer().bind();
                draw.buffer().draw(
                        draw.modelView(), draw.projection(), RenderSystem.getShader()
                );
            }
        } finally {
            VertexBuffer.unbind();
            BufferRenderer.resetCurrentVertexBuffer();
            this.draws.clear();
            this.deferColor = false;
            state.restore();
        }
    }

    private VertexBuffer acquireBuffer() {
        if (this.usedBuffers == this.bufferPool.size()) {
            this.bufferPool.add(new VertexBuffer(GlUsage.DYNAMIC_WRITE));
        }
        return this.bufferPool.get(this.usedBuffers++);
    }

    private void readColorMask() {
        this.colorMaskScratch.clear();
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, this.colorMaskScratch);
    }

    private void restoreColorMask() {
        GL11.glColorMask(
                this.colorMaskScratch.get(0) != 0,
                this.colorMaskScratch.get(1) != 0,
                this.colorMaskScratch.get(2) != 0,
                this.colorMaskScratch.get(3) != 0
        );
    }

    private record DeferredDraw(
            VertexBuffer buffer,
            Matrix4f modelView,
            Matrix4f projection,
            int color,
            boolean additiveBlend,
            boolean cull
    ) {
    }

    private record SavedState(
            ShaderProgram shader,
            float[] shaderColor,
            boolean depthTest,
            boolean depthMask,
            int depthFunction,
            boolean cull,
            boolean blend,
            int blendSrcRgb,
            int blendDstRgb,
            int blendSrcAlpha,
            int blendDstAlpha,
            int blendEquationRgb,
            int blendEquationAlpha,
            boolean[] colorMask
    ) {
        private static SavedState capture() {
            float[] currentColor = RenderSystem.getShaderColor();
            boolean[] colorMask = new boolean[4];
            ByteBuffer mask = BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int index = 0; index < colorMask.length; index++) {
                colorMask[index] = mask.get(index) != 0;
            }
            return new SavedState(
                    RenderSystem.getShader(),
                    currentColor.clone(),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),
                    colorMask
            );
        }

        private void restore() {
            RenderSystem.setShader(this.shader);
            RenderSystem.setShaderColor(
                    this.shaderColor[0], this.shaderColor[1],
                    this.shaderColor[2], this.shaderColor[3]
            );
            RenderSystem.depthMask(this.depthMask);
            RenderSystem.depthFunc(this.depthFunction);
            if (this.depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (this.cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            if (this.blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            GL20.glBlendEquationSeparate(this.blendEquationRgb, this.blendEquationAlpha);
            RenderSystem.blendFuncSeparate(
                    this.blendSrcRgb, this.blendDstRgb,
                    this.blendSrcAlpha, this.blendDstAlpha
            );
            GL11.glColorMask(
                    this.colorMask[0], this.colorMask[1],
                    this.colorMask[2], this.colorMask[3]
            );
        }
    }
}
