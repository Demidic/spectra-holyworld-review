package ru.spectra.client.render;

import ru.spectra.client.module.ShaderHandModule;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import ru.spectra.client.type.HandShaderMode;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.FramebufferUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Lightweight first-person procedural material and contour smoke.
 *
 * The mask is built from the depth-buffer difference immediately before and
 * after the single vanilla held-item pass. Unlike a color difference, depth
 * is not affected by animated skies, translucent blocks or shader-pack color
 * grading, so world pixels cannot leak into the hand material.
 */
public final class HandShaderRenderer {
    private static final float[] WHITE = {1.0f, 1.0f, 1.0f};

    private final FullscreenQuad quad = new FullscreenQuad();
    private Framebuffer depthBefore;
    private Framebuffer depthAfter;
    private Framebuffer maskTarget;
    private Framebuffer trailA;
    private Framebuffer trailB;
    private Framebuffer blur1;
    private Framebuffer blur2;
    private Framebuffer coverageTarget;

    private Pass blit;
    private Pass maskDiff;
    private Pass maskCoverage;
    private Pass trail;
    private Pass kawaseDown;
    private Pass kawaseUp;
    private Pass composite;

    private boolean captureActive;
    private long lastMs;
    private float smoothDt = 1.0f / 60.0f;
    private float smoothActivity;
    private float slash;
    private float slashDir = 1.0f;
    private boolean wasSwinging;
    private float previousSwingProgress;
    private float previousYaw;
    private float previousPitch;
    private boolean hasPreviousCamera;
    private boolean smokeWasEnabled;
    private boolean deferredCompositePending;
    private int captureSourceFramebuffer;
    private int captureSourceWidth;
    private int captureSourceHeight;
    private int capturedWindowWidth;
    private int capturedWindowHeight;
    private SavedGlState savedGlState;
    private RenderPerformanceProfile performanceProfile;
    private final long startNanos = System.nanoTime();

    public void beginHandCapture() {
        Framebuffer source = MinecraftClient.getInstance().getFramebuffer();
        beginHandCapture(source.fbo, source.textureWidth, source.textureHeight);
    }

    public void beginBoundHandCapture() {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        beginHandCapture(
                GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                Math.max(1, viewport[2]),
                Math.max(1, viewport[3])
        );
    }

    private void beginHandCapture(int sourceFramebuffer, int sourceWidth, int sourceHeight) {
        if (!isWindowReady()) {
            return;
        }
        drainGlErrors();
        ensureInitialized();
        ensureTargets();
        SavedGlState stateBeforeCapture = SavedGlState.capture();
        try {
            copyDepth(sourceFramebuffer, sourceWidth, sourceHeight, depthBefore);
        } finally {
            stateBeforeCapture.restore();
        }
        throwOnGlError("capturing depth before the hand");
        captureSourceFramebuffer = sourceFramebuffer;
        captureSourceWidth = sourceWidth;
        captureSourceHeight = sourceHeight;
        captureActive = true;
    }

    public void finishHandCaptureAndRender(ShaderHandModule module) {
        finishHandCapture(module, false);
    }

    public void finishHandCaptureForDeferredRender() {
        finishHandCapture(null, true);
    }

