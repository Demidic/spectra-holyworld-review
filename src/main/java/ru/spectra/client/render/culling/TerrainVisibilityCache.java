package ru.spectra.client.render.culling;

import org.joml.Matrix4fc;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.OptimizerModule;

/** Only reuses angle-dependent candidate lists. Position/world changes keep native invalidation. */
public final class TerrainVisibilityCache {
    private final ShaderCullingGuard shaders = new ShaderCullingGuard();
    private GuardedTerrainFrustum guard;
    private final org.joml.Matrix4f checkedClip = new org.joml.Matrix4f().zero();
    private double x, y, z;
    private float fogEnd, fogAlpha, shaderFogEnd;
    private boolean spectator, occlusion;
    private boolean reuse;

    public boolean prepare(double x, double y, double z, float fogEnd, float fogAlpha,
                           float shaderFogEnd, boolean spectator, boolean occlusion) {
        Spectra client = Spectra.INSTANCE;
        OptimizerModule optimizer = client == null || client.moduleRepository() == null ? null
                : client.moduleRepository().get(OptimizerModule.class);
        Matrix4fc clip = TerrainFrameView.clip();
        boolean enabled = TerrainFrameView.allowCaching() && optimizer != null && optimizer.isState() && optimizer.worldCulling.isValue()
                && !this.shaders.requiresNativeCulling() && clip.isFinite() && clip.determinant() != 0
                && this.x == x && this.y == y && this.z == z;
        this.reuse = enabled && this.guard != null && this.x == x && this.y == y && this.z == z
                && this.fogEnd == fogEnd && this.fogAlpha == fogAlpha && this.shaderFogEnd == shaderFogEnd
                && this.spectator == spectator && this.occlusion == occlusion
                && (this.checkedClip.equals(clip) || this.guard.contains(clip));
        this.checkedClip.set(clip);
        if (this.reuse) return false;
        boolean changed = this.guard != null || enabled;
        this.guard = enabled ? new GuardedTerrainFrustum(clip) : null;
        this.x = x; this.y = y; this.z = z;
        this.fogEnd = fogEnd; this.fogAlpha = fogAlpha; this.shaderFogEnd = shaderFogEnd;
        this.spectator = spectator; this.occlusion = occlusion;
        return changed;
    }

    public boolean canReuse() { return this.reuse; }
    public GuardedTerrainFrustum guard() { return this.guard; }
}
