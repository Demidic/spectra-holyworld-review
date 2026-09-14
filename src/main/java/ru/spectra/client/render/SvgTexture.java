package ru.spectra.client.render;
import ru.spectra.client.resource.ResourceSource;
import ru.spectra.client.util.StencilBufferUtil;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import lombok.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public class SvgTexture {
    public static final SVGUniverse svgUniverse = new SVGUniverse();

    @NonNull
    public final ResourceSource resource;
    public final int width;
    public final int height;
    public int magFilter = GL11.GL_LINEAR;
    public int minFilter = GL11.GL_LINEAR_MIPMAP_LINEAR;
    public int textureId = 0;

    public int id() {
        if (this.textureId == 0) {
            try {
                this.textureId = uploadTexture();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this.textureId;
    }

    public int uploadTexture() throws IOException {
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalStateException("SVG texture dimensions must be positive");
        }
        BufferedImage image = SvgRasterizer.rasterize(
                this.resource, loadDiagram(this.resource), this.width, this.height
        );
        int[] data = image.getRGB(0, 0, this.width, this.height, null, 0, this.width);
        int byteCount = Math.multiplyExact(Math.multiplyExact(this.width, this.height), 4);
        ByteBuffer byteBufferMemAlloc = MemoryUtil.memAlloc(byteCount);
        try {
            for (int i : data) {
                byteBufferMemAlloc.put((byte) ((i >> 16) & StencilBufferUtil.STENCIL_MASK));
                byteBufferMemAlloc.put((byte) ((i >> 8) & StencilBufferUtil.STENCIL_MASK));
                byteBufferMemAlloc.put((byte) (i & StencilBufferUtil.STENCIL_MASK));
                byteBufferMemAlloc.put((byte) ((i >> 24) & StencilBufferUtil.STENCIL_MASK));
            }
            byteBufferMemAlloc.flip();
            return uploadRgba(byteBufferMemAlloc);
        } finally {
            MemoryUtil.memFree(byteBufferMemAlloc);
        }
    }

    private int uploadRgba(ByteBuffer pixels) {
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int previousSwapBytes = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES);
        int previousLsbFirst = GL11.glGetInteger(GL11.GL_UNPACK_LSB_FIRST);
        int previousRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int previousSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int previousSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int previousImageHeight = GL11.glGetInteger(GL21.GL_UNPACK_IMAGE_HEIGHT);
        int previousSkipImages = GL11.glGetInteger(GL21.GL_UNPACK_SKIP_IMAGES);
        int texture = 0;
        boolean uploaded = false;
        try {
            // A PBO or non-zero row length left by Minecraft/another renderer makes
            // glTexImage2D interpret this direct buffer with the wrong stride. On
            // NVIDIA that can become a native access violation instead of a Java
            // exception, so define the complete unpack state for this upload.
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL21.GL_UNPACK_IMAGE_HEIGHT, 0);
            GL11.glPixelStorei(GL21.GL_UNPACK_SKIP_IMAGES, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, this.minFilter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, this.magFilter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                    this.width, this.height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels
            );
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            uploaded = true;
            return texture;
        } finally {
            if (!uploaded && texture != 0) {
                GL11.glDeleteTextures(texture);
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, previousSwapBytes);
            GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, previousLsbFirst);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, previousRowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, previousSkipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, previousSkipPixels);
            GL11.glPixelStorei(GL21.GL_UNPACK_IMAGE_HEIGHT, previousImageHeight);
            GL11.glPixelStorei(GL21.GL_UNPACK_SKIP_IMAGES, previousSkipImages);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, previousUnpackBuffer);
        }
    }

    public static SVGDiagram loadDiagram(ResourceSource class178Var) {
        SVGDiagram diagram;
        try {
            InputStream inputStreamStream = class178Var.stream();
            try {
                if (inputStreamStream == null) {
                    throw new IOException("Stream is null");
                }
                synchronized (svgUniverse) {
                    diagram = svgUniverse.getDiagram(svgUniverse.loadSVG(inputStreamStream, class178Var.toString()));
                    if (diagram == null) {
                        throw new IOException("Diagram is null");
                    }
                    diagram.setIgnoringClipHeuristic(true);
                }
                if (inputStreamStream != null) {
                    inputStreamStream.close();
                }
                return diagram;
            } catch (Throwable th) {
                if (inputStreamStream != null) {
                    try {
                        inputStreamStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void dispose() {
        if (this.textureId != 0) {
            GL11.glDeleteTextures(this.textureId);
            this.textureId = 0;
        }
    }

    public SvgTexture(@NonNull ResourceSource class178Var, int i, int i2) {
        if (class178Var == null) {
            throw new NullPointerException("resource is marked non-null but is null");
        }
        this.resource = class178Var;
        this.width = i;
        this.height = i2;
    }

    public void setMagFilter(int i) {
        this.magFilter = i;
    }

    public void setMinFilter(int i) {
        this.minFilter = i;
    }
}
