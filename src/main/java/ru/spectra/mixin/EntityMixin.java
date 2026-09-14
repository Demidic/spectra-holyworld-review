package ru.spectra.mixin;

import ru.spectra.client.event.RotationVectorEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.EntityInvisibilityEvent;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import ru.spectra.client.event.MovementYawEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Entity.class})
public abstract class EntityMixin {

    @org.spongepowered.asm.mixin.Shadow
    protected abstract Vec3d getRotationVector(float f, float f2);

    @Inject(method = {"isGlowing"}, at = {@At("HEAD")}, cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.GLOWING);
        Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
        if (class252Var.isCancelled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = {"getRotationVector()Lnet/minecraft/util/math/Vec3d;"}, at = {@At("HEAD")}, cancellable = true)
    protected void getRotationVector(CallbackInfoReturnable<Vec3d> callbackInfoReturnable) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ClientPlayerEntity) {
            RotationVectorEvent class150Var = new RotationVectorEvent(entity.getYaw(), entity.getPitch());
            Spectra.INSTANCE.eventDispatcher().dispatch(class150Var);
            callbackInfoReturnable.setReturnValue(getRotationVector(class150Var.getPitch(), class150Var.getYaw()));
        }
    }

    @Inject(method = {"getRotationVec"}, at = {@At("HEAD")}, cancellable = true)
    protected void getRotationVec(float f, CallbackInfoReturnable<Vec3d> callbackInfoReturnable) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ClientPlayerEntity) {
            RotationVectorEvent class150Var = new RotationVectorEvent(entity.getYaw(), entity.getPitch());
            Spectra.INSTANCE.eventDispatcher().dispatch(class150Var);
            callbackInfoReturnable.setReturnValue(getRotationVector(class150Var.getPitch(), class150Var.getYaw()));
        }
    }

    @ModifyArgs(method = {"updateVelocity"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public void updateVelocityArgs(Args args) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ClientPlayerEntity) {
            MovementYawEvent class257Var = new MovementYawEvent(entity.getYaw());
            Spectra.INSTANCE.eventDispatcher().dispatch(class257Var);
            args.set(2, Float.valueOf(class257Var.getYaw()));
        }
    }

    @Inject(at = {@At("RETURN")}, method = {"isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"}, cancellable = true)
    private void onIsInvisibleTo(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Entity entity;
        if (callbackInfoReturnable.getReturnValueZ() && (entity = (Entity) (Object) this) != null) {
            EntityInvisibilityEvent class198Var = new EntityInvisibilityEvent(entity);
            Spectra.INSTANCE.eventDispatcher().dispatch(class198Var);
            if (class198Var.isCancelled()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

}
