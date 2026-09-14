package ru.spectra.client.render;

import ru.spectra.client.Spectra;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import ru.spectra.client.type.SkyShaderMode;
import ru.spectra.client.util.FramebufferUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

/**
 * Real camera-space sky dome used by the original Spectra CustomSky.
 * Unlike a screen quad, every fragment receives a direction on a radius-140 sphere.
 */
public final class SkyEffectRenderer {
    private static final float SKY_RADIUS = 140.0f;
    private static final int AZIMUTH_SEGMENTS = 128;
    private static final int BANDS = 48;
    private static final int FLOATS_PER_VERTEX = 5;
    private static final int VERTEX_COUNT = AZIMUTH_SEGMENTS * BANDS * 6;

    private final Map<SkyShaderMode, SkyPass> shaders = new EnumMap<>(SkyShaderMode.class);
    private final Set<SkyShaderMode> incompatibleShaders = EnumSet.noneOf(SkyShaderMode.class);
    private final FullscreenQuad cacheQuad = new FullscreenQuad();
    private final Framebuffer[] cacheTargets = new Framebuffer[3];
    private Integer vertexArray;
    private Integer vertexBuffer;
    private DisplayPass displayPass;
    private RenderPerformanceProfile performanceProfile;
    private int previousCacheIndex;
    private int currentCacheIndex;
    private int buildingCacheIndex = 1;
    private int cacheTile;
    private CacheKey cacheKey;
    private Snapshot buildingSnapshot;

    public void render(SkyShaderMode mode, int baseColor, float alpha, float time, float skyAngle,
                       float starBrightness, float rainStrength, float patternScale) {
        if (incompatibleShaders.contains(mode)) {
            return;
        }
        SkyGlState state = SkyGlState.capture();
        try {
            SkyPass pass = shaders.computeIfAbsent(mode, this::createPass);
            ensureDome();
            ensureCacheTargets();
            ensureDisplayPass();

            Snapshot snapshot = new Snapshot(baseColor, alpha, time, skyAngle,
                    starBrightness, rainStrength, patternScale);
            CacheKey newKey = new CacheKey(mode, baseColor, alpha, patternScale);
            if (!newKey.equals(cacheKey)
                    || buildingSnapshot == null
                    || time + 0.01f < buildingSnapshot.time) {
                initializeCache(pass, newKey, snapshot);
            } else {
                updateCacheTile(pass, snapshot);
            }
            renderCachedDome(
                    alpha,
                    cacheTile / (float) profile().skyCacheTiles()
            );
        } catch (RuntimeException error) {
            if (incompatibleShaders.add(mode)) {
                Spectra.LOGGER.error(
                        "Custom sky shader {} is incompatible with this OpenGL driver and was disabled",
                        mode,
                        error
                );
            }
        } finally {
            state.restore();
        }
    }

    private SkyPass createPass(SkyShaderMode mode) {
        String fragment = switch (mode) {
            case NORTHERN_LIGHTS -> "shaders/sky/ethereal_flux.fsh";
            case COSMIC_NIGHT -> "shaders/sky/cosmic_night.fsh";
            case STORM_VEIL -> "shaders/sky/storm_veil.fsh";
            case ASTRAL_RIFT -> "shaders/sky/astral_rift.fsh";
        };
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        ShaderProgram program = new ShaderProgram(
                resources.route(fragment),
                resources.route("shaders/sky/sky_cache.vsh")
        );
        return new SkyPass(program, program.uniform("time"), program.uniform("alpha"), program.uniform("skyAngle"),
                program.uniform("starBrightness"), program.uniform("rainStrength"), program.uniform("baseColor"),
                program.uniform("patternScale"));
    }

