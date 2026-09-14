package ru.spectra.client.math;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationMath {
    public static final RotationMath INSTANCE = new RotationMath();

    public Vec3d rotationToVector(Rotation class007Var) {
        float radians = (float) Math.toRadians(class007Var.getPitch());
        float radians2 = (float) Math.toRadians(-class007Var.getYaw());
        float fCos = MathHelper.cos(radians2);
        float fSin = MathHelper.sin(radians2);
        float fCos2 = MathHelper.cos(radians);
        return new Vec3d(fSin * fCos2, -MathHelper.sin(radians), fCos * fCos2);
    }

}
