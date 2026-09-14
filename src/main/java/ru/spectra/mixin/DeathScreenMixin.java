package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.DeathTickEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DeathScreen.class})
public class DeathScreenMixin {

    @Shadow
    private int ticksSinceDeath;

    @Inject(method = {"render"}, at = {@At("HEAD")})
    public void render(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new DeathTickEvent(this.ticksSinceDeath));
    }
}
