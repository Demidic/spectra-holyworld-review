package ru.spectra.client.render;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public class FullscreenQuad {
    public Integer vaoId;
    private Integer vertexBufferId;

    public void draw() {
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        try {
            if (this.vaoId == null) {
                int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
                try {
                    getOrCreateVao();
                } finally {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
                }
            }
            // Binding an existing VAO does not change GL_ARRAY_BUFFER_BINDING.
            GL30.glBindVertexArray(this.vaoId);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        } finally {
            GL30.glBindVertexArray(previousVao);
        }
    }

    public int getOrCreateVao() {
        if (this.vaoId == null) {
            this.vaoId = GL30.glGenVertexArrays();
            this.vertexBufferId = GL15.glGenBuffers();
            GL30.glBindVertexArray(this.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0L);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8L);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer vertices = stack.mallocFloat(24);
                // Two triangles. Keeping the quad non-indexed avoids touching
                // GL_ELEMENT_ARRAY_BUFFER while Minecraft has VAO 0 active.
                vertices
                        .put(-1.0f).put(-1.0f).put(0.0f).put(0.0f)
                        .put(1.0f).put(-1.0f).put(1.0f).put(0.0f)
                        .put(1.0f).put(1.0f).put(1.0f).put(1.0f)
                        .put(-1.0f).put(-1.0f).put(0.0f).put(0.0f)
                        .put(1.0f).put(1.0f).put(1.0f).put(1.0f)
                        .put(-1.0f).put(1.0f).put(0.0f).put(1.0f)
                        .flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            }
        }
        return this.vaoId;
    }
}
