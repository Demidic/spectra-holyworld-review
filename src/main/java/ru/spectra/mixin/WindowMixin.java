package ru.spectra.mixin;

import ru.spectra.client.event.WindowResizeEvent;
import ru.spectra.client.Spectra;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Window.class})
public class WindowMixin {
    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    public void init(WindowEventHandler windowEventHandler, MonitorTracker monitorTracker, WindowSettings windowSettings, @Nullable String str, String str2, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new WindowResizeEvent(windowSettings.width, windowSettings.height));
        Spectra.INSTANCE.windowControllerAdapter().handleResize(windowSettings.width, windowSettings.height);
    }

    @Inject(method = {"onWindowSizeChanged"}, at = {@At("HEAD")})
    public void onResize(long j, int i, int i2, CallbackInfo callbackInfo) {
        if (i < 64 || i2 < 64) {
            return;
        }
        Spectra.INSTANCE.windowControllerAdapter().handleResize(i, i2);
        Spectra.INSTANCE.eventDispatcher().dispatch(new WindowResizeEvent(i, i2));
    }
}
