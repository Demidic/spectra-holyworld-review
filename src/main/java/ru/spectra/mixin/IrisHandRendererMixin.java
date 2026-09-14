package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.module.ShaderHandModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the hand from Iris's real shader-pack rendering phase. */
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public abstract class IrisHandRendererMixin {
    @Inject(
            method = "renderSolid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/pipeline/WorldRenderingPipeline;setPhase(Lnet/irisshaders/iris/pipeline/WorldRenderingPhase;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER,
                    remap = false
            )
    )
    private void beginSpectraHandCapture(CallbackInfo callbackInfo) {
        ShaderHandModule module = Spectra.INSTANCE.moduleRepository().get(ShaderHandModule.class);
        if (module.isState()) {
            module.beginIrisHandCapture();
        }
    }

    @Inject(
            method = "renderSolid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/pipeline/WorldRenderingPipeline;setPhase(Lnet/irisshaders/iris/pipeline/WorldRenderingPhase;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE,
                    remap = false
            )
    )
    private void finishSpectraHandCapture(CallbackInfo callbackInfo) {
        ShaderHandModule module = Spectra.INSTANCE.moduleRepository().get(ShaderHandModule.class);
        if (module.isState()) {
            module.finishIrisHandCapture();
        }
    }
}
