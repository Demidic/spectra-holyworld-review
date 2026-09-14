package ru.spectra.mixin;

import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.ChamsModule;
import ru.spectra.client.module.WorldTweaksModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin {

    @Shadow
    public Frustum frustum;

    @Inject(method = "render", at = @At("HEAD"))
    private void spectra$captureTerrainView(ObjectAllocator allocator, RenderTickCounter ticks, boolean outline,
                                             Camera camera, GameRenderer renderer, Matrix4f view,
                                             Matrix4f projection, CallbackInfo ci) {
        var position = camera.getPos();
        ru.spectra.client.render.culling.TerrainFrameView.set(view, projection, position.x, position.y, position.z);
        WorldTweaksModule worldTweaks = Spectra.INSTANCE.moduleRepository().get(WorldTweaksModule.class);
        Spectra.INSTANCE.moduleRepository().get(ChamsModule.class)
                .beginWorldFrame(worldTweaks.shouldRenderBlurFog());
    }

    @Inject(
            method = {"render"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Framebuffer;beginWrite(Z)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void spectra$composeFogBeforeClientEffects(
            ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean z, Camera camera, GameRenderer gameRenderer,
            Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo callbackInfo
    ) {
        WorldTweaksModule worldTweaks = Spectra.INSTANCE.moduleRepository().get(WorldTweaksModule.class);
        ChamsModule chams = Spectra.INSTANCE.moduleRepository().get(ChamsModule.class);
        try {
            worldTweaks.renderBlurFog(gameRenderer.getFarPlaneDistance());
        } finally {
            chams.drawDeferredColor();
        }
    }

    @Inject(method = {"renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"}, at = {@At("HEAD")})
    private void onRenderEntity(Entity entity, double d, double d2, double d3, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
        ((ChamsModule) Spectra.INSTANCE.moduleRepository().get(ChamsModule.class)).currentEntity(entity);
    }

    @Inject(method = {"renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"}, at = {@At("RETURN")})
    private void clearRenderedEntity(Entity entity, double d, double d2, double d3, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
        // The inventory preview is rendered after the world and does not
        // pass through renderEntity(). Keeping the last world entity here made
        // Chams treat that GUI model as the stale entity.
        ((ChamsModule) Spectra.INSTANCE.moduleRepository().get(ChamsModule.class)).currentEntity(null);
    }

    @Inject(method = {"render"}, at = {@At("TAIL")}, require = 0)
    private void onRender(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter, boolean z, Camera camera, GameRenderer gameRenderer, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo callbackInfo) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        Spectra.INSTANCE.eventDispatcher().dispatch(new WorldRenderEvent(matrixStack, matrix4f, matrix4f2, renderTickCounter, this.frustum));
    }

    @ModifyArgs(
            method = {"method_62215"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/SkyRendering;renderSky(FFF)V"
            ),
            require = 0
    )
    private void modifyCustomSkyColor(Args args, Fog fog, DimensionEffects.SkyType skyType,
                                      float tickDelta, DimensionEffects dimensionEffects) {
        WorldTweaksModule customSky = Spectra.INSTANCE.moduleRepository().get(WorldTweaksModule.class);
        int original = 0xFF000000
                | Math.round((Float) args.get(0) * 255.0f) << 16
                | Math.round((Float) args.get(1) * 255.0f) << 8
                | Math.round((Float) args.get(2) * 255.0f);
        int color = customSky.modifySkyColor(original);
        args.set(0, ((color >> 16) & 0xFF) / 255.0f);
        args.set(1, ((color >> 8) & 0xFF) / 255.0f);
        args.set(2, (color & 0xFF) / 255.0f);
    }

    @Inject(
            method = {"method_62215"},
            // Render after every vanilla sky layer. Rendering directly after
            // renderSky() allowed the later horizon/dark-sky pass to wash the
            // lower half of the custom dome into a hard horizontal rectangle.
            at = @At("TAIL"),
            require = 0
    )
    private void renderCustomSkyDome(Fog fog, DimensionEffects.SkyType skyType, float tickDelta,
                                     DimensionEffects dimensionEffects, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.moduleRepository().get(WorldTweaksModule.class).renderSkyShader(tickDelta);
    }

    @ModifyArg(
            method = {"method_62215"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/SkyRendering;renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;FIFFLnet/minecraft/client/render/Fog;)V"
            ),
            index = 5,
            require = 0
    )
    private float hideDefaultStarsForCustomSky(float starBrightness) {
        WorldTweaksModule customSky = Spectra.INSTANCE.moduleRepository().get(WorldTweaksModule.class);
        return customSky.shouldHideDefaultStars() ? 0.0f : starBrightness;
    }
}
