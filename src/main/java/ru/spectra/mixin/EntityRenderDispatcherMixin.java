package ru.spectra.mixin;

import ru.spectra.client.module.SelfNameTagModule;
import ru.spectra.client.Spectra;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
    @WrapOperation(method = {"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;")})
    private EntityRenderState spectra$hidePlayerLabel(EntityRenderer<?, ?> entityRenderer, Entity entity, float f, Operation<EntityRenderState> operation) {
        EntityRenderState entityRenderState = (EntityRenderState) operation.call(new Object[]{entityRenderer, entity, Float.valueOf(f)});
        if (entity == net.minecraft.client.MinecraftClient.getInstance().player
                && Spectra.INSTANCE.moduleRepository().find(SelfNameTagModule.class)
                .map(ru.spectra.client.module.Module::isState).orElse(false)) {
            entityRenderState.displayName = entity.getDisplayName();
            entityRenderState.nameLabelPos = entity.getAttachments().getPointNullable(
                    net.minecraft.entity.EntityAttachmentType.NAME_TAG,
                    0,
                    entity.getLerpedYaw(ru.spectra.client.type.Mc.INSTANCE.getTickDelta()));
        }
        return entityRenderState;
    }
}