    private void finishHandCapture(ShaderHandModule module, boolean deferComposite) {
        if (!captureActive || !isWindowReady()) {
            restoreSavedState();
            captureActive = false;
            return;
        }

        try {
            // Restore exactly the state left by the vanilla held-item pass,
            // not the earlier state used while taking the first snapshot.
            savedGlState = SavedGlState.capture();
            MinecraftClient client = MinecraftClient.getInstance();
            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();
            RenderPerformanceProfile profile = profile();
            int effectWidth = Math.max(
                    1,
                    width / profile.handEffectDivisor()
            );
            int effectHeight = Math.max(
                    1,
                    height / profile.handEffectDivisor()
            );
            int maskWidth = Math.max(1, width / profile.handMaskDivisor());
            int maskHeight = Math.max(1, height / profile.handMaskDivisor());

            copyDepth(
                    captureSourceFramebuffer,
                    captureSourceWidth,
                    captureSourceHeight,
                    depthAfter
            );

            renderTo(maskTarget, maskDiff, () -> {
                bindTexture(maskDiff, "Sampler0", 0, depthBefore.getDepthAttachment());
                bindTexture(maskDiff, "Sampler1", 1, depthAfter.getDepthAttachment());
                maskDiff.vec2("texSize", maskWidth, maskHeight);
            });

            if (deferComposite) {
                capturedWindowWidth = width;
                capturedWindowHeight = height;
                deferredCompositePending = true;
                return;
            }

            renderCapturedHand(module, width, height, profile);
            deferredCompositePending = false;
        } finally {
            restoreSavedState();
            captureActive = false;
        }
    }

    public boolean hasDeferredHandCapture() {
        return deferredCompositePending;
    }

    public boolean renderDeferredHandCapture(ShaderHandModule module) {
        if (!deferredCompositePending || !isWindowReady()) {
            deferredCompositePending = false;
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width != capturedWindowWidth || height != capturedWindowHeight) {
            deferredCompositePending = false;
            return false;
        }
        deferredCompositePending = false;
        try {
            savedGlState = SavedGlState.capture();
            renderCapturedHand(module, width, height, profile());
            return true;
        } finally {
            restoreSavedState();
        }
    }

    private void renderCapturedHand(ShaderHandModule module, int width, int height,
                                    RenderPerformanceProfile profile) {
        int effectWidth = Math.max(1, width / profile.handEffectDivisor());
        int effectHeight = Math.max(1, height / profile.handEffectDivisor());
        Dynamics dynamics = updateDynamics(width, height);
        float[] glow = module.glowColor();
        float[] smokeColor = module.smokeColor();
        boolean smokeEnabled = module.smokeEnabled.isValue();
        float smokeStrength = smokeEnabled
                ? module.smokeAmount.currentValue() * smokeColor[3]
                : 0.0f;

        renderTo(coverageTarget, maskCoverage, () ->
                bindTexture(maskCoverage, "Sampler0", 0, maskTarget.getColorAttachment()));

        int smokeTexture = trailA.getColorAttachment();
        if (smokeEnabled) {
            renderTo(trailB, trail, () -> {
                bindTexture(trail, "Sampler0", 0, trailA.getColorAttachment());
                bindTexture(trail, "Sampler1", 1, maskTarget.getColorAttachment());
                bindTexture(trail, "Sampler2", 2, coverageTarget.getColorAttachment());
                trail.vec2("texSize", effectWidth, effectHeight);
                trail.value("time", dynamics.time);
                trail.value("intensity", module.intensity.currentValue());
                trail.value("speed", module.speed.currentValue());
                trail.value("length", module.smokeLength.currentValue());
                trail.value("smoke", smokeStrength);
                trail.value("activity", dynamics.activity);
                trail.value("persistence", module.smokePersistence.currentValue());
                trail.value("slash", dynamics.slash);
                trail.value("slashDir", dynamics.slashDirection);
                trail.value("swingHand", dynamics.swingHand);
                trail.vec2("camShift", dynamics.cameraShiftX, dynamics.cameraShiftY);
                trail.vec4("glowColor", smokeColor[0], smokeColor[1], smokeColor[2], 1.0f);
            });

            smokeTexture = buildBlur(
                    trailB.getColorAttachment(),
                    0.45f + smokeStrength * 0.95f,
                    effectWidth, effectHeight
            );
        } else if (smokeWasEnabled) {
            FramebufferUtil.clearTransparent(trailA);
            FramebufferUtil.clearTransparent(trailB);
            FramebufferUtil.clearTransparent(blur1);
            FramebufferUtil.clearTransparent(blur2);
        }

        savedGlState.bindDrawTarget();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        composite.bind();
        bindTexture(composite, "Sampler0", 0, maskTarget.getColorAttachment());
        bindTexture(composite, "Sampler1", 1, smokeTexture);
        bindTexture(composite, "Sampler2", 2, coverageTarget.getColorAttachment());
        composite.integer("mode", module.mode.currentValue().ordinal());
        composite.vec2("texSize", effectWidth, effectHeight);
        composite.value("time", dynamics.time);
        composite.value("intensity", module.intensity.currentValue());
        composite.value("speed", module.speed.currentValue());
        composite.value("patternScale", module.patternScale.currentValue());
        composite.value("smokeAmount", smokeStrength);
        composite.value("activity", dynamics.activity);
        composite.vec2("motion", dynamics.cameraShiftX, dynamics.cameraShiftY);
        composite.vec3("glowColor", glow[0], glow[1], glow[2]);
        composite.vec3("smokeColor", smokeColor[0], smokeColor[1], smokeColor[2]);
        quad.draw();
        composite.unbind();
        throwOnGlError("rendering isolated hand effect");

        if (smokeEnabled) {
            Framebuffer swap = trailA;
            trailA = trailB;
            trailB = swap;
        }
        smokeWasEnabled = smokeEnabled;
    }

