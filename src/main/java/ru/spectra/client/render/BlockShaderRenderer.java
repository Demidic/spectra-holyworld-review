package ru.spectra.client.render;

import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import ru.spectra.client.type.ShaderEffectMode;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Small reusable renderer for the procedural fill used by Block Outline.
 * Geometry is uploaded only for the currently selected block (36 vertices).
 */
public final class BlockShaderRenderer {
    private ShaderProgram shader;
    private ShaderUniform mvpUniform;
    private ShaderUniform resolutionUniform;
    private ShaderUniform timeUniform;
    private ShaderUniform tintUniform;
    private ShaderUniform modeUniform;
    private int vao;
    private int vbo;

    public void render(Box cameraRelativeBox, Matrix4f modelView, Matrix4f projection, int color,
                       float alpha, float time, ShaderEffectMode mode, boolean throughWalls) {
        ensureInitialized();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int previousBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int previousBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int previousBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int previousBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            float[] vertices = buildVertices(cameraRelativeBox);
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_DYNAMIC_DRAW);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            if (throughWalls) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthMask(false);

            shader.bind();
            mvpUniform.uploadMatrix4f(new Matrix4f(projection).mul(modelView));
            resolutionUniform.uploadVec2(MinecraftClient.getInstance().getWindow().getFramebufferWidth(),
                    MinecraftClient.getInstance().getWindow().getFramebufferHeight());
            timeUniform.uploadFloat(time);
            tintUniform.uploadVec4(((color >> 16) & 0xFF) / 255.0f, ((color >> 8) & 0xFF) / 255.0f,
                    (color & 0xFF) / 255.0f, alpha);
            modeUniform.uploadInt(Math.max(0, mode.ordinal() - 1));
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);
        } finally {
            shader.unbind();
            GL20.glUseProgram(previousProgram);
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousElementBuffer);
            GL14.glBlendFuncSeparate(previousBlendSrcRgb, previousBlendDstRgb,
                    previousBlendSrcAlpha, previousBlendDstAlpha);
            RenderSystem.depthMask(depthWriteEnabled);
            if (blendEnabled) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cullEnabled) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (depthEnabled) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void ensureInitialized() {
        if (shader == null) {
            ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
            shader = new ShaderProgram(resources.route("shaders/block_effect.fsh"),
                    resources.route("shaders/block_effect.vsh"));
            mvpUniform = shader.uniform("u_modelViewProjection");
            resolutionUniform = shader.uniform("u_resolution");
            timeUniform = shader.uniform("u_time");
            tintUniform = shader.uniform("u_tint");
            modeUniform = shader.uniform("u_mode");
        }
        if (vao != 0) {
            return;
        }
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 24, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 24, 12L);
        GL30.glBindVertexArray(0);
    }

    private static float[] buildVertices(Box box) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        float[] output = new float[36 * 6];
        int offset = 0;
        offset = face(output, offset, x0, y0, z0, 0, 0, 0, x0, y1, z0, 0, 1, 0,
                x1, y1, z0, 1, 1, 0, x1, y0, z0, 1, 0, 0);
        offset = face(output, offset, x1, y0, z1, 0, 0, 1, x1, y1, z1, 0, 1, 1,
                x0, y1, z1, 1, 1, 1, x0, y0, z1, 1, 0, 1);
        offset = face(output, offset, x0, y0, z1, 0, 0, 0, x0, y1, z1, 0, 1, 0,
                x0, y1, z0, 1, 1, 0, x0, y0, z0, 1, 0, 0);
        offset = face(output, offset, x1, y0, z0, 0, 0, 1, x1, y1, z0, 0, 1, 1,
                x1, y1, z1, 1, 1, 1, x1, y0, z1, 1, 0, 1);
        offset = face(output, offset, x0, y1, z0, 0, 0, 0, x0, y1, z1, 0, 1, 0,
                x1, y1, z1, 1, 1, 0, x1, y1, z0, 1, 0, 0);
        face(output, offset, x0, y0, z1, 0, 0, 1, x0, y0, z0, 0, 1, 1,
                x1, y0, z0, 1, 1, 1, x1, y0, z1, 1, 0, 1);
        return output;
    }

    private static int face(float[] out, int offset,
                            float ax, float ay, float az, float au, float av, float aw,
                            float bx, float by, float bz, float bu, float bv, float bw,
                            float cx, float cy, float cz, float cu, float cv, float cw,
                            float dx, float dy, float dz, float du, float dv, float dw) {
        offset = vertex(out, offset, ax, ay, az, au, av, aw);
        offset = vertex(out, offset, bx, by, bz, bu, bv, bw);
        offset = vertex(out, offset, cx, cy, cz, cu, cv, cw);
        offset = vertex(out, offset, ax, ay, az, au, av, aw);
        offset = vertex(out, offset, cx, cy, cz, cu, cv, cw);
        return vertex(out, offset, dx, dy, dz, du, dv, dw);
    }

    private static int vertex(float[] out, int offset, float x, float y, float z,
                              float surfaceX, float surfaceY, float surfaceZ) {
        out[offset++] = x;
        out[offset++] = y;
        out[offset++] = z;
        out[offset++] = surfaceX;
        out[offset++] = surfaceY;
        out[offset++] = surfaceZ;
        return offset;
    }
}
