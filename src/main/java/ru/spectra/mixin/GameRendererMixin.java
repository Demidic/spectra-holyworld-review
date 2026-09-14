package ru.spectra.mixin;

import ru.spectra.client.event.AspectRatioEvent;
import ru.spectra.client.module.AspectRatioModule;
import ru.spectra.client.module.ShaderHandModule;
import ru.spectra.client.type.Mc;
import ru.spectra.client.event.FovEvent;
import ru.spectra.client.event.PerspectiveEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.RaycastUtil;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import ru.spectra.client.event.VisualEffectEvent;
import ru.spectra.client.type.VisualEffectType;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {

    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    private float zoom;

    @Shadow
    private float zoomX;

    @Shadow
    private float zoomY;

    @Shadow
    public abstract float getFarPlaneDistance();

    @ModifyExpressionValue(method = {"renderWorld"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;")})
    private Perspective hookPerspectiveEventOnCamera(Perspective perspective) {
        PerspectiveEvent class160Var = new PerspectiveEvent(perspective);
        Spectra.INSTANCE.eventDispatcher().dispatch(class160Var);
        return class160Var.perspective();
    }

    @ModifyExpressionValue(method = {"renderHand"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;")})
    private Perspective hookPerspectiveEventOnHand(Perspective perspective) {
        PerspectiveEvent class160Var = new PerspectiveEvent(perspective);
        Spectra.INSTANCE.eventDispatcher().dispatch(class160Var);
        return class160Var.perspective();
    }

    @WrapOperation(
            method = {"renderHand"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
            )
    )
    private void captureShaderHand(HeldItemRenderer renderer, float tickDelta, MatrixStack matrices,
                                   VertexConsumerProvider.Immediate consumers, ClientPlayerEntity player, int light,
                                   Operation<Void> original) {
        ShaderHandModule shaderHand = Spectra.INSTANCE.moduleRepository().get(ShaderHandModule.class);
        if (shaderHand.hasDeferredHandCapture()) {
            original.call(renderer, tickDelta, matrices, consumers, player, light);
            return;
        }
        if (!shaderHand.isState()) {
            original.call(renderer, tickDelta, matrices, consumers, player, light);
            return;
        }
        // Flush everything submitted before the hand. This makes the two
        // framebuffer snapshots differ only by the one vanilla hand pass.
        consumers.draw();
        if (!shaderHand.beginHandCapture()) {
            original.call(renderer, tickDelta, matrices, consumers, player, light);
            return;
        }
        try {
            original.call(renderer, tickDelta, matrices, consumers, player, light);
            consumers.draw();
            shaderHand.finishHandCaptureAndRender();
        } catch (RuntimeException error) {
            shaderHand.abortHandCapture();
            throw error;
        }
    }

    @Inject(method = {"renderHand"}, at = {@At("TAIL")})
    private void renderDeferredIrisShaderHand(CallbackInfo callbackInfo) {
        ShaderHandModule shaderHand = Spectra.INSTANCE.moduleRepository().get(ShaderHandModule.class);
        if (shaderHand.isState() && shaderHand.hasDeferredHandCapture()) {
            shaderHand.renderDeferredHandCapture();
        }
    }

    @Inject(method = {"tiltViewWhenHurt"}, at = {@At("HEAD")}, cancellable = true)
    public void tiltViewWhenHurt(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.CAMERA_HURT);
        Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
        if (class252Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = {"renderWorld"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F")})
    private float hookNausea(float f) {
        VisualEffectEvent class258Var = new VisualEffectEvent(VisualEffectType.NAUSEA);
        Spectra.INSTANCE.eventDispatcher().dispatch(class258Var);
        if (class258Var.isCancelled()) {
            return 0.0f;
        }
        return f;
    }

    @ModifyExpressionValue(method = {"getFov"}, at = {@At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false)})
    private int hookGetFov(int i) {
        FovEvent class146Var = new FovEvent();
        Spectra.INSTANCE.eventDispatcher().dispatch(class146Var);
        return class146Var.isCancelled() ? class146Var.getFov() : i;
    }



    @Inject(method = {"getBasicProjectionMatrix"}, at = {@At("HEAD")}, cancellable = true)
    public void getBasicProjectionMatrix(float f, CallbackInfoReturnable<Matrix4f> callbackInfoReturnable) {
        if (!((AspectRatioModule) Spectra.INSTANCE.moduleRepository().get(AspectRatioModule.class)).isState() || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        MatrixStack matrixStack = new MatrixStack();
        Window window = this.client.getWindow();
        AspectRatioEvent class109Var = new AspectRatioEvent((float) window.getFramebufferWidth() / (float) window.getFramebufferHeight());
        Spectra.INSTANCE.eventDispatcher().dispatch(class109Var);
        matrixStack.peek().getPositionMatrix().identity();
        if (this.zoom != 1.0f) {
            matrixStack.translate(this.zoomX, -this.zoomY, 0.0f);
            matrixStack.scale(this.zoom, this.zoom, 1.0f);
        }
        matrixStack.peek().getPositionMatrix().mul(new Matrix4f().setPerspective((float) (((double) f) * 0.01745329238474369d), class109Var.getAspectRatio(), 0.05f, getFarPlaneDistance()));
        callbackInfoReturnable.setReturnValue(matrixStack.peek().getPositionMatrix());
    }
}
