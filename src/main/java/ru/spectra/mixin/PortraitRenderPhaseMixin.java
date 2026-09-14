package ru.spectra.mixin;

import ru.spectra.client.ui.TargetHudWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armor and held-item layers normally use opaque/cutout render phases. Their
 * vertex alpha is therefore ignored unless blending is enabled after the
 * phase has installed its own state. Restrict this override to the tiny
 * Target HUD portrait render.
 */
@Mixin(RenderPhase.class)
public abstract class PortraitRenderPhaseMixin {
    @Inject(method = "startDrawing", at = @At("TAIL"))
    private void spectra$enablePortraitLayerAlpha(CallbackInfo ci) {
        if (!TargetHudWidget.isRenderingModelPortrait()
                || TargetHudWidget.modelPortraitOpacity() >= 0.999f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Inject(method = "endDrawing", at = @At("TAIL"))
    private void spectra$restorePortraitLayerAlpha(CallbackInfo ci) {
        if (!TargetHudWidget.isRenderingModelPortrait()
                || TargetHudWidget.modelPortraitOpacity() >= 0.999f) {
            return;
        }
        // Some opaque/cutout phases have no transparency teardown of their
        // own. Do not let the portrait's temporary blending leak into the HUD.
        RenderSystem.disableBlend();
    }
}
