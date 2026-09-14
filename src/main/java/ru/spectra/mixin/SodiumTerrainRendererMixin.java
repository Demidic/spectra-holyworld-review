package ru.spectra.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.spectra.client.render.culling.TerrainDrawBuilder;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public abstract class SodiumTerrainRendererMixin {
    @Shadow @Final @Mutable private MultiDrawBatch batch;
    @Unique private MultiDrawBatch spectra$nativeBatch;
    @Unique private TerrainDrawBuilder spectra$draws;
    @Unique private boolean spectra$culling;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void spectra$rememberNativeBatch(CallbackInfo ci) {
        this.spectra$nativeBatch = this.batch;
        this.spectra$draws = new TerrainDrawBuilder();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spectra$prepareCulling(ChunkRenderMatrices matrices, CommandList commands,
                                        ChunkRenderListIterable lists, TerrainRenderPass pass,
                                        CameraTransform camera, CallbackInfo ci) {
        this.batch = this.spectra$nativeBatch;
        this.spectra$culling = this.spectra$draws.begin(matrices, pass, camera);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;fillCommandBuffer(Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Z)V"))
    private void spectra$filterBlockFaces(MultiDrawBatch nativeBatch, RenderRegion region,
                                          SectionRenderDataStorage storage, ChunkRenderList list,
                                          CameraTransform camera, TerrainRenderPass pass,
                                          boolean faceCulling, Operation<Void> original) {
        if (this.spectra$culling) {
            this.batch = this.spectra$draws.build(region, storage, list, camera, pass, faceCulling);
        } else {
            original.call(nativeBatch, region, storage, list, camera, pass, faceCulling);
        }
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void spectra$releaseDraws(CommandList commands, CallbackInfo ci) {
        this.batch = this.spectra$nativeBatch;
        if (this.spectra$draws != null) this.spectra$draws.close();
    }
}

