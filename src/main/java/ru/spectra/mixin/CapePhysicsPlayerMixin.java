package ru.spectra.mixin;

import ru.spectra.client.cape.CapePhysicsManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class CapePhysicsPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void spectra$updateCapeSimulation(CallbackInfo ci) {
        CapePhysicsManager.tick((AbstractClientPlayerEntity) (Object) this);
    }
}