    public void abortHandCapture() {
        restoreSavedState();
        captureActive = false;
    }

    private Dynamics updateDynamics(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        float rawDt = lastMs > 0L ? (now - lastMs) / 1000.0f : 1.0f / 60.0f;
        lastMs = now;
        if (rawDt <= 0.0f || rawDt > 0.05f) rawDt = smoothDt;
        rawDt = Math.max(1.0f / 144.0f, Math.min(1.0f / 30.0f, rawDt));
        smoothDt += (rawDt - smoothDt) * 0.1f;
        float time = (System.nanoTime() - startNanos) / 1_000_000_000.0f;

        boolean swinging = client.player != null && client.player.handSwinging;
        float swingProgress = client.player != null ? client.player.handSwingProgress : 0.0f;
        boolean restarted = swinging
                && (swingProgress + 0.08f < previousSwingProgress || (!wasSwinging && swingProgress > 0.02f));
        if (restarted) {
            slash = 1.0f;
            slashDir = -slashDir;
        }
        wasSwinging = swinging;
        previousSwingProgress = swingProgress;
        if (swinging) {
            slash = Math.max(slash, 1.0f - Math.min(1.0f, swingProgress) * 0.72f);
        }
        slash = Math.max(0.0f, slash - smoothDt * 2.2f);

        float motion = client.player == null ? 0.0f
                : (float) Math.min(1.0, client.player.getVelocity().horizontalLength() * 2.2);
        float swingActivity = swinging && client.player != null ? 1.0f - Math.min(1.0f, swingProgress) : 0.0f;
        float useActivity = client.player != null && client.player.isUsingItem()
                ? 0.55f + Math.min(0.45f, client.player.getItemUseTime() * 0.06f) : 0.0f;
        float activityTarget = Math.max(Math.max(swingActivity, useActivity), motion);
        smoothActivity += (activityTarget - smoothActivity) * (1.0f - (float) Math.exp(-smoothDt * 6.0));

        float cameraShiftX = 0.0f;
        float cameraShiftY = 0.0f;
        if (client.gameRenderer != null && client.gameRenderer.getCamera() != null) {
            float yaw = client.gameRenderer.getCamera().getYaw();
            float pitch = client.gameRenderer.getCamera().getPitch();
            if (hasPreviousCamera) {
                float deltaYaw = wrapDegrees(yaw - previousYaw);
                float deltaPitch = pitch - previousPitch;
                float fov = (float) Math.toRadians(Math.max(1.0, client.options.getFov().getValue()));
                float tanHalf = (float) Math.tan(fov * 0.5f);
                float aspect = height > 0 ? (float) width / height : 1.0f;
                cameraShiftX = (float) Math.toRadians(deltaYaw) / (2.0f * tanHalf * aspect);
                cameraShiftY = -(float) Math.toRadians(deltaPitch) / (2.0f * tanHalf);
                cameraShiftX = Math.max(-0.25f, Math.min(0.25f, cameraShiftX));
                cameraShiftY = Math.max(-0.25f, Math.min(0.25f, cameraShiftY));
            }
            previousYaw = yaw;
            previousPitch = pitch;
            hasPreviousCamera = true;
        }

        float swingHand = 0.0f;
        if (client.player != null) {
            Hand active = swinging ? client.player.preferredHand
                    : (client.player.isUsingItem() ? client.player.getActiveHand() : Hand.MAIN_HAND);
            if (active == Hand.MAIN_HAND) {
                swingHand = client.player.getMainArm() == Arm.LEFT ? -1.0f : 1.0f;
            } else {
                swingHand = client.player.getMainArm() == Arm.LEFT ? 1.0f : -1.0f;
            }
        }
        return new Dynamics(time, smoothActivity, slash, slashDir, swingHand, cameraShiftX, cameraShiftY);
    }

