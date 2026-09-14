package ru.spectra.client.util;
import ru.spectra.client.math.DirectionalInput;
import ru.spectra.client.type.Mc;
import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.math.SimulatedPlayer;

import java.util.Objects;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class GrimDelayHandler {
    public static final ActionScheduler script = new ActionScheduler();
    public static boolean canMove = true;
    public static final Mc mc = Mc.INSTANCE;

    public static void onWorldChange() {
        canMove = true;
        script.cleanup();
        refreshPressedKeys();
    }

    public static void tick() {
        script.update().cleanupIfFinished();
    }

    public static void input(MovementInputEvent class040Var) {
        if (canMove) {
            return;
        }
        class040Var.setStopProgression(true);
        class040Var.setJumping(false);
        class040Var.setSprinting(false);
        class040Var.setSneaking(false);
        class040Var.setInput(DirectionalInput.NONE);
    }

    public static void addTask(Runnable runnable) {
        addTask(RotationManager.INSTANCE.getCurrentRotation(), runnable);
    }

    public static void addTask(boolean z, Runnable runnable) {
        addTask(RotationManager.INSTANCE.getCurrentRotation(), z, runnable);
    }

    public static int getTaskRunnableTime() {
        switch (ServerUtil.getServer()) {
            case "FunTime":
                return (ServerUtil.getAnarchy() >= 100 || ServerUtil.getProtocolVersion() <= 767) ? 1 : 2;
            case "SpookyTime":
                if (!MovementInputHelper.hasPlayerMovement()) {
                    return 1;
                }
                SimulatedPlayer class136VarSimulateLocalPlayer = SimulatedPlayer.simulateLocalPlayer(2);
                return (class136VarSimulateLocalPlayer.onGround || class136VarSimulateLocalPlayer.touchingWater) ? 1 : 2;
            default:
                return 1;
        }
    }

    public static void addTask(Rotation class007Var, Runnable runnable) {
        addTask(class007Var, true, runnable);
    }

    public static void addTask(Rotation class007Var, boolean z, Runnable runnable) {
        if (!z) {
            ActionScheduler class265VarAddTickStep = script.addTickStep(0, () -> {
            });
            Objects.requireNonNull(runnable);
            class265VarAddTickStep.addTickStep(1, runnable::run);
            return;
        }
        if (ServerUtil.isConnectedToServer("spookytime")) {
            SimulatedPlayer class136VarSimulateLocalPlayer = SimulatedPlayer.simulateLocalPlayer(2);
            if (!class136VarSimulateLocalPlayer.onGround && !class136VarSimulateLocalPlayer.touchingWater) {
                ActionScheduler class265VarAddTickStep2 = script.addTickStep(0, () -> {
                    disableMoveKeys();
                });
                Objects.requireNonNull(runnable);
                class265VarAddTickStep2.addTickStep(1, runnable::run).addTickStep(2, GrimDelayHandler::enableMoveKeys);
                return;
            }
        }
        script.addTickStep(0, () -> {
            disableMoveKeys();
        }).addTickStep(1, () -> {
            runnable.run();
            enableMoveKeys();
        });
    }



    public static void disableMoveKeys() {
        canMove = false;
        releaseKeys();
    }

    public static void enableMoveKeys() {
        canMove = true;
        refreshPressedKeys();
    }

    public static void releaseKeys() {
        for (KeyBinding keyBinding : MovementInputHelper.getMovementKeys(false, true)) {
            keyBinding.setPressed(false);
        }
    }

    public static void refreshPressedKeys() {
        long handle = Mc.INSTANCE.getWindow().getHandle();
        for (KeyBinding keyBinding : MovementInputHelper.getMovementKeys(false, true)) {
            keyBinding.setPressed(InputUtil.isKeyPressed(handle, keyBinding.getDefaultKey().getCode()));
        }
    }

    public GrimDelayHandler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
