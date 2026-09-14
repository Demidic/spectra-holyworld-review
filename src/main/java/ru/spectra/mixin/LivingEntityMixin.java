package ru.spectra.mixin;

import ru.spectra.client.event.HandSwingEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.JumpEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {

    @Unique
    private JumpEvent lastJumpEvent;

    @Shadow
    public float bodyYaw;

    @Shadow
    public abstract void setSprinting(boolean z);

    @Shadow
    protected abstract float getMaxRelativeHeadRotation();

    @Inject(method = {"jump"}, at = {@At("HEAD")})
    private void jump(CallbackInfo callbackInfo) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof ClientPlayerEntity) {
            JumpEvent class237Var = new JumpEvent(livingEntity.getYaw());
            Spectra.INSTANCE.eventDispatcher().dispatch(class237Var);
            this.lastJumpEvent = class237Var;
        }
    }

    @Inject(method = {"getHandSwingDuration"}, at = {@At("HEAD")}, cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        HandSwingEvent class087Var = new HandSwingEvent();
        Spectra.INSTANCE.eventDispatcher().dispatch(class087Var);
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (class087Var.isCancelled() && (livingEntity instanceof ClientPlayerEntity)) {
            callbackInfoReturnable.setReturnValue(Integer.valueOf((int) class087Var.swingSpeed()));
        }
    }



    @ModifyExpressionValue(method = {"jump"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F")})
    private float getYaw(float f) {
        return (this.lastJumpEvent == null || !(((LivingEntity) (Object) this) instanceof ClientPlayerEntity)) ? f : this.lastJumpEvent.getYaw();
    }

}
