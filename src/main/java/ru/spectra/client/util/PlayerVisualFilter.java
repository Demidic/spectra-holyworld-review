package ru.spectra.client.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Shared visibility rule for player-only render effects.
 *
 * <p>An invisible player is ignored while all four armor slots are empty.
 * Held items deliberately do not count as armor.</p>
 */
public final class PlayerVisualFilter {
    private PlayerVisualFilter() {
    }

    public static boolean shouldRender(Entity entity) {
        return !(entity instanceof PlayerEntity player) || shouldRender(player);
    }

    public static boolean shouldRender(PlayerEntity player) {
        if (!player.isInvisible()) {
            return true;
        }
        return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }
}