    private int buildBlur(int sourceTexture, float radius, int width, int height) {
        ensureBlurTargets();
        float offset = Math.max(1.0f, 1.0f + radius);
        kawasePass(kawaseDown, blur1, sourceTexture, offset, width, height, null);
        kawasePass(kawaseUp, blur2, blur1.getColorAttachment(), offset, width, height, WHITE);
        return blur2.getColorAttachment();
    }

    private void kawasePass(Pass pass, Framebuffer target, int sourceTexture, float offset,
                            int width, int height, float[] color) {
        renderTo(target, pass, () -> {
            bindTexture(pass, "Sampler0", 0, sourceTexture);
            pass.vec2("uOffset", offset, offset);
            pass.vec2("uHalfPixel", 0.5f / width, 0.5f / height);
            pass.vec2("uSize", width, height);
            if (color != null) {
                pass.vec3("color", color[0], color[1], color[2]);
            }
        });
    }

    private void renderTo(Framebuffer target, Pass pass, Runnable uniforms) {
        target.beginWrite(true);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        pass.bind();
        uniforms.run();
        quad.draw();
        pass.unbind();
    }

    private void ensureInitialized() {
        if (maskDiff != null) {
            return;
        }
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        char[] sharedVertex = ShaderProgram.readUtf8(
                resources.route("shaders/hands/quad.vsh")
        );
        try {
            blit = new Pass(resources, "shaders/hands/blit.fsh");
            maskDiff = new Pass(resources, "shaders/hands/mask_diff.fsh");
            maskCoverage = new Pass(resources, "shaders/hands/mask_coverage.fsh");
            trail = new Pass(resources, "shaders/hands/hand_trail.fsh");
            kawaseDown = new Pass(resources, "shaders/hands/kawase_down.fsh");
            kawaseUp = new Pass(resources, "shaders/hands/kawase_up.fsh");
            composite = new Pass(resources, "shaders/hands/hand_composite.fsh");
            for (Pass pass : new Pass[]{
                    blit,
                    maskDiff,
                    maskCoverage,
                    trail,
                    kawaseDown,
                    kawaseUp,
                    composite
            }) {
                pass.initialize(sharedVertex);
            }
        } catch (RuntimeException error) {
            releasePasses();
            throw error;
        } finally {
            Arrays.fill(sharedVertex, '\0');
        }
    }

    private void releasePasses() {
        for (Pass pass : new Pass[]{
                blit,
                maskDiff,
                maskCoverage,
                trail,
                kawaseDown,
                kawaseUp,
                composite
        }) {
            if (pass != null) {
                pass.release();
            }
        }
        blit = null;
        maskDiff = null;
        maskCoverage = null;
        trail = null;
        kawaseDown = null;
        kawaseUp = null;
        composite = null;
    }

