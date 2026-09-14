package ru.spectra.client.render;

import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.FramebufferUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Applies a screen-space blur only where the world depth enters the fog range. */
public final class BlurFogRenderer {
    private static final float NEAR_PLANE = 0.05f;
    private static final int BLUR_RADIUS = 18;

    private final BlurEffect blur = new BlurEffect();
    private final FullscreenQuad quad = new FullscreenQuad();
    private Framebuffer depthCopy;
    private ShaderProgram composite;
    private ShaderUniform blurTexture;
    private ShaderUniform depthTexture;
    private ShaderUniform nearPlane;
    private ShaderUniform farPlane;
    private ShaderUniform fogDistance;

    public void render(float distance, float projectionFarPlane) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = Mc.INSTANCE.getWindow().getFramebufferWidth();
        int height = Mc.INSTANCE.getWindow().getFramebufferHeight();
        Framebuffer main = client.getFramebuffer();
        if (width <= 0 || height <= 0 || main == null || !main.useDepthAttachment) {
            return;
        }

        SavedState state = SavedState.capture();
        try {
            ensureInitialized();
            depthCopy = FramebufferUtil.ensureFramebuffer(depthCopy, width, height,
                    () -> new SimpleFramebuffer(width, height, true));
            FramebufferUtil.resizeIfNeeded(depthCopy, width, height);
            depthCopy.copyDepthFrom(main);

            blur.apply(BLUR_RADIUS);
            Framebuffer blurred = blur.getBlurFramebuffer();
            if (blurred == null) {
                return;
            }

            main.beginWrite(true);
            RenderSystem.viewport(0, 0, width, height);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
            );

            composite.bind();
            bindTexture(0, blurred.getColorAttachment());
            bindTexture(1, depthCopy.getDepthAttachment());
            blurTexture.uploadInt(0);
            depthTexture.uploadInt(1);
            nearPlane.uploadFloat(NEAR_PLANE);
            farPlane.uploadFloat(Math.max(projectionFarPlane, distance + 1.0f));
            fogDistance.uploadFloat(Math.max(distance, 2.1f));
            quad.draw();
            composite.unbind();
        } finally {
            state.restore();
        }
    }

    private void ensureInitialized() {
        if (composite != null) {
            return;
        }
        blur.init();
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        composite = new ShaderProgram(
                resources.route("shaders/blur_fog.fsh"),
                resources.route("shaders/framebuffer.vsh")
        );
        blurTexture = composite.uniform("uBlurTexture");
        depthTexture = composite.uniform("uDepthTexture");
        nearPlane = composite.uniform("uNearPlane");
        farPlane = composite.uniform("uFarPlane");
        fogDistance = composite.uniform("uFogDistance");
    }

    private static void bindTexture(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    public void release() {
        if (depthCopy != null) {
            depthCopy.delete();
            depthCopy = null;
        }
        if (blur.horizontalFramebuffer != null) {
            blur.horizontalFramebuffer.delete();
            blur.horizontalFramebuffer = null;
        }
        if (blur.blurFramebuffer != null) {
            blur.blurFramebuffer.delete();
            blur.blurFramebuffer = null;
        }
        if (blur.blurShader != null) {
            blur.blurShader.release();
            blur.blurShader = null;
        }
        if (composite != null) {
            composite.release();
            composite = null;
        }
    }

    private record SavedState(
            int activeTexture, int texture0, int texture1, int program,
            int readFramebuffer, int drawFramebuffer, int[] viewport,
            boolean depthTest, boolean depthMask, boolean cull, boolean blend,
            boolean scissor, int[] scissorBox, boolean[] colorMask,
            int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha,
            int blendEquationRgb, int blendEquationAlpha,
            int vertexArray, int arrayBuffer, int elementArrayBuffer
    ) {
        private static SavedState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            int texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(activeTexture);
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int[] scissorBox = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            boolean[] colorMask = new boolean[4];
            var colorMaskBuffer = BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);
            for (int index = 0; index < colorMask.length; index++) {
                colorMask[index] = colorMaskBuffer.get(index) != 0;
            }
            return new SavedState(
                    activeTexture, texture0, texture1,
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), viewport,
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), scissorBox, colorMask,
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                    GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING)
            );
        }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.depthMask(depthMask);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture1);
            GL13.glActiveTexture(activeTexture);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            if (vertexArray != 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
            }
            GL20.glUseProgram(program);
        }
    }
}
