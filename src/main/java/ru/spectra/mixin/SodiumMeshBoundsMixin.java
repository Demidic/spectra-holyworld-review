package ru.spectra.mixin;

import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.spectra.client.render.culling.TerrainMeshAccess;
import ru.spectra.client.render.culling.TerrainMeshData;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts", remap = false)
public abstract class SodiumMeshBoundsMixin implements TerrainMeshAccess {
    @Unique private TerrainMeshData spectra$mesh;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void spectra$copyBounds(NativeBuffer vertices, int[] counts, CallbackInfo ci) {
        // Runs on the chunk builder, before its native vertex buffer is freed.
        this.spectra$mesh = TerrainMeshData.decode(vertices.getDirectBuffer(), counts);
    }

    @Override
    public TerrainMeshData spectra$terrainMesh() { return this.spectra$mesh; }
}
