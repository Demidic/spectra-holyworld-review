package ru.spectra.mixin;

import ru.spectra.client.util.PinnedServersController;
import ru.spectra.client.Spectra;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MultiplayerScreen.class})
public class MultiplayerScreenMixin {

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    @Shadow
    private ButtonWidget buttonEdit;

    @Shadow
    private ButtonWidget buttonDelete;

    @Inject(method = {"updateButtonActivationStates"}, at = {@At("TAIL")})
    private void spectra$disableButtonsForPinned(CallbackInfo callbackInfo) {
        MultiplayerServerListWidget.Entry selected = this.serverListWidget.getSelectedOrNull();
        if (selected instanceof MultiplayerServerListWidget.ServerEntry serverEntry
                && Spectra.INSTANCE.pinnedServersController().isPinned(serverEntry.getServer().address)) {
            this.buttonEdit.active = false;
            this.buttonDelete.active = false;
        }
    }

    @Inject(method = {"removeEntry"}, at = {@At("HEAD")}, cancellable = true)
    public void spectra$removeEntry(boolean z, CallbackInfo callbackInfo) {
        MultiplayerServerListWidget.Entry selected = this.serverListWidget.getSelectedOrNull();
        PinnedServersController class028VarPinnedServersController = Spectra.INSTANCE.pinnedServersController();
        if (z
                && selected instanceof MultiplayerServerListWidget.ServerEntry serverEntry
                && class028VarPinnedServersController.isPinned(serverEntry.getServer().address)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"editEntry"}, at = {@At("HEAD")}, cancellable = true)
    public void spectra$editEntry(boolean z, CallbackInfo callbackInfo) {
        MultiplayerServerListWidget.Entry selected = this.serverListWidget.getSelectedOrNull();
        PinnedServersController class028VarPinnedServersController = Spectra.INSTANCE.pinnedServersController();
        if (z
                && selected instanceof MultiplayerServerListWidget.ServerEntry serverEntry
                && class028VarPinnedServersController.isPinned(serverEntry.getServer().address)) {
            callbackInfo.cancel();
        }
    }
}
