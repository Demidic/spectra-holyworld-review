package ru.spectra.client.render;

import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.WorldEffectType;
import ru.spectra.client.util.FramebufferUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Depth-aware world-space effects drawn by a full-screen raymarch pass.
 * The quad only launches shader rays; no particles, hitboxes or world meshes
 * are created for the visible effects.
 */
public final class WorldEffectsRenderer {
    private static final int MAX_EFFECTS = 20;

    private final Deque<EffectInstance> effects = new ArrayDeque<>();
    private final EnumMap<WorldEffectType, PendingEffect> pendingEffects =
            new EnumMap<>(WorldEffectType.class);
    private final BlurEffect blur = new BlurEffect();
    private final FullscreenQuad quad = new FullscreenQuad();
    private final long clockOriginNanos = System.nanoTime();
    private final float[] frameData = new float[MAX_EFFECTS * 4];
    private final float[] frameParams = new float[MAX_EFFECTS * 4];
    private final float[] frameDirections = new float[MAX_EFFECTS * 4];
    private final float[] frameAnchors = new float[MAX_EFFECTS * 4];
    private final Matrix4f frameViewProjection = new Matrix4f();
    private final Matrix4f frameInverseViewProjection = new Matrix4f();
    private Framebuffer sceneCopy;
    private ShaderProgram shader;
    private ShaderUniform sourceTexture;
    private ShaderUniform blurTexture;
    private ShaderUniform depthTexture;
    private ShaderUniform inverseViewProjection;
    private ShaderUniform viewProjection;
    private ShaderUniform viewMatrix;
    private ShaderUniform resolution;
    private ShaderUniform time;
    private ShaderUniform intensity;
    private ShaderUniform screenShake;
    private ShaderUniform effectCount;
    private ShaderUniform effectData;
    private ShaderUniform effectParams;
    private ShaderUniform effectDirections;
    private ShaderUniform effectAnchors;

    public void trigger(WorldEffectType type, Vec3d position, float strength) {
        trigger(type, position, strength, Vec3d.ZERO, position);
    }

    public void trigger(WorldEffectType type, Vec3d position, float strength, Vec3d direction) {
        trigger(type, position, strength, direction, position);
    }

    public void trigger(WorldEffectType type, Vec3d position, float strength,
                        Vec3d direction, Vec3d anchor) {
        long now = System.nanoTime();
        discardExpired(now);
        float seed = (float) ((now * 0.000000001d
                + position.hashCode() * 0.61803398875d) % 10_000.0d);
        Vec3d safeDirection = direction == null || direction.lengthSquared() < 0.000001d
                ? Vec3d.ZERO
                : direction.normalize();
        PendingEffect requested = new PendingEffect(
                type, position, safeDirection, anchor == null ? position : anchor,
                Math.max(0.1f, Math.min(1.5f, strength)), seed);
        if (activeCount(type) >= activeLimit(type)) {
            // A chain explosion or rapid hit sequence keeps only its newest
            // event. It is promoted after the current effect has fully faded.
            this.pendingEffects.put(type, requested);
            return;
        }
        this.effects.addLast(start(requested, now));
    }

    public boolean hasActiveEffects() {
        discardExpired(System.nanoTime());
        return !this.effects.isEmpty();
    }

    public void render(WorldRenderEvent event, float configuredIntensity,
                       int configuredBlurRadius, boolean shakeEnabled) {
        long now = System.nanoTime();
        discardExpired(now);
        if (this.effects.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        int width = Mc.INSTANCE.getWindow().getFramebufferWidth();
        int height = Mc.INSTANCE.getWindow().getFramebufferHeight();
        if (main == null || !main.useDepthAttachment || width <= 0 || height <= 0) {
            return;
        }

        SavedState state = SavedState.capture();
        try {
            ensureInitialized(width, height);
            // Keep the blurred scene alive for the complete effect lifetime.
            // Blood puddles use it for their wet reflection as well as airborne
            // refraction. Stopping this pass after the drops landed made a
            // puddle disappear, then flash back when a later hit restarted it.
            // This remains one shared blur pass per frame, not one per effect.
            blur.apply(Math.max(1, Math.min(60, configuredBlurRadius)), 2.4f);
            Framebuffer blurred = blur.getBlurFramebuffer();
            if (blurred == null) {
                return;
            }
            copyScene(main, this.sceneCopy, width, height);
            main.beginWrite(true);
            RenderSystem.viewport(0, 0, width, height);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);

            float[] data = this.frameData;
            float[] params = this.frameParams;
            float[] directions = this.frameDirections;
            float[] anchors = this.frameAnchors;
            // Preserve the zeroed unused slots of the original fresh arrays.
            Arrays.fill(data, 0.0f);
            Arrays.fill(params, 0.0f);
            Arrays.fill(directions, 0.0f);
            Arrays.fill(anchors, 0.0f);
            int count = fillUniformData(data, params, directions, anchors, now);
            Matrix4f viewProjectionMatrix = this.frameViewProjection.set(event.projectionMatrix())
                    .mul(event.modelViewMatrix());
            Matrix4f inverseViewProjectionMatrix = this.frameInverseViewProjection.set(viewProjectionMatrix).invert();

            shader.bind();
            bindTexture(0, this.sceneCopy.getColorAttachment());
            bindTexture(1, blurred.getColorAttachment());
            bindTexture(2, this.sceneCopy.getDepthAttachment());
            sourceTexture.uploadInt(0);
            blurTexture.uploadInt(1);
            depthTexture.uploadInt(2);
            inverseViewProjection.uploadMatrix4f(inverseViewProjectionMatrix);
            viewProjection.uploadMatrix4f(viewProjectionMatrix);
            viewMatrix.uploadMatrix4f(event.modelViewMatrix());
            resolution.uploadVec2(width, height);
            time.uploadFloat((float) ((now - this.clockOriginNanos) * 0.000000001d));
            intensity.uploadFloat(Math.max(0.0f, Math.min(2.0f, configuredIntensity)));
            screenShake.uploadFloat(shakeEnabled ? 1.0f : 0.0f);
            effectCount.uploadInt(count);
            effectData.uploadVec4Array(data);
            effectParams.uploadVec4Array(params);
            effectDirections.uploadVec4Array(directions);
            effectAnchors.uploadVec4Array(anchors);
            quad.draw();
            shader.unbind();
        } finally {
            state.restore();
        }
    }

