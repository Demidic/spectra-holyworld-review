package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.EntityLifecycleEvent;
import ru.spectra.client.type.EntityLifecycleAction;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientEntityManager.Listener.class})
public class ClientEntityManagerMixin<T extends EntityLike> {

    @Shadow
    @Final
    private T entity;

    @Inject(method = {"<init>"}, at = {@At("TAIL")})
    protected void init(CallbackInfo callbackInfo) {
        Entity entity = (Entity) (this.entity);
        if (entity instanceof Entity) {
            Spectra.INSTANCE.eventDispatcher().dispatch(new EntityLifecycleEvent(entity, EntityLifecycleAction.ADD));
        }
    }

    @Inject(method = {"remove"}, at = {@At("HEAD")})
    public void removeEntityHook(Entity.RemovalReason removalReason, CallbackInfo callbackInfo) {
        Entity entity = (Entity) (this.entity);
        if (entity instanceof Entity) {
            Spectra.INSTANCE.eventDispatcher().dispatch(new EntityLifecycleEvent(entity, EntityLifecycleAction.REMOVE));
        }
    }
}
