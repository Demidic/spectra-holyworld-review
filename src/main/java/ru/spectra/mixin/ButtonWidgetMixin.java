package ru.spectra.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ButtonWidget.Builder.class})
public class ButtonWidgetMixin {

    @Shadow
    @Final
    private Text message;

    @Shadow
    private int x;

    @Inject(method = {"position"}, at = {@At("TAIL")})
    private void builderHook(int i, int i2, CallbackInfoReturnable<ButtonWidget.Builder> callbackInfoReturnable) {
        if (this.message.getString().equals("ViaFabricPlus")) {
            this.x = 5;
        }
    }
}
