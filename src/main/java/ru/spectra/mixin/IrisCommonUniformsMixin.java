package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.module.FullBrightModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes Spectra brightness modes visible to Iris shader-pack uniforms. */
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisCommonUniformsMixin {
    @ModifyReturnValue(
            method = "lambda$generalCommonUniforms$7",
            at = @At("RETURN"),
            remap = false
    )
    private static double exposeFullBrightGammaToShaderPack(double original) {
        FullBrightModule module = fullBright();
        if (module == null || !module.isState() || !module.isFullBrightMode()) {
            return original;
        }
        return Math.max(original, module.currentFullBrightGamma());
    }

    @ModifyReturnValue(
            method = "getNightVision",
            at = @At("RETURN"),
            remap = false
    )
    private static float exposeArtificialNightVisionToShaderPack(float original) {
        FullBrightModule module = fullBright();
        if (module == null || !module.isState() || !module.isNightVisionMode()) {
            return original;
        }
        return 1.0f;
    }

    private static FullBrightModule fullBright() {
        try {
            return Spectra.INSTANCE.moduleRepository().get(FullBrightModule.class);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }
}