    private void ensureCacheTargets() {
        RenderPerformanceProfile profile = profile();
        for (int index = 0; index < cacheTargets.length; index++) {
            Framebuffer target = cacheTargets[index];
            if (target != null) {
                continue;
            }
            target = new SimpleFramebuffer(
                    profile.skyCacheWidth(),
                    profile.skyCacheHeight(),
                    false
            );
            cacheTargets[index] = target;
            FramebufferUtil.setLinearTextureFilter(target);

            int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.getColorAttachment());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL13.glActiveTexture(previousActiveTexture);
        }
    }

    private void ensureDisplayPass() {
        if (displayPass != null) {
            return;
        }
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        ShaderProgram program = new ShaderProgram(
                resources.route("shaders/sky/sky_cached.fsh"),
                resources.route("shaders/sky/dome.vsh")
        );
        displayPass = new DisplayPass(
                program,
                program.uniform("u_modelView"),
                program.uniform("u_projection"),
                program.uniform("SkyTexturePrevious"),
                program.uniform("SkyTextureCurrent"),
                program.uniform("u_blend"),
                program.uniform("u_opacity")
        );
    }

    private void initializeCache(SkyPass pass, CacheKey newKey, Snapshot snapshot) {
        RenderPerformanceProfile profile = profile();
        previousCacheIndex = 0;
        currentCacheIndex = 0;
        buildingCacheIndex = 1;
        cacheTile = 0;
        cacheKey = newKey;
        buildingSnapshot = snapshot;
        clearCache(cacheTargets[currentCacheIndex], profile);
        renderDirectionalCache(
                pass,
                cacheTargets[currentCacheIndex],
                snapshot,
                0,
                profile.skyCacheWidth(),
                profile
        );
        clearCache(cacheTargets[buildingCacheIndex], profile);
    }

    private void updateCacheTile(SkyPass pass, Snapshot latestSnapshot) {
        RenderPerformanceProfile profile = profile();
        if (cacheTile == 0) {
            buildingSnapshot = latestSnapshot;
            clearCache(cacheTargets[buildingCacheIndex], profile);
        }

        int tileStart = profile.skyCacheWidth()
                * cacheTile / profile.skyCacheTiles();
        int tileEnd = profile.skyCacheWidth()
                * (cacheTile + 1) / profile.skyCacheTiles();
        renderDirectionalCache(
                pass,
                cacheTargets[buildingCacheIndex],
                buildingSnapshot,
                tileStart,
                tileEnd - tileStart,
                profile
        );
        cacheTile++;

        if (cacheTile >= profile.skyCacheTiles()) {
            previousCacheIndex = currentCacheIndex;
            currentCacheIndex = buildingCacheIndex;
            for (int index = 0; index < cacheTargets.length; index++) {
                if (index != previousCacheIndex && index != currentCacheIndex) {
                    buildingCacheIndex = index;
                    break;
                }
            }
            cacheTile = 0;
        }
    }

    private static void clearCache(
            Framebuffer target,
            RenderPerformanceProfile profile
    ) {
        target.beginWrite(true);
        RenderSystem.viewport(
                0,
                0,
                profile.skyCacheWidth(),
                profile.skyCacheHeight()
        );
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    private void renderDirectionalCache(SkyPass pass, Framebuffer target, Snapshot snapshot,
                                        int tileStart, int tileWidth,
                                        RenderPerformanceProfile profile) {
        target.beginWrite(true);
        RenderSystem.viewport(
                0,
                0,
                profile.skyCacheWidth(),
                profile.skyCacheHeight()
        );
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                tileStart,
                0,
                tileWidth,
                profile.skyCacheHeight()
        );

        pass.program.bind();
        pass.time.uploadFloat(snapshot.time);
        pass.alpha.uploadFloat(snapshot.alpha);
        pass.skyAngle.uploadFloat(snapshot.skyAngle);
        pass.starBrightness.uploadFloat(snapshot.starBrightness);
        pass.rainStrength.uploadFloat(snapshot.rainStrength);
        pass.patternScale.uploadFloat(snapshot.patternScale);
        pass.baseColor.uploadVec4(
                ((snapshot.baseColor >> 16) & 0xFF) / 255.0f,
                ((snapshot.baseColor >> 8) & 0xFF) / 255.0f,
                (snapshot.baseColor & 0xFF) / 255.0f,
                1.0f
        );
        cacheQuad.draw();
        pass.program.unbind();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderCachedDome(float opacity, float blend) {
        SkyGlState.bindDrawTarget();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        // The sky sphere is intentionally larger than the camera frustum. Depth
        // clamping prevents its far half from being cut into a visible band when
        // render distance or a shader pack supplies a shorter far plane.
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);

        displayPass.program.bind();
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
                .rotateY((float) Math.toRadians(-90.0));
        displayPass.modelView.uploadMatrix4f(modelView);
        displayPass.projection.uploadMatrix4f(RenderSystem.getProjectionMatrix());
        displayPass.opacity.uploadFloat(opacity);
        displayPass.blend.uploadFloat(blend * blend * (3.0f - 2.0f * blend));
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, cacheTargets[previousCacheIndex].getColorAttachment());
        displayPass.previousTexture.uploadInt(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, cacheTargets[currentCacheIndex].getColorAttachment());
        displayPass.currentTexture.uploadInt(1);

        GL30.glBindVertexArray(vertexArray);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, VERTEX_COUNT);
        displayPass.program.unbind();
    }

    private RenderPerformanceProfile profile() {
        if (performanceProfile == null) {
            performanceProfile = RenderPerformanceProfile.current();
        }
        return performanceProfile;
    }

    private void ensureDome() {
        if (vertexArray != null) {
            return;
        }
        int previousVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        vertexArray = GL30.glGenVertexArrays();
        vertexBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);

        FloatBuffer vertices = MemoryUtil.memAllocFloat(VERTEX_COUNT * FLOATS_PER_VERTEX);
        for (int band = 0; band < BANDS; band++) {
            float v0 = band / (float) BANDS;
            float v1 = (band + 1) / (float) BANDS;
            float elevation0 = lerp(v0, -(float) Math.PI / 2.0f, (float) Math.PI / 2.0f);
            float elevation1 = lerp(v1, -(float) Math.PI / 2.0f, (float) Math.PI / 2.0f);
            for (int segment = 0; segment < AZIMUTH_SEGMENTS; segment++) {
                float u0 = segment / (float) AZIMUTH_SEGMENTS;
                float u1 = (segment + 1) / (float) AZIMUTH_SEGMENTS;
                double azimuth0 = u0 * Math.PI * 2.0;
                double azimuth1 = u1 * Math.PI * 2.0;

                putVertex(vertices, azimuth0, elevation0, u0, v0);
                putVertex(vertices, azimuth1, elevation0, u1, v0);
                putVertex(vertices, azimuth1, elevation1, u1, v1);
                putVertex(vertices, azimuth0, elevation0, u0, v0);
                putVertex(vertices, azimuth1, elevation1, u1, v1);
                putVertex(vertices, azimuth0, elevation1, u0, v1);
            }
        }
        vertices.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(vertices);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL30.glBindVertexArray(previousVertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
    }

    private static void putVertex(FloatBuffer output, double azimuth, float elevation, float u, float v) {
        double cosElevation = Math.cos(elevation);
        output.put((float) (Math.cos(azimuth) * cosElevation * SKY_RADIUS));
        output.put((float) (Math.sin(elevation) * SKY_RADIUS));
        output.put((float) (Math.sin(azimuth) * cosElevation * SKY_RADIUS));
        output.put(u);
        output.put(v);
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    private record SkyPass(ShaderProgram program, ShaderUniform time, ShaderUniform alpha, ShaderUniform skyAngle,
                           ShaderUniform starBrightness, ShaderUniform rainStrength, ShaderUniform baseColor,
                           ShaderUniform patternScale) {
    }

    private record DisplayPass(ShaderProgram program, ShaderUniform modelView, ShaderUniform projection,
                               ShaderUniform previousTexture, ShaderUniform currentTexture,
                               ShaderUniform blend, ShaderUniform opacity) {
    }

    private record CacheKey(SkyShaderMode mode, int baseColor, float alpha, float patternScale) {
    }

    private record Snapshot(int baseColor, float alpha, float time, float skyAngle,
                            float starBrightness, float rainStrength, float patternScale) {
    }

    private record SkyGlState(
            int readFramebuffer,
            int drawFramebuffer,
            int[] viewport,
            boolean blend,
            boolean cull,
            boolean depthTest,
            boolean depthMask,
            int blendSrcRgb,
            int blendDstRgb,
            int blendSrcAlpha,
            int blendDstAlpha,
            int currentProgram,
            int vertexArray,
            int arrayBuffer,
            int activeTexture,
            int texture0,
            int texture1,
            boolean framebufferSrgb,
            boolean depthClamp,
            boolean scissorTest,
            int[] scissorBox,
            boolean[] colorMask
    ) {
        private static final ThreadLocal<SkyGlState> CURRENT = new ThreadLocal<>();

        private static SkyGlState capture() {
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            boolean[] colorMask = new boolean[4];
            var colorMaskBuffer = BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);
            for (int index = 0; index < colorMask.length; index++) {
                colorMask[index] = colorMaskBuffer.get(index) != 0;
            }
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            int texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(activeTexture);
            int[] scissorBox = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            SkyGlState state = new SkyGlState(
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                    viewport,
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                    activeTexture,
                    texture0,
                    texture1,
                    GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB),
                    GL11.glIsEnabled(GL32.GL_DEPTH_CLAMP),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                    scissorBox,
                    colorMask
            );
            CURRENT.set(state);
            return state;
        }

        private static void bindDrawTarget() {
            SkyGlState state = CURRENT.get();
            if (state == null) {
                return;
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, state.readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, state.drawFramebuffer);
            RenderSystem.viewport(
                    state.viewport[0], state.viewport[1],
                    state.viewport[2], state.viewport[3]
            );
            if (state.framebufferSrgb) {
                GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
            } else {
                GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            }
        }

        private void restore() {
            bindDrawTarget();
            RenderSystem.depthMask(depthMask);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (depthClamp) GL11.glEnable(GL32.GL_DEPTH_CLAMP); else GL11.glDisable(GL32.GL_DEPTH_CLAMP);
            if (scissorTest) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture1);
            GL13.glActiveTexture(activeTexture);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GL20.glUseProgram(currentProgram);
            CURRENT.remove();
        }
    }
}
