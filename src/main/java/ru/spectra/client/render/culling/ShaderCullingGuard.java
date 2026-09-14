package ru.spectra.client.render.culling;

import java.lang.reflect.Method;
import ru.spectra.client.Spectra;

/** Shader vertex displacement and shadow/voxel passes require pack-owned bounds. */
final class ShaderCullingGuard {
    private boolean resolved;
    private boolean failed;
    private Object api;
    private Method shaderPackInUse;

    boolean requiresNativeCulling() {
        if (this.failed) return true;
        try {
            if (!this.resolved) {
                this.resolved = true;
                Class<?> type;
                try {
                    type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                } catch (ClassNotFoundException absent) {
                    return false;
                }
                this.api = type.getMethod("getInstance").invoke(null);
                this.shaderPackInUse = type.getMethod("isShaderPackInUse");
                if (this.api == null) throw new IllegalStateException("Iris API unavailable");
            }
            return this.shaderPackInUse != null && (boolean) this.shaderPackInUse.invoke(this.api);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            this.failed = true;
            Spectra.LOGGER.warn("Cannot inspect shader rendering; preserving native terrain culling", error);
            return true;
        }
    }
}
