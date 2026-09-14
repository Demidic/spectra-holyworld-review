package ru.spectra.mixin;

import ru.spectra.client.util.StencilBufferUtil;
import ru.spectra.client.event.WorldLoadEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.ClientInitEvent;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.type.Mc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public class MinecraftClientMixin {

    @Shadow
    @Nullable
    public ClientWorld world;

    @Shadow
    @Final
    private Window window;

    @Inject(method = {"tick"}, at = {@At("HEAD")})
    private void onTick(CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new ClientTickEvent());
    }

    @Inject(method = {"setWorld"}, at = {@At("HEAD")})
    private void setWorld(ClientWorld clientWorld, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new WorldLoadEvent(clientWorld));
    }

    @Inject(method = {"onResolutionChanged"}, at = {@At("RETURN")})
    public void onResolutionChanged(CallbackInfo callbackInfo) {
        StencilBufferUtil.initFramebuffer(this.window.getFramebufferWidth(), this.window.getFramebufferHeight(), Mc.INSTANCE.getFramebuffer().getColorAttachment());
    }

    @Inject(method = {"render"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;endWrite()V")})
    private void runTick(boolean z, CallbackInfo callbackInfo) throws MatchException {
        Spectra.INSTANCE.windowControllerAdapter().preBlitFramebufferToBackbuffer(Mc.INSTANCE.getWindow().getHandle());
    }

    @Inject(at = {@At("HEAD")}, method = {"stop"})
    private void stop(CallbackInfo callbackInfo) {
        Spectra.INSTANCE.discordManager().stopRPC();
        Spectra.INSTANCE.configManager().saveSessionNickname();
    }

    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    public void init(RunArgs runArgs, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new ClientInitEvent());
        Spectra.LOGGER.info("SPECTRA_CLIENT_READY");
    }
}
