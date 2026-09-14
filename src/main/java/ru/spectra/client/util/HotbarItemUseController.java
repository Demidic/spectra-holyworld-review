package ru.spectra.client.util;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.model.SlotSearchResult2;
import ru.spectra.client.type.InventoryScope;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.NotificationType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.concurrent.TimeUnit;

/**
 * Performs the visible, hotbar-only item use shared by the server helpers.
 * Inventory slots are deliberately never swapped into the hotbar.
 */
public final class HotbarItemUseController {
    private static final long USE_DELAY_MS = 60L;
    private static final long RESTORE_DELAY_MS = 170L;
    private static final long MAX_HELD_USE_MS = 5000L;

    private int originalSlot = -1;
    private int targetSlot = -1;
    private long startedAt;
    private boolean used;
    private Rotation rotation = Rotation.playerRotation();
    private String displayName = "";
    private ItemStack displayStack = ItemStack.EMPTY;

    public boolean request(SlotSearchResult2 result, Rotation requestedRotation,
                           String name, ItemStack stack) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (isActive() || player == null || Mc.INSTANCE.getCurrentScreen() != null
                || result == null || !result.found()
                || result.slotReference().scope() != InventoryScope.HOTBAR) {
            return false;
        }
        int slot = result.slotReference().slot();
        if (slot < 0 || slot > 8) {
            return false;
        }
        this.originalSlot = player.getInventory().selectedSlot;
        this.targetSlot = slot;
        this.startedAt = System.currentTimeMillis();
        this.used = false;
        this.rotation = requestedRotation == null ? Rotation.playerRotation() : requestedRotation;
        this.displayName = name == null ? result.stack().getName().getString() : name;
        this.displayStack = stack == null ? result.stack().copy() : stack.copy();
        select(player, slot);
        return true;
    }

    public void update() {
        if (!isActive()) {
            return;
        }
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || Mc.INSTANCE.getInteractionManager() == null) {
            clear();
            return;
        }
        long elapsed = System.currentTimeMillis() - this.startedAt;
        if (!this.used && elapsed >= USE_DELAY_MS) {
            PlayerActionUtil.INSTANCE.interactItem(Hand.MAIN_HAND, this.rotation, true);
            this.used = true;
            Spectra.INSTANCE.notificationRepository().post(
                    Text.literal(Lang.USE_ITEM.effective().replace("{item}",
                            String.valueOf(Formatting.RED) + this.displayName + Formatting.RESET)),
                    this.displayStack, 2L, TimeUnit.SECONDS);
        }
        if (!this.used || elapsed < RESTORE_DELAY_MS) {
            return;
        }
        if (player.isUsingItem() && elapsed < MAX_HELD_USE_MS) {
            return;
        }
        restore(player);
    }

    public void cancel() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null && isActive()) {
            restore(player);
        } else {
            clear();
        }
    }

    public boolean isActive() {
        return this.targetSlot >= 0;
    }

    public static void notifyNotInHotbar() {
        Spectra.INSTANCE.notificationRepository().post(
                NotificationType.ERROR,
                Text.literal(ClientLocalization.text(
                        "Item is not in the hotbar!", "\u041F\u0440\u0435\u0434\u043C\u0435\u0442 \u043D\u0435 \u0432 \u0445\u043E\u0442\u0431\u0430\u0440\u0435!")),
                2L, TimeUnit.SECONDS);
    }

    private void restore(ClientPlayerEntity player) {
        if (this.originalSlot >= 0 && this.originalSlot <= 8) {
            select(player, this.originalSlot);
        }
        clear();
    }

    private static void select(ClientPlayerEntity player, int slot) {
        if (player.getInventory().selectedSlot == slot) {
            return;
        }
        player.getInventory().selectedSlot = slot;
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void clear() {
        this.originalSlot = -1;
        this.targetSlot = -1;
        this.startedAt = 0L;
        this.used = false;
        this.displayName = "";
        this.displayStack = ItemStack.EMPTY;
    }
}
