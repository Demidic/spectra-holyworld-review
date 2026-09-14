package ru.spectra.client.util;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Render-thread cache for one immutable quad batch (outlines or atlas glyphs).
 * Text callers must bind the current atlas and key by its allocated slot. */
public final class RectangleMeshCache {
    private final Matrix4f matrix = new Matrix4f();
    private final Vector4f scratch = new Vector4f();
    private final float[] key = new float[9];
    private byte[] vertices = new byte[0];
    private int[] indices = new int[0];
    private int vertexSize, indexSize, vertexCount, color, framebufferHeight;
    private long vertexStart, indexStart;
    private int firstVertex;
    private DrawEngine owner;
    private boolean valid;

    public boolean replayOrBegin(DrawEngine draw, Matrix4f transform,
            float left, float top, float right, float bottom, float thickness,
            float upper, float lower, int rgba, int height) {
        float alpha = draw.vertexAlpha();
        if (valid && owner == draw && color == rgba && framebufferHeight == height
                && matrix.properties() == transform.properties() && matrix.equals(transform)
                && same(key[0], left) && same(key[1], top) && same(key[2], right) && same(key[3], bottom)
                && same(key[4], thickness) && same(key[5], upper) && same(key[6], lower)
                && same(key[7], draw.contentScale) && same(key[8], alpha)) {
            draw.vertexBuffer.requireMoreFreeBytes(vertexSize);
            draw.indexBuffer.requireMoreFreeBytes((long) indexSize * Integer.BYTES);
            MemoryUtil.memByteBuffer(draw.vertexBuffer.effectiveAddress(), vertexSize)
                    .put(vertices, 0, vertexSize);
            long destination = draw.indexBuffer.effectiveAddress();
            for (int i = 0; i < indexSize; i++) {
                draw.indexBuffer.writeInt(destination + (long) i * Integer.BYTES,
                        indices[i] + draw.vertexCount);
            }
            draw.vertexBuffer.offset(vertexSize);
            draw.indexBuffer.offset((long) indexSize * Integer.BYTES);
            draw.vertexCount += vertexCount;
            draw.indexCount += indexSize;
            draw.tempVector.set(scratch);
            return true;
        }
        valid = false;
        owner = draw;
        matrix.set(transform);
        key[0] = left; key[1] = top; key[2] = right; key[3] = bottom;
        key[4] = thickness; key[5] = upper; key[6] = lower;
        key[7] = draw.contentScale; key[8] = alpha;
        color = rgba;
        framebufferHeight = height;
        vertexStart = draw.vertexBuffer.position;
        indexStart = draw.indexBuffer.position;
        firstVertex = draw.vertexCount;
        return false;
    }

    private static boolean same(float a, float b) {
        return Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b);
    }

    /** Call only after quad emission with no intervening texture binds or flushes. */
    public void finish(DrawEngine draw) {
        long bytes = draw.vertexBuffer.position - vertexStart;
        long indexBytes = draw.indexBuffer.position - indexStart;
        // Bound retention even for malformed or exceptionally large layouts.
        if (draw != owner || bytes <= 0 || bytes > 256 * 1024
                || indexBytes < 0 || indexBytes > 256 * 1024 || indexBytes % 4 != 0) return;
        vertexSize = (int) bytes;
        indexSize = (int) (indexBytes / 4);
        vertexCount = draw.vertexCount - firstVertex;
        if (vertices.length < vertexSize) vertices = new byte[vertexSize];
        if (indices.length < indexSize) indices = new int[indexSize];
        MemoryUtil.memByteBuffer(draw.vertexBuffer.address + vertexStart, vertexSize)
                .get(vertices, 0, vertexSize);
        for (int i = 0; i < indexSize; i++) {
            indices[i] = draw.indexBuffer.readInt(draw.indexBuffer.address + indexStart + (long) i * 4)
                    - firstVertex;
        }
        scratch.set(draw.tempVector);
        valid = true;
    }
}
