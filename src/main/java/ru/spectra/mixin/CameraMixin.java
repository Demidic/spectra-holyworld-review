package ru.spectra.mixin;

import ru.spectra.client.event.CameraRotationEvent;
import ru.spectra.client.Spectra;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Camera.class})
public abstract class CameraMixin {

    @Redirect(method = {"update"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1))
    private void update(Camera camera, float f, float f2) {
        CameraRotationEvent class152Var = new CameraRotationEvent(f, f2);
        Spectra.INSTANCE.eventDispatcher().dispatch(class152Var);
        if (class152Var.isCancelled()) {
            camera.setRotation(class152Var.getYaw(), class152Var.getPitch());
        } else {
            camera.setRotation(f, f2);
        }
    }
}
