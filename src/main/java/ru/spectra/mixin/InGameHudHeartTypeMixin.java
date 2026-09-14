package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.client.gui.hud.InGameHud$HeartType"})
public class InGameHudHeartTypeMixin {
    @Inject(method = {"fromPlayerState"}, at = {@At("RETURN")}, cancellable = true)
    private static void onFromPlayerState(PlayerEntity playerEntity, CallbackInfoReturnable<Enum> callbackInfoReturnable) {
        if (((Enum) callbackInfoReturnable.getReturnValue()).name().equals("WITHERED")) {
            RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.WITHER_HEARTS);
            Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
            if (class252Var.isCancelled()) {
                callbackInfoReturnable.setReturnValue(Enum.valueOf(((Enum) callbackInfoReturnable.getReturnValue()).getDeclaringClass(), "NORMAL"));
            }
        }
    }
}
