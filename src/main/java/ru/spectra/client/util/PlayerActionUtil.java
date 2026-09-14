package ru.spectra.client.util;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import ru.spectra.client.math.Rotation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class PlayerActionUtil {
    public static final PlayerActionUtil INSTANCE = new PlayerActionUtil();

    public OptionalInt hotbarSlotsStream(IntPredicate intPredicate) {
        return IntStream.range(0, 9).filter(intPredicate).findFirst();
    }

    public float getRemainingCooldownSeconds(Item item) {
        Mc class815Var = Mc.INSTANCE;
        ItemCooldownManager itemCooldownManager = class815Var.getPlayer().getItemCooldownManager();
        ItemCooldownManager.Entry entry = (ItemCooldownManager.Entry) itemCooldownManager.entries.get(Registries.ITEM.getId(item));
        if (entry == null) {
            return 0.0f;
        }
        return Math.max(1, (int) Math.ceil(itemCooldownManager.getCooldownProgress(item.getDefaultStack(), class815Var.getMinecraft().getRenderTickCounter().getTickDelta(false)) * (entry.endTick - entry.startTick))) / 20.0f;
    }

    public void interactItem(Hand hand, Rotation class007Var, boolean z) {
        Mc class815Var = Mc.INSTANCE;
        ClientPlayerEntity player = class815Var.getPlayer();
        Optional.of(Integer.valueOf(ServerUtil.getProtocolVersion())).filter(num -> {
            return num.intValue() > 754 && num.intValue() < 767;
        }).ifPresent(num2 -> {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(), class007Var.getYaw(), class007Var.getPitch(), player.isOnGround(), false));
        });
        Spectra.INSTANCE.inventoryService().itemInteractor().interactItem(player, hand, class007Var);
        if (z) {
            class815Var.getPlayer().networkHandler.sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public int getEmptySlots(PlayerInventory playerInventory) {
        int i = 0;
        for (int i2 = 0; i2 < 36; i2++) {
            if (playerInventory.getStack(i2).isEmpty()) {
                i++;
            }
        }
        return i;
    }







    public Slot mainHandSlot() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        DefaultedList defaultedList = player.currentScreenHandler.slots;
        long size = defaultedList.size();
        return (Slot) defaultedList.get(Math.toIntExact((size - ((long) (size == 46 ? 10 : 9))) + ((long) player.getInventory().selectedSlot)));
    }

    public void swapHand(int i, Hand hand) {
        if (i == -1) {
            return;
        }
        windowClick(SlotActionType.SWAP, i, hand.equals(Hand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().selectedSlot : 40);
    }

    public void clickSlot(int i, int i2, int i3, SlotActionType slotActionType, boolean z) {
        Mc class815Var = Mc.INSTANCE;
        ScreenHandler currentScreenHandler = class815Var.getCurrentScreenHandler();
        if (!z) {
            currentScreenHandler.onSlotClick(i2, i3, slotActionType, class815Var.getPlayer());
        }
        class815Var.getPlayer().networkHandler.sendPacket(new ClickSlotC2SPacket(i, currentScreenHandler.getRevision(), i2, i3, slotActionType, currentScreenHandler.getCursorStack().copy(), Int2ObjectMaps.emptyMap()));
    }

    public void swapHand(int i, Hand hand, boolean z, boolean z2) {
        if (i == -1) {
            return;
        }
        windowClick(SlotActionType.SWAP, i, hand.equals(Hand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().selectedSlot : 40, !z && z2);
        if (z) {
            updateSlots(z2);
        }
    }

    public void swapHand(int i, Hand hand, boolean z) {
        if (i == -1) {
            return;
        }
        windowClick(SlotActionType.SWAP, i, hand.equals(Hand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().selectedSlot : 40);
        if (z) {
            updateSlots(false);
        }
    }

    public void swapHand(int i, int i2, boolean z) {
        if (i == -1) {
            return;
        }
        windowClick(SlotActionType.SWAP, i, i2);
        if (z) {
            updateSlots(false);
        }
    }

    public void swapHand(int i, int i2, boolean z, boolean z2) {
        if (i == -1) {
            return;
        }
        windowClick(SlotActionType.SWAP, i, i2, !z && z2);
        if (z) {
            updateSlots(z2);
        }
    }

    public void updateSlots(boolean z) {
        if (ServerUtil.getProtocolVersion() < 755) {
            return;
        }
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        ItemStack defaultStack = ((Item) Registries.ITEM.get(MathUtil.getRandom(0, 100))).getDefaultStack();
        player.networkHandler.sendPacket(new ClickSlotC2SPacket(player.currentScreenHandler.syncId, 0, ((Integer) player.currentScreenHandler.slots.stream().filter(slot -> {
            return !slot.getStack().isEmpty();
        }).map(slot2 -> {
            return Integer.valueOf(slot2.id);
        }).findFirst().orElse(0)).intValue(), 0, SlotActionType.PICKUP_ALL, defaultStack, Int2ObjectMaps.singleton(0, defaultStack)));
        if (z) {
            player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId));
        }
    }

    public void windowClick(SlotActionType slotActionType, int i, int i2, boolean z) {
        Mc class815Var;
        ClientPlayerEntity player;
        if (i == -1 || (player = (class815Var = Mc.INSTANCE).getPlayer()) == null) {
            return;
        }
        int i3 = player.currentScreenHandler.syncId;
        class815Var.getInteractionManager().clickSlot(i3, i, i2, slotActionType, player);
        System.out.println("windowClick successful: Slot ID = " + i + ", Button = " + i2 + ", Type = " + String.valueOf(slotActionType));
        if (z) {
            player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(i3));
        }
    }

    public void windowClick(SlotActionType slotActionType, int i, int i2) {
        Mc class815Var = Mc.INSTANCE;
        ClientPlayerEntity player = class815Var.getPlayer();
        if (player == null) {
            return;
        }
        class815Var.getInteractionManager().clickSlot(player.currentScreenHandler.syncId, i, i2, slotActionType, player);
        System.out.println("windowClick successful: Slot ID = " + i + ", Button = " + i2 + ", Type = " + String.valueOf(slotActionType));
    }
}
