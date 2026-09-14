package ru.spectra.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.spectra.client.render.culling.TerrainVisibilityCache;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class SodiumVisibilityCacheMixin {
    @Shadow private RenderSectionManager renderSectionManager;
    @Unique private TerrainVisibilityCache spectra$visibility;

    @Unique
    private TerrainVisibilityCache spectra$visibilityCache() {
        // Mixin constructor extraction is sensitive to stripped line tables.
        // Setup runs on the render thread; initialize at the point of use.
        if (this.spectra$visibility == null) this.spectra$visibility = new TerrainVisibilityCache();
        return this.spectra$visibility;
    }

    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Viewport spectra$prepareCandidates(Viewport original, Camera camera, Viewport viewport, Fog fog,
                                                boolean spectator, boolean immediate) {
        var pos = camera.getPos();
        var visibility = this.spectra$visibilityCache();
        if (visibility.prepare(pos.x, pos.y, pos.z, fog.end(), fog.alpha(),
                com.mojang.blaze3d.systems.RenderSystem.getShaderFog().end(), spectator,
                net.minecraft.client.MinecraftClient.getInstance().chunkCullingEnabled)) {
            this.renderSectionManager.markGraphDirty();
        }
        var guard = visibility.guard();
        return guard == null ? original : new Viewport(guard::testAab, new Vector3d(pos.x, pos.y, pos.z));
    }

    @WrapOperation(method = "setupTerrain", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;markGraphDirty()V"))
    private void spectra$reuseCandidates(RenderSectionManager manager, Operation<Void> original) {
        // Never clear an existing dirty flag: block edits/uploads still rebuild immediately.
        if (this.spectra$visibility == null || !this.spectra$visibility.canReuse()) original.call(manager);
    }

}
