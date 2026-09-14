package ru.spectra.mixin;

import ru.spectra.client.module.HudModules;
import ru.spectra.client.ui.HudEditorOverlayWidget;
import ru.spectra.client.ui.HudEditorOverlays;
import ru.spectra.client.ui.HudEditorScreen;
import ru.spectra.client.ui.HudVanillaPreviewRenderer;
import ru.spectra.client.ui.WidgetStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Unique
    private boolean spectra$matrixApplied;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spectra$transformBossBars(DrawContext context, CallbackInfo ci) {
        if (HudVanillaPreviewRenderer.isRenderingPreview()) {
            return;
        }
        if (HudEditorScreen.isAdvancedOpen()) {
            ci.cancel();
            return;
        }
        HudEditorOverlayWidget overlay = HudEditorOverlays.bossBar();
        if (overlay != null && overlay.customized()) {
            overlay.refreshRuntimeLayout();
            var window = MinecraftClient.getInstance().getWindow();
            float hudToGui = (float) (WidgetStack.HUD_SCALE / Math.max(1.0d, window.getScaleFactor()));
            float defaultX = window.getScaledWidth() / 2.0f - 91.0f;
            float defaultY = 3.0f;
            float desiredX = overlay.contentX() * hudToGui;
            float desiredY = overlay.contentY() * hudToGui;
            context.getMatrices().push();
            context.getMatrices().translate(desiredX, desiredY, 0.0f);
            context.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
            context.getMatrices().translate(-defaultX, -defaultY, 0.0f);
            this.spectra$matrixApplied = true;
            return;
        }
        float offset = HudModules.watermarkBossBarOffset();
        this.spectra$matrixApplied = offset > 0.01f;
        if (this.spectra$matrixApplied) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0f, offset, 0.0f);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void spectra$restoreBossBarMatrix(DrawContext context, CallbackInfo ci) {
        if (this.spectra$matrixApplied) {
            context.getMatrices().pop();
            this.spectra$matrixApplied = false;
        }
    }
}
