package ru.spectra.mixin;

import java.util.Collection;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.spectra.client.render.culling.TerrainMeshAccess;
import ru.spectra.client.render.culling.TerrainSectionAccess;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager", remap = false)
public abstract class SodiumRegionUploadMixin {
    @Inject(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;)V",
            at = @At("TAIL")
    )
    private void spectra$publishUploadedBounds(CommandList commands, RenderRegion region,
                                               Collection<BuilderTaskOutput> results, CallbackInfo ci) {
        ((ru.spectra.client.render.culling.TerrainRegionAccess) region).spectra$meshChanged();
        // Publish only after the matching geometry/order reaches the GPU. Discarded
        // or out-of-date worker results never reach this upload method.
        for (BuilderTaskOutput result : results) {
            TerrainSectionAccess section = (TerrainSectionAccess) result.render;
            if (result instanceof ChunkBuildOutput build) {
                for (var pass : DefaultTerrainRenderPasses.ALL) {
                    var mesh = build.getMesh(pass);
                    section.spectra$terrainMesh(pass, mesh == null ? null : ((TerrainMeshAccess) mesh).spectra$terrainMesh());
                }
            }
            if (result instanceof ChunkSortOutput sort && !sort.isReusingUploadedIndexData()
                    && sort.getIndexBuffer() != null) {
                var mesh = section.spectra$terrainMesh(DefaultTerrainRenderPasses.TRANSLUCENT);
                if (mesh != null) mesh.updateIndices(sort.getIndexBuffer().getDirectBuffer());
            }
        }
    }
}
