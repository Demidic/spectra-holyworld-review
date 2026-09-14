package ru.spectra.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import ru.spectra.client.render.culling.TerrainMeshData;
import ru.spectra.client.render.culling.TerrainSectionAccess;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSection", remap = false)
public abstract class SodiumSectionBoundsMixin implements TerrainSectionAccess {
    @Unique private TerrainMeshData spectra$solid, spectra$cutout, spectra$translucent;

    @Override
    public TerrainMeshData spectra$terrainMesh(Object pass) {
        if (pass == DefaultTerrainRenderPasses.SOLID) return this.spectra$solid;
        if (pass == DefaultTerrainRenderPasses.CUTOUT) return this.spectra$cutout;
        if (pass == DefaultTerrainRenderPasses.TRANSLUCENT) return this.spectra$translucent;
        return null;
    }

    @Override
    public void spectra$terrainMesh(Object pass, TerrainMeshData mesh) {
        if (mesh != null) mesh.reuseOrder(spectra$terrainMesh(pass));
        if (pass == DefaultTerrainRenderPasses.SOLID) this.spectra$solid = mesh;
        else if (pass == DefaultTerrainRenderPasses.CUTOUT) this.spectra$cutout = mesh;
        else if (pass == DefaultTerrainRenderPasses.TRANSLUCENT) this.spectra$translucent = mesh;
    }
}
