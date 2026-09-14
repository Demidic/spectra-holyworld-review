package ru.spectra.client.math;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.type.Mc;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Rotation {
    public float yaw;
    public float pitch;
    public static Rotation ZERO = new Rotation(0.0f, 0.0f);

    public static Rotation playerRotation() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        return player == null ? ZERO : new Rotation(player.getYaw(), player.getPitch());
    }

    public Rotation add(float f, float f2) {
        return new Rotation(this.yaw + f, MathUtil.clamp(this.pitch + f2, -90.0f, 90.0f));
    }

    public Vec3d getDirectionVector() {
        return RotationMath.INSTANCE.rotationToVector(this);
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public Rotation(float f, float f2) {
        this.yaw = f;
        this.pitch = f2;
    }
}
