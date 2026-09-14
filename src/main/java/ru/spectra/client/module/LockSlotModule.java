package ru.spectra.client.module;

import ru.spectra.client.event.PacketSendEvent;
import ru.spectra.client.event.SlotClickEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public final class LockSlotModule extends Module {
    public final MultiSelectSetting<LockedSlot> slots =
            new MultiSelectSetting<LockedSlot>(Translation.clearText("Slots")).values(LockedSlot.class);

    public LockSlotModule() {
        super(ModuleTab.PLAYER, "Lock Slot");
        addSettings(slots);
        register(SlotClickEvent.class, event -> {
            if (!isState() || !isLockedContainerSlot(event.getSlotId())) {
                return;
            }
            if (event.getActionType() == SlotActionType.THROW
                    || event.getActionType() == SlotActionType.QUICK_MOVE) {
                event.cancel();
            }
        });
        register(PacketSendEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded()
                    || !(event.getPacket() instanceof PlayerActionC2SPacket packet)) {
                return;
            }
            if ((packet.getAction() == PlayerActionC2SPacket.Action.DROP_ITEM
                    || packet.getAction() == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS)
                    && slots.isSelected(LockedSlot.forHotbar(Mc.INSTANCE.getPlayer().getInventory().selectedSlot))) {
                event.cancel();
            }
        });
    }

    private boolean isLockedContainerSlot(int slot) {
        if (slot >= 36 && slot <= 44) {
            return slots.isSelected(LockedSlot.forHotbar(slot - 36));
        }
        return slot == 45 && slots.isSelected(LockedSlot.OFFHAND);
    }

    public boolean isHotbarSlotLocked(int slot) {
        return slot >= 0 && slot < 9
                && slots.isSelected(LockedSlot.forHotbar(slot));
    }

    public enum LockedSlot implements DisplayNamed {
        SLOT_1("Slot 1", 0), SLOT_2("Slot 2", 1), SLOT_3("Slot 3", 2),
        SLOT_4("Slot 4", 3), SLOT_5("Slot 5", 4), SLOT_6("Slot 6", 5),
        SLOT_7("Slot 7", 6), SLOT_8("Slot 8", 7), SLOT_9("Slot 9", 8),
        OFFHAND("Offhand", -1);

        private final Translation name;
        private final int hotbarSlot;

        LockedSlot(String name, int hotbarSlot) {
            this.name = Translation.clearText(name);
            this.hotbarSlot = hotbarSlot;
        }

        static LockedSlot forHotbar(int slot) {
            for (LockedSlot value : values()) {
                if (value.hotbarSlot == slot) {
                    return value;
                }
            }
            return OFFHAND;
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
