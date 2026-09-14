package ru.spectra.mixin;

import ru.spectra.client.module.PvPSafeModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Unique
    private boolean spectra$bypassDisconnectGuard;

    @Shadow
    private void disconnect() {
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void spectra$confirmPvpDisconnect(CallbackInfo ci) {
        if (spectra$bypassDisconnectGuard || !PvPSafeModule.shouldConfirmDisconnect()) {
            return;
        }
        ci.cancel();
        MinecraftClient client = MinecraftClient.getInstance();
        GameMenuScreen parent = (GameMenuScreen) (Object) this;
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                spectra$bypassDisconnectGuard = true;
                try {
                    disconnect();
                } finally {
                    spectra$bypassDisconnectGuard = false;
                }
            } else {
                client.setScreen(parent);
            }
        }, Text.literal("Disconnect during PvP?"),
                Text.literal("PvP cooldown is active. Disconnecting may kill your character.")));
    }
}
