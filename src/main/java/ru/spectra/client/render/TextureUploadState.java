package ru.spectra.client.render;

import org.lwjgl.opengl.GL33;

/** Isolates client-memory uploads from Minecraft/mod PBO and pixel-store state. */
final class TextureUploadState implements AutoCloseable {
    private static final int[] OPTIONS = {
            GL33.GL_UNPACK_SWAP_BYTES, GL33.GL_UNPACK_LSB_FIRST, GL33.GL_UNPACK_ROW_LENGTH,
            GL33.GL_UNPACK_IMAGE_HEIGHT, GL33.GL_UNPACK_SKIP_ROWS, GL33.GL_UNPACK_SKIP_PIXELS,
            GL33.GL_UNPACK_SKIP_IMAGES, GL33.GL_UNPACK_ALIGNMENT
    };
    private final int texture = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
    private final int unpackBuffer = GL33.glGetInteger(GL33.GL_PIXEL_UNPACK_BUFFER_BINDING);
    private final int[] values = new int[OPTIONS.length];

    TextureUploadState() {
        for (int i = 0; i < OPTIONS.length; i++) values[i] = GL33.glGetInteger(OPTIONS[i]);
        GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER, 0);
        for (int option : OPTIONS) GL33.glPixelStorei(option, option == GL33.GL_UNPACK_ALIGNMENT ? 1 : 0);
    }

    @Override public void close() {
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, this.texture);
        for (int i = 0; i < OPTIONS.length; i++) GL33.glPixelStorei(OPTIONS[i], this.values[i]);
        GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER, this.unpackBuffer);
    }
}
