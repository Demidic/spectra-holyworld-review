package ru.spectra.client.util;

import ru.spectra.client.math.Rotation;
import ru.spectra.client.type.Mc;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/** Read-only player angles for trajectory previews; no aim, spoofing or rotation tasks. */
public final class RotationManager {
    public static final RotationManager INSTANCE = new RotationManager();

    private RotationManager() {
    }

    public Rotation getCurrentRotation() {
        return Rotation.playerRotation();
    }

    public Rotation getPreviousRotation() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        return new Rotation(player.prevYaw, player.prevPitch);
    }

    public Rotation getInterpolatedRotation() {
        float delta = Mc.INSTANCE.getTickDelta();
        Rotation previous = getPreviousRotation();
        Rotation current = getCurrentRotation();
        return new Rotation(MathHelper.lerpAngleDegrees(delta, previous.getYaw(), current.getYaw()),
                MathHelper.lerp(delta, previous.getPitch(), current.getPitch()));
    }

    public Rotation getMoveRotation() {
        return getCurrentRotation();
    }
}
