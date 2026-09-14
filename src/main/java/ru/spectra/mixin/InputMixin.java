package ru.spectra.mixin;

import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Input.class})
public class InputMixin {

    @Shadow
    public float movementForward;

    @Shadow
    public float movementSideways;
}
