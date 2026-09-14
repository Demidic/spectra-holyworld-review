package ru.spectra.mixin;

import ru.spectra.client.ui.HudEditorOverlayWidget;
import ru.spectra.client.ui.HudEditorOverlays;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Unique
    private boolean spectra$matrixApplied;

    @Inject(method = "render", at = @At("HEAD"))
    private void spectra$scalePlayerList(
            DrawContext context, int screenWidth, Scoreboard scoreboard,
            ScoreboardObjective objective, CallbackInfo ci) {
        HudEditorOverlayWidget overlay = HudEditorOverlays.playerList();
        if (overlay == null || Math.abs(overlay.scale() - 1.0f) < 0.001f) {
            return;
        }
        float centerX = screenWidth / 2.0f;
        float top = 10.0f;
        context.getMatrices().push();
        context.getMatrices().translate(centerX, top, 0.0f);
        context.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
        context.getMatrices().translate(-centerX, -top, 0.0f);
        this.spectra$matrixApplied = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void spectra$restorePlayerListMatrix(
            DrawContext context, int screenWidth, Scoreboard scoreboard,
            ScoreboardObjective objective, CallbackInfo ci) {
        if (this.spectra$matrixApplied) {
            context.getMatrices().pop();
            this.spectra$matrixApplied = false;
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void spectra$hidePlayerListBackground(
            DrawContext context, int x1, int y1, int x2, int y2, int color,
            Operation<Void> original) {
        HudEditorOverlayWidget overlay = HudEditorOverlays.playerList();
        if (overlay == null || overlay.showBackground()) {
            original.call(context, x1, y1, x2, y2, color);
        }
    }
}
