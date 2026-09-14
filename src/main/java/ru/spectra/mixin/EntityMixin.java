package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.EntityInvisibilityEvent;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {

    @Inject(method = {"isGlowing"}, at = {@At("HEAD")}, cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.GLOWING);
        Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
        if (class252Var.isCancelled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(at = {@At("RETURN")}, method = {"isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"}, cancellable = true)
    private void onIsInvisibleTo(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Entity entity;
        if (callbackInfoReturnable.getReturnValueZ() && (entity = (Entity) (Object) this) != null) {
            EntityInvisibilityEvent class198Var = new EntityInvisibilityEvent(entity);
            Spectra.INSTANCE.eventDispatcher().dispatch(class198Var);
            if (class198Var.isCancelled()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

}
