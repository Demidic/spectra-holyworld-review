package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.VisualEffectEvent;
import ru.spectra.client.type.VisualEffectType;
import ru.spectra.client.event.GammaEvent;
import ru.spectra.client.module.FullBrightModule;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LightmapTextureManager.class})
public class LightMapTextureManagerMixin {
    @Shadow
    private boolean dirty;

    @Inject(method = "update(F)V", at = @At("HEAD"))
    private void spectra$forceFullBrightUpdate(float delta, CallbackInfo ci) {
        if (spectra$isGammaFullBrightActive()
                || spectra$isNightVisionActive()
                || FullBrightModule.consumeRestoreLightmapRequest()) {
            this.dirty = true;
        }
    }

    @ModifyExpressionValue(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getSkyBrightness(F)F"
            )
    )
    private float spectra$maximizeSkyBrightness(float original) {
        return spectra$isGammaFullBrightActive() ? 1.0f : original;
    }

    @ModifyExpressionValue(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/dimension/DimensionType;ambientLight()F"
            )
    )
    private float spectra$maximizeAmbientLight(float original) {
        return spectra$isGammaFullBrightActive() ? 1.0f : original;
    }

    @ModifyExpressionValue(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/DimensionEffects;shouldBrightenLighting()Z"
            )
    )
    private boolean spectra$useBrightLightmap(boolean original) {
        return spectra$isGammaFullBrightActive() || original;
    }

    @ModifyExpressionValue(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;getSkyDarkness(F)F"
            )
    )
    private float spectra$removeWorldDarkening(float original) {
        return spectra$isGammaFullBrightActive() ? 0.0f : original;
    }

    @ModifyReturnValue(method = "getDarknessFactor(F)F", at = @At("RETURN"))
    private float spectra$removeDarknessForFullBright(float original) {
        return spectra$isGammaFullBrightActive() ? 0.0f : original;
    }

    @ModifyReturnValue(
            method = "getDarkness(Lnet/minecraft/entity/LivingEntity;FF)F",
            at = @At("RETURN")
    )
    private float spectra$removeDarknessScaleForFullBright(float original) {
        return spectra$isGammaFullBrightActive() ? 0.0f : original;
    }

    @ModifyExpressionValue(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
                    ordinal = 0
            )
    )
    private boolean spectra$applyArtificialNightVision(boolean original) {
        return spectra$isNightVisionActive() || original;
    }

    @Redirect(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"
            )
    )
    private float spectra$keepArtificialNightVisionStable(LivingEntity entity, float tickDelta) {
        return spectra$isNightVisionActive()
                ? 1.0f
                : GameRenderer.getNightVisionStrength(entity, tickDelta);
    }

    @ModifyExpressionValue(method = {"update(F)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1)})
    private Object onBrightness(Object obj) {
        GammaEvent class336Var = new GammaEvent();
        Spectra.INSTANCE.eventDispatcher().dispatch(class336Var);
        return class336Var.isCancelled() ? Double.valueOf(class336Var.getGamma()) : obj;
    }

    @Redirect(method = {"getDarknessFactor"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;"))
    private StatusEffectInstance injectAntiDarkness(ClientPlayerEntity clientPlayerEntity, RegistryEntry<StatusEffect> registryEntry) {
        VisualEffectEvent class258Var = new VisualEffectEvent(VisualEffectType.DARKNESS);
        Spectra.INSTANCE.eventDispatcher().dispatch(class258Var);
        if (class258Var.isCancelled()) {
            return null;
        }
        return clientPlayerEntity.getStatusEffect(registryEntry);
    }

    private static boolean spectra$isGammaFullBrightActive() {
        try {
            FullBrightModule module = Spectra.INSTANCE.moduleRepository()
                    .get(FullBrightModule.class);
            return module.isState() && module.isFullBrightMode();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean spectra$isNightVisionActive() {
        try {
            FullBrightModule module = Spectra.INSTANCE.moduleRepository()
                    .get(FullBrightModule.class);
            return module.isState() && module.isNightVisionMode();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
