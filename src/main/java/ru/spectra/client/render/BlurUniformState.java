package ru.spectra.client.render;

import java.nio.FloatBuffer;

/** Render-thread state for one blur program. No images or previous frames are cached. */
final class BlurUniformState {
    private boolean initialized;
    private int pairs, brightnessBits;
    private int[] kernelBits, offsetBits;

    void invalidate() { initialized = false; }

    void upload(ShaderUniform texture, ShaderUniform pairCount, ShaderUniform kernel,
                ShaderUniform offsets, ShaderUniform brightness, int count,
                FloatBuffer weights, FloatBuffer distances, float gain) {
        if (!initialized) texture.uploadInt(0);
        if (!initialized || pairs != count) pairCount.uploadInt(count);
        if (!initialized || !matches(kernelBits, weights)) {
            kernel.uploadFloatBuffer(weights);
            kernelBits = snapshot(kernelBits, weights);
        }
        if (!initialized || !matches(offsetBits, distances)) {
            offsets.uploadFloatBuffer(distances);
            offsetBits = snapshot(offsetBits, distances);
        }
        int bits = Float.floatToRawIntBits(gain);
        if (!initialized || brightnessBits != bits) brightness.uploadFloat(gain);
        pairs = count;
        brightnessBits = bits;
        initialized = true;
    }

    // Buffers are public in BlurEffect. Compare their current contents rather than
    // assuming buffer identity/radius implies unchanged values. Preserve position.
    private static boolean matches(int[] previous, FloatBuffer values) {
        if (previous == null || previous.length != values.remaining()) return false;
        for (int i = 0; i < previous.length; i++)
            if (previous[i] != Float.floatToRawIntBits(values.get(values.position() + i))) return false;
        return true;
    }

    private static int[] snapshot(int[] reuse, FloatBuffer values) {
        if (reuse == null || reuse.length != values.remaining()) reuse = new int[values.remaining()];
        for (int i = 0; i < reuse.length; i++) reuse[i] = Float.floatToRawIntBits(values.get(values.position() + i));
        return reuse;
    }
}
