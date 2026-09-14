package ru.spectra.mixin;

import ru.spectra.client.event.SoundPlayEvent;
import ru.spectra.client.Spectra;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SoundManager.class})
public class SoundManagerMixin {
    @Inject(method = {"play(Lnet/minecraft/client/sound/SoundInstance;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void spectra$onPlayNoDelay(SoundInstance soundInstance, CallbackInfo callbackInfo) {
        if (soundInstance == null) {
            return;
        }
        SoundPlayEvent class017Var = new SoundPlayEvent(soundInstance);
        Spectra.INSTANCE.eventDispatcher().dispatch(class017Var);
        if (class017Var.isCancelled()) {
            callbackInfo.cancel();
        } else {
            if (class017Var.getVolumeMultiplier() == 1.0f || !(soundInstance instanceof AbstractSoundInstance)) {
                return;
            }
            ((AbstractSoundInstance) soundInstance).volume *= class017Var.getVolumeMultiplier();
        }
    }

    @Inject(method = {"play(Lnet/minecraft/client/sound/SoundInstance;I)V"}, at = {@At("HEAD")}, cancellable = true)
    private void spectra$onPlayWithDelay(SoundInstance soundInstance, int i, CallbackInfo callbackInfo) {
        if (soundInstance == null) {
            return;
        }
        SoundPlayEvent class017Var = new SoundPlayEvent(soundInstance);
        Spectra.INSTANCE.eventDispatcher().dispatch(class017Var);
        if (class017Var.isCancelled()) {
            callbackInfo.cancel();
        } else {
            if (class017Var.getVolumeMultiplier() == 1.0f || !(soundInstance instanceof AbstractSoundInstance)) {
                return;
            }
            ((AbstractSoundInstance) soundInstance).volume *= class017Var.getVolumeMultiplier();
        }
    }
}
