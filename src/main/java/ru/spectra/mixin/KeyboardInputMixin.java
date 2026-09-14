package ru.spectra.mixin;

import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.math.DirectionalInput;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.MovementUpdateEvent;
import ru.spectra.client.event.MovementUpdateSource;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin extends InputMixin {

    @Shadow
    @Final
    private GameOptions settings;

    @ModifyExpressionValue(method = {"tick"}, at = {@At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;")})
    private PlayerInput modifyInput(PlayerInput playerInput) {
        MovementInputEvent class040Var = new MovementInputEvent(DirectionalInput.fromInput(playerInput), playerInput.jump(), playerInput.sneak(), playerInput.sprint());
        Spectra.INSTANCE.eventDispatcher().dispatch(class040Var);
        DirectionalInput class041VarTransformDirection = class040Var.getInput();
        MovementUpdateEvent class308Var = new MovementUpdateEvent(class041VarTransformDirection, playerInput.sprint(), MovementUpdateSource.INPUT);
        Spectra.INSTANCE.eventDispatcher().dispatch(class308Var);
        return new PlayerInput(class041VarTransformDirection.forward(), class041VarTransformDirection.backward(), class041VarTransformDirection.left(), class041VarTransformDirection.right(), class040Var.isJumping(), class040Var.isSneaking(), class308Var.isSprint());
    }

}