    private void ensureTargets() {
        int width = Mc.INSTANCE.getWindow().getFramebufferWidth();
        int height = Mc.INSTANCE.getWindow().getFramebufferHeight();
        RenderPerformanceProfile profile = profile();
        int effectWidth = Math.max(
                1,
                width / profile.handEffectDivisor()
        );
        int effectHeight = Math.max(
                1,
                height / profile.handEffectDivisor()
        );
        int maskWidth = Math.max(1, width / profile.handMaskDivisor());
        int maskHeight = Math.max(1, height / profile.handMaskDivisor());
        depthBefore = ensure(depthBefore, maskWidth, maskHeight, true);
        depthAfter = ensure(depthAfter, maskWidth, maskHeight, true);
        // The procedural smoke can safely run at half resolution, but the
        // material mask must match the framebuffer pixel-for-pixel. Scaling a
        // half-resolution mask was responsible for the stair-stepped outline
        // around held items and the player's arm.
        maskTarget = ensure(maskTarget, maskWidth, maskHeight);
        trailA = ensure(trailA, effectWidth, effectHeight);
        trailB = ensure(trailB, effectWidth, effectHeight);
        coverageTarget = ensure(coverageTarget, 1, 1);
    }

    private void ensureBlurTargets() {
        RenderPerformanceProfile profile = profile();
        int width = Math.max(
                1,
                Mc.INSTANCE.getWindow().getFramebufferWidth()
                        / profile.handEffectDivisor()
        );
        int height = Math.max(
                1,
                Mc.INSTANCE.getWindow().getFramebufferHeight()
                        / profile.handEffectDivisor()
        );
        blur1 = ensure(blur1, width, height);
        blur2 = ensure(blur2, width, height);
    }

    private Framebuffer ensure(Framebuffer framebuffer, int width, int height) {
        return ensure(framebuffer, width, height, false);
    }

    private Framebuffer ensure(Framebuffer framebuffer, int width, int height, boolean useDepth) {
        framebuffer = FramebufferUtil.ensureFramebuffer(framebuffer, width, height,
                () -> new SimpleFramebuffer(width, height, useDepth));
        FramebufferUtil.resizeIfNeeded(framebuffer, width, height);
        return framebuffer;
    }

    private void copyTexture(int sourceTexture, Framebuffer destination) {
        renderTo(destination, blit, () ->
                bindTexture(blit, "Sampler0", 0, sourceTexture));
    }

