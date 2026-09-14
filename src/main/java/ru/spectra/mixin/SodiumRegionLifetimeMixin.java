package ru.spectra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.spectra.client.render.culling.TerrainRegionAccess;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion", remap = false)
public abstract class SodiumRegionLifetimeMixin implements TerrainRegionAccess {
    @Unique private long spectra$meshRevision;
    @Override public long spectra$meshRevision() { return this.spectra$meshRevision; }
    @Override public void spectra$meshChanged() { this.spectra$meshRevision++; }
    @Inject(method = {"addSection", "removeSection", "delete", "refreshTesselation", "refreshIndexedTesselation"}, at = @At("HEAD"))
    private void spectra$invalidateDrawAddresses(CallbackInfo ci) { spectra$meshChanged(); }
}
