package ru.spectra.mixin;

import ru.spectra.client.event.PerspectiveEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.CrosshairRenderEvent;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import ru.spectra.client.ui.HudEditorOverlayWidget;
import ru.spectra.client.ui.HudEditorOverlays;
import ru.spectra.client.ui.HudEditorScreen;
import ru.spectra.client.ui.WidgetStack;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.type.Render2DStage;
import ru.spectra.client.event.StatusEffectOverlayEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyExpressionValue(method = {"tick()V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getMainHandStack()Lnet/minecraft/item/ItemStack;")})
    private ItemStack injectSilent(ItemStack itemStack) {
        return this.client.player != null ? (ItemStack) (Object) this.client.player.getInventory().main.get(Spectra.INSTANCE.inventoryService().hotbarSlotSwapper().getClientsideSlot()) : itemStack;
    }

    @Shadow
    protected abstract void renderScoreboardSidebar(DrawContext drawContext, ScoreboardObjective scoreboardObjective);

    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void preRender(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new Render2DEvent(drawContext.getMatrices(), Render2DStage.PRE, renderTickCounter, drawContext));
    }

    @Inject(method = {"renderCrosshair"}, at = {@At("HEAD")}, cancellable = true)
    private void crosshair(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        CrosshairRenderEvent class247Var = new CrosshairRenderEvent();
        Spectra.INSTANCE.eventDispatcher().dispatch(class247Var);
        if (class247Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"render"}, at = {@At("RETURN")})
    private void postRender(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new Render2DEvent(drawContext.getMatrices(), Render2DStage.POST, renderTickCounter, drawContext));
    }

    @ModifyExpressionValue(method = {"renderCrosshair"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;")})
    private Perspective hookPerspectiveEventOnCrosshair(Perspective perspective) {
        PerspectiveEvent class160Var = new PerspectiveEvent(perspective);
        Spectra.INSTANCE.eventDispatcher().dispatch(class160Var);
        return class160Var.perspective();
    }

    @ModifyExpressionValue(method = {"renderMiscOverlays"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;")})
    private Perspective hookPerspectiveEventOnMiscOverlays(Perspective perspective) {
        PerspectiveEvent class160Var = new PerspectiveEvent(perspective);
        Spectra.INSTANCE.eventDispatcher().dispatch(class160Var);
        return class160Var.perspective();
    }

    @Inject(method = {"renderStatusEffectOverlay"}, at = {@At("HEAD")}, cancellable = true)
    private void renderStatusEffectOverlay(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        StatusEffectOverlayEvent class315Var = new StatusEffectOverlayEvent();
        Spectra.INSTANCE.eventDispatcher().dispatch(class315Var);
        if (class315Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"renderVignetteOverlay"}, at = {@At("HEAD")}, cancellable = true)
    private void renderVignetteOverlay(DrawContext drawContext, Entity entity, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.VIGNETTE);
        Spectra.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"))
    private void renderScoreboard(InGameHud inGameHud, DrawContext drawContext, ScoreboardObjective scoreboardObjective) {
        if (HudEditorScreen.isAdvancedOpen()) {
            return;
        }
        RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.SCOREBOARD);
        Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
        if (class252Var.isCancelled()) {
            return;
        }
        HudEditorOverlayWidget overlay = HudEditorOverlays.scoreboard();
        if (overlay == null || !overlay.customized()) {
            renderScoreboardSidebar(drawContext, scoreboardObjective);
            return;
        }
        overlay.refreshRuntimeLayout();
        var window = MinecraftClient.getInstance().getWindow();
        float hudToGui = (float) (WidgetStack.HUD_SCALE / Math.max(1.0d, window.getScaleFactor()));
        float defaultRight = window.getScaledWidth() - 1.0f;
        float defaultCenterY =
                ru.spectra.client.ui.HudVanillaPreviewRenderer.scoreboardDefaultCenterY(
                        window.getScaledHeight(), scoreboardObjective);
        float desiredRight = (overlay.contentX() + overlay.contentWidth()) * hudToGui;
        float desiredCenterY = (overlay.contentY() + overlay.contentHeight() / 2.0f) * hudToGui;
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(desiredRight, desiredCenterY, 0.0f);
        drawContext.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
        drawContext.getMatrices().translate(-defaultRight, -defaultCenterY, 0.0f);
        try {
            renderScoreboardSidebar(drawContext, scoreboardObjective);
        } finally {
            drawContext.getMatrices().pop();
        }
    }

    @WrapOperation(
            method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void spectra$scoreboardBackground(
            DrawContext context, int x1, int y1, int x2, int y2, int color,
            Operation<Void> original) {
        HudEditorOverlayWidget overlay = HudEditorOverlays.scoreboard();
        if (overlay == null || overlay.showBackground()) {
            original.call(context, x1, y1, x2, y2, color);
        }
    }

}
