package ru.spectra.mixin;

import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.ItemUseController;
import ru.spectra.client.event.SlotClickEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = {"stopUsingItem"}, at = {@At("HEAD")}, cancellable = true)
    public void stopUsingItem(CallbackInfo callbackInfo) {
        if (ItemUseController.INSTANCE.isUseItem()) {
            ItemUseController.INSTANCE.setUseItem(false);
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"clickSlot"}, at = {@At("HEAD")}, cancellable = true)
    public void clickSlotHook(int i, int i2, int i3, SlotActionType slotActionType, PlayerEntity playerEntity, CallbackInfo callbackInfo) {
        SlotClickEvent class233Var = new SlotClickEvent(i, i2, i3, slotActionType);
        Spectra.INSTANCE.eventDispatcher().dispatch(class233Var);
        if (class233Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = {"syncSelectedSlot"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")})
    private int hookCustomSelectedSlot(int i) {
        return Spectra.INSTANCE.inventoryService().hotbarSlotSwapper().getServersideSlot();
    }

    @Inject(method = {"attackEntity"}, at = {@At("HEAD")})
    private void attackEntity(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new AttackEntityEvent(entity));
    }
}
