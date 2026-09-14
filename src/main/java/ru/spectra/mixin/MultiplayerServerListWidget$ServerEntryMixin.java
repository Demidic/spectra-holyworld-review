package ru.spectra.mixin;

import ru.spectra.client.util.StencilBufferUtil;
import ru.spectra.client.util.PinnedServersController;
import ru.spectra.client.model.PinnedServerEntry;
import ru.spectra.client.Spectra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MultiplayerServerListWidget.ServerEntry.class})
public class MultiplayerServerListWidget$ServerEntryMixin {

    @Shadow
    @Final
    private ServerInfo server;

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = {"render"}, at = {@At("TAIL")})
    private void spectra$renderPinned(DrawContext drawContext, int i, int i2, int i3, int i4, int i5, int i6, int i7, boolean z, float f, CallbackInfo callbackInfo) {
        PinnedServersController class028VarPinnedServersController = Spectra.INSTANCE.pinnedServersController();
        if (class028VarPinnedServersController == null) {
            return;
        }
        String str = this.server.address;
        if (class028VarPinnedServersController.isPinned(str)) {
            int iSpectra$getAccentColor = spectra$getAccentColor(class028VarPinnedServersController, str);
            if (iSpectra$getAccentColor == 0) {
                iSpectra$getAccentColor = 52945;
            }
            int iSpectra$withAlpha = spectra$withAlpha(iSpectra$getAccentColor, z ? 53 : 34);
            int iSpectra$withAlpha2 = spectra$withAlpha(iSpectra$getAccentColor, 204);
            drawContext.fill(i3, i2, i3 + i4, i2 + i5, iSpectra$withAlpha);
            drawContext.fill(i3, i2, i3 + 3, i2 + i5, iSpectra$withAlpha2);
        }
    }

    @Unique
    private static int spectra$getAccentColor(PinnedServersController class028Var, String str) {
        String strSpectra$normalize = spectra$normalize(str);
        for (PinnedServerEntry class029Var : class028Var.favorites()) {
            if (spectra$normalize(class029Var.address()).equals(strSpectra$normalize)) {
                return class029Var.accentColor();
            }
        }
        return 0;
    }

    @Unique
    private static String spectra$normalize(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return !strTrim.contains(":") ? strTrim + ":25565" : strTrim;
    }

    @Unique
    private static int spectra$withAlpha(int i, int i2) {
        return ((i2 & StencilBufferUtil.STENCIL_MASK) << 24) | (i & 16777215);
    }
}
