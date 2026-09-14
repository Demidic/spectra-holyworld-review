package ru.spectra.client.util;
import ru.spectra.client.type.Mc;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.type.RotationDispatchMode;
import ru.spectra.client.internal.RotationDispatchModeSwitchMap;

import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class SwapUtil {
    public static RotationDispatchMode resolvedMethod() {
        return resolve(RotationDispatchMode.AUTO);
    }

    public static RotationDispatchMode resolve(RotationDispatchMode class389Var) {
        if (class389Var != RotationDispatchMode.AUTO) {
            return class389Var;
        }
        switch (ServerUtil.server) {
            case "FunTime":
                if (ServerUtil.getAnarchy() < 1000 && ServerUtil.getProtocolVersion() > 767) {
                    return RotationDispatchMode.DELAYED;
                }
                break;
            case "DexLand":
            case "MineBlaze":
                if (ServerUtil.getProtocolVersion() > 767) {
                    return RotationDispatchMode.DELAYED;
                }
                break;
            case "HolyWorld":
                return RotationDispatchMode.SEQUENTIAL;
        }
        return RotationDispatchMode.GRIM;
    }

    public static void swapAndExecute(int i, Rotation class007Var, boolean z, Runnable runnable) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || !GrimDelayHandler.script.isFinished()) {
            return;
        }
        int[] iArr = {player.getInventory().selectedSlot};
        switch (RotationDispatchModeSwitchMap.dispatchModeSwitchMap[class389VarResolvedMethod.ordinal()]) {
            case 1:
                GrimDelayHandler.addTask(class007Var, false, () -> {
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, player.getInventory().selectedSlot, SlotActionType.SWAP, true);
                    runnable.run();
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, player.getInventory().selectedSlot, SlotActionType.SWAP, true);
                    PlayerActionUtil.INSTANCE.updateSlots(true);
                });
                break;
            case 2:
                GrimDelayHandler.addTask(class007Var, z, () -> {
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, player.getInventory().selectedSlot, SlotActionType.SWAP, true);
                    runnable.run();
                    PlayerActionUtil.INSTANCE.clickSlot(player.currentScreenHandler.syncId, i, player.getInventory().selectedSlot, SlotActionType.SWAP, true);
                    PlayerActionUtil.INSTANCE.updateSlots(true);
                });
                break;
            case 3:
                GrimDelayHandler.script.addTickStep(0, () -> {
                    GrimDelayHandler.disableMoveKeys();
                }).addTickStep(1, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, Hand.MAIN_HAND, false);
                    runnable.run();
                    iArr[0] = player.getInventory().selectedSlot;
                }).addTickStep(2, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                    GrimDelayHandler.enableMoveKeys();
                });
                break;
            case 4:
                ActionScheduler class265VarAddTickStep = GrimDelayHandler.script.addTickStep(0, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, Hand.MAIN_HAND, false);
                    iArr[0] = player.getInventory().selectedSlot;
                });
                Objects.requireNonNull(runnable);
                class265VarAddTickStep.addTickStep(1, runnable::run).addTickStep(2, () -> {
                    PlayerActionUtil.INSTANCE.swapHand(i, iArr[0], true, true);
                });
                break;
        }
    }

    public static boolean needsStop() {
        return resolvedMethod() != RotationDispatchMode.VANILLA;
    }

    public static void swapAction(Runnable runnable) {
        swapAction(RotationManager.INSTANCE.getCurrentRotation(), true, runnable);
    }

    public static void swapAction(Rotation class007Var, boolean z, Runnable runnable) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        if (GrimDelayHandler.script.isFinished()) {
            if (Objects.requireNonNull(class389VarResolvedMethod) == RotationDispatchMode.VANILLA) {
                GrimDelayHandler.addTask(class007Var, false, runnable);
            } else {
                GrimDelayHandler.addTask(class007Var, z, runnable);
            }
        }
    }

    public static void swapToOffhand(int i) {
        RotationDispatchMode class389VarResolvedMethod = resolvedMethod();
        if (Mc.INSTANCE.getPlayer() != null && GrimDelayHandler.script.isFinished()) {
            if (Objects.requireNonNull(class389VarResolvedMethod) != RotationDispatchMode.VANILLA) {
                GrimDelayHandler.addTask(true, () -> {
                    PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, 40, false);
                    PlayerActionUtil.INSTANCE.updateSlots(true);
                });
            } else {
                PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, 40, false);
                PlayerActionUtil.INSTANCE.updateSlots(true);
            }
        }
    }

    public SwapUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
