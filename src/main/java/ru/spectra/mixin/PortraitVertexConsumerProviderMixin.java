package ru.spectra.mixin;

import ru.spectra.client.ui.TargetHudWidget;
import ru.spectra.client.render.PortraitAlphaVertexConsumer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VertexConsumerProvider.Immediate.class)
public abstract class PortraitVertexConsumerProviderMixin {
    @ModifyReturnValue(method = "getBuffer", at = @At("RETURN"))
    private VertexConsumer spectra$fadePortraitLayers(
            VertexConsumer original, RenderLayer layer) {
        if (!TargetHudWidget.isRenderingModelPortrait()) {
            return original;
        }
        float opacity = TargetHudWidget.modelPortraitOpacity();
        return opacity >= 0.999f
                ? original
                : new PortraitAlphaVertexConsumer(original, opacity);
    }
}