    private void copyDepth(int sourceFramebuffer, int sourceWidth, int sourceHeight,
                           Framebuffer destination) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.fbo);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL30.glBlitFramebuffer(
                0, 0, sourceWidth, sourceHeight,
                0, 0, destination.textureWidth, destination.textureHeight,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );
    }

    private void bindTexture(Pass pass, String sampler, int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        pass.integer(sampler, unit);
    }

    private void restoreSavedState() {
        if (savedGlState != null) {
            savedGlState.restore();
            savedGlState = null;
        }
    }

    private boolean isWindowReady() {
        return Mc.INSTANCE.getWindow().getFramebufferWidth() > 0
                && Mc.INSTANCE.getWindow().getFramebufferHeight() > 0;
    }

    private RenderPerformanceProfile profile() {
        if (performanceProfile == null) {
            performanceProfile = RenderPerformanceProfile.current();
        }
        return performanceProfile;
    }

    public void invalidateState() {
        restoreSavedState();
        depthBefore = delete(depthBefore);
        depthAfter = delete(depthAfter);
        maskTarget = delete(maskTarget);
        trailA = delete(trailA);
        trailB = delete(trailB);
        blur1 = delete(blur1);
        blur2 = delete(blur2);
        coverageTarget = delete(coverageTarget);
        captureActive = false;
        deferredCompositePending = false;
        captureSourceFramebuffer = 0;
        captureSourceWidth = 0;
        captureSourceHeight = 0;
        capturedWindowWidth = 0;
        capturedWindowHeight = 0;
        lastMs = 0L;
        smoothDt = 1.0f / 60.0f;
        smoothActivity = 0.0f;
        slash = 0.0f;
        slashDir = 1.0f;
        wasSwinging = false;
        previousSwingProgress = 0.0f;
        hasPreviousCamera = false;
        smokeWasEnabled = false;
    }

    private Framebuffer delete(Framebuffer framebuffer) {
        if (framebuffer != null) {
            framebuffer.delete();
        }
        return null;
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    private static void drainGlErrors() {
        for (int index = 0; index < 32 && GL11.glGetError() != GL11.GL_NO_ERROR; index++) {
            // Remove errors raised by an earlier renderer so Shader Hand can
            // attribute only errors produced inside its own isolated pass.
        }
    }

    private static void throwOnGlError(String stage) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException(
                    "Shader Hand OpenGL failure while " + stage
                            + " (0x" + Integer.toHexString(error) + ")"
            );
        }
    }

    private record Dynamics(float time, float activity, float slash, float slashDirection, float swingHand,
                            float cameraShiftX, float cameraShiftY) {
    }

    private record SavedGlState(int activeTexture, int[] textures, int currentProgram,
                                int readFramebuffer, int drawFramebuffer, int[] viewport,
                                boolean depthTest, boolean cull, boolean blend, boolean depthMask,
                                int depthFunction, int blendSrcRgb, int blendDstRgb,
                                int blendSrcAlpha, int blendDstAlpha,
                                int vertexArray, int arrayBuffer, int elementArrayBuffer,
                                boolean scissorTest, int[] scissorBox, boolean[] colorMask) {
        private static SavedGlState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] textures = new int[3];
            for (int unit = 0; unit < textures.length; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
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
            return new SavedGlState(
                    activeTexture,
                    textures,
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                    viewport,
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                    GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                    scissorBox,
                    colorMask
            );
        }

        private void bindDrawTarget() {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }

        private void restore() {
            bindDrawTarget();
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunction);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (scissorTest) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
            GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            for (int unit = 0; unit < textures.length; unit++) {
                // Hand passes bind textures through raw OpenGL and therefore deliberately do
                // not mutate RenderSystem's texture cache. Restore the actual GL bindings raw
                // as well; using setShaderTexture here may be skipped by that unchanged cache.
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[unit]);
            }
            GL13.glActiveTexture(activeTexture);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            // In the core OpenGL profile the element-array binding belongs to a VAO.
            // Binding it while VAO 0 is active raises GL_INVALID_OPERATION every frame
            // and was the source of both log spam and the ShaderHand stutter.
            if (vertexArray != 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
            }
            GL20.glUseProgram(currentProgram);
        }
    }

    private static final class Pass {
        private final ShaderProgram program;
        private final Map<String, ShaderUniform> uniforms = new HashMap<>();

        private Pass(ResourceRouter resources, String fragment) {
            program = new ShaderProgram(resources.route(fragment), resources.route("shaders/hands/quad.vsh"));
        }

        private void initialize(char[] sharedVertex) {
            program.initializeWithVertexSource(
                    CharBuffer.wrap(sharedVertex).asReadOnlyBuffer()
            );
        }

        private void release() {
            program.release();
        }

        private void bind() {
            program.bind();
        }

        private void unbind() {
            program.unbind();
        }

        private ShaderUniform uniform(String name) {
            return uniforms.computeIfAbsent(name, program::uniform);
        }

        private void integer(String name, int value) {
            uniform(name).uploadInt(value);
        }

        private void value(String name, float value) {
            uniform(name).uploadFloat(value);
        }

        private void vec2(String name, float x, float y) {
            uniform(name).uploadVec2(x, y);
        }

        private void vec3(String name, float x, float y, float z) {
            uniform(name).uploadVec3(x, y, z);
        }

        private void vec4(String name, float x, float y, float z, float w) {
            uniform(name).uploadVec4(x, y, z, w);
        }
    }
}
