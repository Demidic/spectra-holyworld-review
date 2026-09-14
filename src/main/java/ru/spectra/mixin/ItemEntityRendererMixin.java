package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.module.ItemPhysicModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique
    private boolean spectra$itemOnGround;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("HEAD"))
    private void spectra$captureItemState(ItemEntity entity, ItemEntityRenderState state,
                                          float tickDelta, CallbackInfo ci) {
        spectra$itemOnGround = entity.isOnGround();
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
                    ordinal = 0))
    private void spectra$removeVanillaBob(MatrixStack matrices, float x, float y, float z,
                                          Operation<Void> original) {
        if (enabled()) {
            original.call(matrices, 0.0f, 0.0f, 0.0f);
        } else {
            original.call(matrices, x, y, z);
        }
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V",
                    ordinal = 0))
    private void spectra$layItemFlat(MatrixStack matrices, Quaternionf vanillaRotation,
                                     Operation<Void> original) {
        if (!enabled()) {
            original.call(matrices, vanillaRotation);
            return;
        }
        original.call(matrices, spectra$itemOnGround
                ? RotationAxis.POSITIVE_X.rotationDegrees(90.0f)
                : RotationAxis.POSITIVE_X.rotationDegrees(vanillaRotation.angle() * 300.0f));
    }

    @Unique
    private static boolean enabled() {
        return Spectra.INSTANCE != null && Spectra.INSTANCE.moduleRepository()
                .find(ItemPhysicModule.class).map(ru.spectra.client.module.Module::isState).orElse(false);
    }
}
