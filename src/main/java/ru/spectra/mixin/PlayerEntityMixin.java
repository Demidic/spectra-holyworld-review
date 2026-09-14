package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin {
    @ModifyExpressionValue(method = {"tick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")})
    private ItemStack injectSilentHotbar(ItemStack itemStack) {
        PlayerEntity playerEntity = (PlayerEntity) (Object) this;
        return playerEntity instanceof ClientPlayerEntity ? (ItemStack) playerEntity.getInventory().main.get(Spectra.INSTANCE.inventoryService().hotbarSlotSwapper().getClientsideSlot()) : itemStack;
    }
}
