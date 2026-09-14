package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.module.LockSlotModule;
import ru.spectra.client.module.Module;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerInventory.class})
public class PlayerInventoryMixin {

    @Shadow
    public int selectedSlot;

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void spectra$cancelLockedDrop(boolean entireStack,
                                          CallbackInfoReturnable<ItemStack> cir) {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null) {
            return;
        }
        boolean locked = Spectra.INSTANCE.moduleRepository()
                .find(LockSlotModule.class)
                .filter(Module::isState)
                .map(module -> module.isHotbarSlotLocked(this.selectedSlot))
                .orElse(false);
        if (locked) {
            // Cancel before vanilla mutates the client inventory. Cancelling only
            // the outgoing packet leaves a phantom empty slot until resync.
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @ModifyExpressionValue(method = {"dropSelectedItem", "getBlockBreakingSpeed", "getMainHandStack"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")})
    private int hookOverrideOriginalSlot(int i) {
        return ((PlayerInventory) (Object) this).player == MinecraftClient.getInstance().player ? Spectra.INSTANCE.inventoryService().hotbarSlotSwapper().getServersideSlot() : i;
    }
}
