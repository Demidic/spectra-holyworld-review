package ru.spectra.client.render;
import ru.spectra.client.resource.Reloadable;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceSource;
import ru.spectra.client.util.StencilBufferUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL33;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class GlTexture implements Reloadable {
    private static final Map<SharedTextureKey, SharedTexture> SHARED_CLASSPATH_TEXTURES =
            new ConcurrentHashMap<>();
    private final ResourceSource source;
    private final String sharedResourcePath;
    public ByteBuffer imageBuffer;
    public Integer textureId;
    public int width;
    public int height;
    public int magFilterMode;
    public int minFilterMode;
    public int wrapModeX;
    public int wrapModeY;
    public boolean mipmap;

    public GlTexture(ResourceSource class178Var) {
        this(Objects.requireNonNull(class178Var), null);
    }

    @Override
    public void reload() {
        free();
    }

    public void bindToSlot(int i) {
        GL33.glActiveTexture(33984 + i);
        GL33.glBindTexture(3553, textureWithSTB());
    }

    public void free() {
        if (this.sharedResourcePath != null) {
            ByteBuffer current = this.imageBuffer;
            if (current != null) {
                discardProtectedSourceBuffer(current);
            }
            this.textureId = null;
            return;
        }
        if (this.textureId != null) {
            GL33.glDeleteTextures(this.textureId.intValue());
            this.textureId = null;
        }
    }

    public int textureWithSTB() {
        if (this.sharedResourcePath != null) {
            return sharedTextureWithSTB();
        }
        if (this.textureId != null) {
            return this.textureId.intValue();
        }
        return uploadSourceTexture();
    }

    private int uploadSourceTexture() {
        ByteBuffer encoded = ensureImageBuffer();
        try {
            if (isWebp(encoded)) {
                Integer value = Integer.valueOf(loadWithImageIo(encoded));
                this.textureId = value;
                return value.intValue();
            }
            Integer value = Integer.valueOf(loadWithStb(encoded));
            this.textureId = value;
            return value.intValue();
        } finally {
            discardProtectedSourceBuffer(encoded);
        }
    }

    public int texture() {
        if (this.textureId != null) {
            return this.textureId.intValue();
        }
        Integer numValueOf = Integer.valueOf(buildTexture());
        this.textureId = numValueOf;
        return numValueOf.intValue();
    }

    public GlTexture setDimensions(int i, int i2) {
        this.width = i;
        this.height = i2;
        return this;
    }

    public int buildTexture() {
        if (this.textureId != null) {
            return this.textureId.intValue();
        }
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalStateException("Width and Height must be set before building the texture without stb_image.");
        }
        ByteBuffer pixels = ensureImageBuffer();
        try {
            int iGlGenTextures = uploadTexture(pixels, this.width, this.height);
            this.textureId = Integer.valueOf(iGlGenTextures);
            return iGlGenTextures;
        } finally {
            discardProtectedSourceBuffer(pixels);
        }
    }

    private int sharedTextureWithSTB() {
        SharedTextureKey key = new SharedTextureKey(
                this.sharedResourcePath,
                this.magFilterMode,
                this.minFilterMode,
                this.wrapModeX,
                this.wrapModeY,
                this.mipmap
        );
        SharedTexture shared = SHARED_CLASSPATH_TEXTURES.computeIfAbsent(
                key,
                ignored -> new SharedTexture()
        );
        synchronized (shared) {
            if (shared.textureId == null) {
                int requestedWidth = this.width;
                int requestedHeight = this.height;
                boolean hasRequestedDimensions = requestedWidth > 0 && requestedHeight > 0;
                try {
                    int uploaded = uploadSourceTexture();
                    shared.textureId = Integer.valueOf(uploaded);
                    shared.width = this.width;
                    shared.height = this.height;
                } finally {
                    if (hasRequestedDimensions) {
                        this.width = requestedWidth;
                        this.height = requestedHeight;
                    }
                }
            }
            if (this.width <= 0 || this.height <= 0) {
                this.width = shared.width;
                this.height = shared.height;
            }
            this.textureId = shared.textureId;
            return shared.textureId.intValue();
        }
    }

    public int loadWithStb(ByteBuffer encoded) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
            ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, width, height, stack.mallocInt(1), 4);
            if (decoded == null) throw new IllegalStateException("Texture decode failed: " + STBImage.stbi_failure_reason());
            try {
                int texture = uploadTexture(decoded, width.get(0), height.get(0));
                this.width = width.get(0);
                this.height = height.get(0);
                return texture;
            } finally {
                // Upload failure must neither leak plaintext decoded pixels nor
                // close the same MemoryStack frame twice.
                try { wipe(decoded); }
                finally { STBImage.stbi_image_free(decoded); }
            }
        }
    }

    public int loadWithImageIo(ByteBuffer encoded) {
        byte[] encodedBytes = null;
        BufferedImage bufferedImage = null;
        int[] rgb = null;
        ByteBuffer decoded = null;
        try {
            encodedBytes = new byte[encoded.remaining()];
            encoded.duplicate().get(encodedBytes);
            bufferedImage = ImageIO.read(new ByteArrayInputStream(encodedBytes));
            if (bufferedImage == null) {
                throw new IllegalStateException("ImageIO failed to decode image (unsupported format or corrupt data).");
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            rgb = bufferedImage.getRGB(
                    0, 0, width, height, (int[]) null, 0, width
            );
            decoded = ByteBuffer
                    .allocateDirect(width * height * 4)
                    .order(ByteOrder.nativeOrder());
            for (int pixel : rgb) {
                decoded.put((byte) ((pixel >> 16) & StencilBufferUtil.STENCIL_MASK));
                decoded.put((byte) ((pixel >> 8) & StencilBufferUtil.STENCIL_MASK));
                decoded.put((byte) (pixel & StencilBufferUtil.STENCIL_MASK));
                decoded.put((byte) ((pixel >> 24) & StencilBufferUtil.STENCIL_MASK));
            }
            decoded.flip();
            this.width = width;
            this.height = height;
            return uploadTexture(decoded, width, height);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e2) {
            throw new IllegalStateException("Failed to decode image via ImageIO", e2);
        } finally {
            if (encodedBytes != null) {
                Arrays.fill(encodedBytes, (byte) 0);
            }
            if (rgb != null) {
                Arrays.fill(rgb, 0);
            }
            if (decoded != null) {
                wipe(decoded);
            }
            if (bufferedImage != null) {
                bufferedImage.flush();
            }
        }
    }

    public int uploadTexture(ByteBuffer byteBuffer, int i, int i2) {
        try (TextureUploadState ignored = new TextureUploadState()) {
            int texture = GL33.glGenTextures();
            boolean uploaded = false;
            try {
                GL33.glBindTexture(3553, texture);
                GL33.glTexParameteri(3553, 10242, this.wrapModeX);
                GL33.glTexParameteri(3553, 10243, this.wrapModeY);
                GL33.glTexParameteri(3553, 10241, this.minFilterMode);
                GL33.glTexParameteri(3553, 10240, this.magFilterMode);
                GL33.glTexImage2D(3553, 0, 6408, i, i2, 0, 6408, 5121, byteBuffer);
                if (this.mipmap) {
                    GL33.glGenerateMipmap(3553);
                }
                uploaded = true;
                return texture;
            } finally {
                if (!uploaded) GL33.glDeleteTextures(texture);
            }
        }
    }

    public boolean isWebp(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 12) {
            return false;
        }
        int iPosition = byteBuffer.position();
        return byteBuffer.get(iPosition) == 82 && byteBuffer.get(iPosition + 1) == 73 && byteBuffer.get(iPosition + 2) == 70 && byteBuffer.get(iPosition + 3) == 70 && byteBuffer.get(iPosition + 8) == 87 && byteBuffer.get(iPosition + 9) == 69 && byteBuffer.get(iPosition + 10) == 66 && byteBuffer.get(iPosition + 11) == 80;
    }

    public String hexDump(ByteBuffer byteBuffer, int i) {
        int iPosition = byteBuffer.position();
        byteBuffer.position(0);
        StringBuilder sb = new StringBuilder("[");
        int iMin = Math.min(i, byteBuffer.remaining());
        for (int i2 = 0; i2 < iMin; i2++) {
            if (i2 > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", Integer.valueOf(byteBuffer.get() & 255)));
        }
        byteBuffer.position(iPosition);
        return sb.append("]").toString();
    }

    public GlTexture(ByteBuffer byteBuffer) {
        this(null, Objects.requireNonNull(byteBuffer));
    }

    private GlTexture(ResourceSource source, ByteBuffer byteBuffer) {
        this.magFilterMode = 9729;
        this.minFilterMode = 9729;
        this.wrapModeX = 10497;
        this.wrapModeY = 10497;
        this.source = source;
        this.sharedResourcePath = source instanceof ClasspathResource classpath
                ? classpath.normalizedPath()
                : null;
        this.imageBuffer = byteBuffer;
    }

    private ByteBuffer ensureImageBuffer() {
        ByteBuffer current = this.imageBuffer;
        if (current != null) {
            return current;
        }
        if (this.source == null) {
            throw new IllegalStateException("Texture source is unavailable");
        }
        current = this.source.asDirectByteBuffer();
        this.imageBuffer = current;
        return current;
    }

    private void discardProtectedSourceBuffer(ByteBuffer used) {
        if (this.source == null || this.imageBuffer != used) {
            return;
        }
        wipe(used);
        this.imageBuffer = null;
    }

    private static void wipe(ByteBuffer buffer) {
        ByteBuffer value = buffer.duplicate();
        value.clear();
        while (value.hasRemaining()) {
            value.put((byte) 0);
        }
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public GlTexture magFilter(int i) {
        this.magFilterMode = i;
        return this;
    }

    public GlTexture minFilter(int i) {
        this.minFilterMode = i;
        return this;
    }

    public GlTexture wrapX(int i) {
        this.wrapModeX = i;
        return this;
    }

    public GlTexture wrapY(int i) {
        this.wrapModeY = i;
        return this;
    }

    public GlTexture generateMipMap(boolean z) {
        this.mipmap = z;
        return this;
    }

    private record SharedTextureKey(
            String resourcePath,
            int magFilterMode,
            int minFilterMode,
            int wrapModeX,
            int wrapModeY,
            boolean mipmap
    ) {
    }

    private static final class SharedTexture {
        private Integer textureId;
        private int width;
        private int height;
    }
}