    private int fillUniformData(float[] data, float[] params, float[] directions,
                                float[] anchors, long now) {
        Vec3d cameraPosition = Mc.INSTANCE.getCamera().getPos();
        int index = 0;
        for (EffectInstance effect : this.effects) {
            if (index >= MAX_EFFECTS) {
                break;
            }
            Vec3d relative = effect.position.subtract(cameraPosition);
            Vec3d relativeAnchor = effect.anchor.subtract(cameraPosition);
            float progress = Math.min(1.0f,
                    (now - effect.startedAtNanos) / (float) effect.lifetimeNanos);
            int offset = index * 4;
            data[offset] = (float) relative.x;
            data[offset + 1] = (float) relative.y;
            data[offset + 2] = (float) relative.z;
            data[offset + 3] = effect.type.ordinal();
            params[offset] = progress;
            params[offset + 1] = effect.strength;
            params[offset + 2] = effect.seed;
            params[offset + 3] = 0.0f;
            directions[offset] = (float) effect.direction.x;
            directions[offset + 1] = (float) effect.direction.y;
            directions[offset + 2] = (float) effect.direction.z;
            directions[offset + 3] = 0.0f;
            anchors[offset] = (float) relativeAnchor.x;
            anchors[offset + 1] = (float) relativeAnchor.y;
            anchors[offset + 2] = (float) relativeAnchor.z;
            anchors[offset + 3] = 0.0f;
            index++;
        }
        return index;
    }

    private void ensureInitialized(int width, int height) {
        if (shader == null) {
            blur.init();
            ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
            shader = new ShaderProgram(
                    resources.route("shaders/world_effects.fsh"),
                    resources.route("shaders/framebuffer.vsh")
            );
            sourceTexture = shader.uniform("uSource");
            blurTexture = shader.uniform("uBlur");
            depthTexture = shader.uniform("uDepth");
            inverseViewProjection = shader.uniform("uInverseViewProjection");
            viewProjection = shader.uniform("uViewProjection");
            viewMatrix = shader.uniform("uViewMatrix");
            resolution = shader.uniform("uResolution");
            time = shader.uniform("uTime");
            intensity = shader.uniform("uIntensity");
            screenShake = shader.uniform("uScreenShake");
            effectCount = shader.uniform("uEffectCount");
            effectData = shader.uniform("uEffects[0]");
            effectParams = shader.uniform("uParams[0]");
            effectDirections = shader.uniform("uDirections[0]");
            effectAnchors = shader.uniform("uAnchors[0]");
        }
        this.sceneCopy = FramebufferUtil.ensureFramebuffer(
                this.sceneCopy, width, height, () -> new SimpleFramebuffer(width, height, true));
        FramebufferUtil.resizeIfNeeded(this.sceneCopy, width, height);
    }

    private static void copyScene(Framebuffer source, Framebuffer destination,
                                  int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }

    private static void bindTexture(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    private void discardExpired(long now) {
        this.effects.removeIf(effect -> now - effect.startedAtNanos >= effect.lifetimeNanos);
        for (WorldEffectType type : WorldEffectType.values()) {
            if (activeCount(type) >= activeLimit(type)) {
                continue;
            }
            PendingEffect pending = this.pendingEffects.remove(type);
            if (pending != null) {
                this.effects.addLast(start(pending, now));
            }
        }
    }

    private long activeCount(WorldEffectType type) {
        return this.effects.stream().filter(effect -> effect.type == type).count();
    }

    private static int activeLimit(WorldEffectType type) {
        return switch (type) {
            case TNT_EXPLOSION -> 15;
            case ARROW_BLOOD -> 3;
            case MACE_SMASH -> 1;
        };
    }

    private static EffectInstance start(PendingEffect pending, long now) {
        return new EffectInstance(
                pending.type, pending.position, pending.direction, pending.anchor, now,
                pending.type.lifetimeMs() * 1_000_000L,
                pending.strength, pending.seed
        );
    }

    public void clear() {
        this.effects.clear();
        this.pendingEffects.clear();
    }

    public void release() {
        clear();
        if (this.sceneCopy != null) {
            this.sceneCopy.delete();
            this.sceneCopy = null;
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
        if (shader != null) {
            shader.release();
            shader = null;
        }
    }

    private record EffectInstance(WorldEffectType type, Vec3d position, Vec3d direction,
                                  Vec3d anchor,
                                  long startedAtNanos, long lifetimeNanos,
                                  float strength, float seed) {
    }

    private record PendingEffect(WorldEffectType type, Vec3d position, Vec3d direction,
                                 Vec3d anchor, float strength, float seed) {
    }

    private record SavedState(
            int activeTexture, int texture0, int texture1, int texture2, int program,
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
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            int texture2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
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
            return new SavedState(activeTexture, texture0, texture1, texture2,
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
                    GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING));
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
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2);
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
