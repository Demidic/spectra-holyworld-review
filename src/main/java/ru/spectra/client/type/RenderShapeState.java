package ru.spectra.client.type;

import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

import java.util.Objects;

public final class RenderShapeState {
    private boolean additiveBlend;
    private boolean blend;
    private boolean cull;
    private boolean depthTest;
    private VertexFormat format;
    private float lineWidth;
    private VertexFormat.DrawMode mode;
    private ShaderProgramKey shader;
    private Identifier texture;
    private int textureId = -1;
    private boolean textureEnabled;

    public RenderShapeState set(
            boolean additiveBlend,
            boolean blend,
            boolean cull,
            boolean depthTest,
            VertexFormat format,
            float lineWidth,
            VertexFormat.DrawMode mode,
            ShaderProgramKey shader,
            Identifier texture,
            boolean textureEnabled
    ) {
        this.additiveBlend = additiveBlend;
        this.blend = blend;
        this.cull = cull;
        this.depthTest = depthTest;
        this.format = Objects.requireNonNull(format, "format");
        this.lineWidth = lineWidth;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.shader = Objects.requireNonNull(shader, "shader");
        this.texture = texture;
        this.textureId = -1;
        this.textureEnabled = textureEnabled;
        return this;
    }

    public RenderShapeState set(
            boolean additiveBlend,
            boolean blend,
            boolean cull,
            boolean depthTest,
            VertexFormat format,
            float lineWidth,
            VertexFormat.DrawMode mode,
            ShaderProgramKey shader,
            int textureId,
            boolean textureEnabled
    ) {
        this.additiveBlend = additiveBlend;
        this.blend = blend;
        this.cull = cull;
        this.depthTest = depthTest;
        this.format = Objects.requireNonNull(format, "format");
        this.lineWidth = lineWidth;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.shader = Objects.requireNonNull(shader, "shader");
        this.texture = null;
        this.textureId = textureId;
        this.textureEnabled = textureEnabled;
        return this;
    }

    public boolean additiveBlend() {
        return this.additiveBlend;
    }

    public boolean blend() {
        return this.blend;
    }

    public boolean cull() {
        return this.cull;
    }

    public boolean depthTest() {
        return this.depthTest;
    }

    public VertexFormat format() {
        return this.format;
    }

    public float lineWidth() {
        return this.lineWidth;
    }

    public VertexFormat.DrawMode mode() {
        return this.mode;
    }

    public ShaderProgramKey shader() {
        return this.shader;
    }

    public Identifier texture() {
        return this.texture;
    }

    public int textureId() {
        return this.textureId;
    }

    public boolean textureEnabled() {
        return this.textureEnabled;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RenderShapeState other)) {
            return false;
        }
        return this.additiveBlend == other.additiveBlend
                && this.blend == other.blend
                && this.cull == other.cull
                && this.depthTest == other.depthTest
                && Float.compare(this.lineWidth, other.lineWidth) == 0
                && this.textureId == other.textureId
                && this.textureEnabled == other.textureEnabled
                && this.format == other.format
                && this.mode == other.mode
                && this.shader == other.shader
                && Objects.equals(this.texture, other.texture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.additiveBlend, this.blend, this.cull, this.depthTest,
                this.format, this.lineWidth, this.mode, this.shader, this.texture,
                this.textureId, this.textureEnabled);
    }
}
