package ru.spectra.client.module;

import ru.spectra.client.Lang;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.GammaEvent;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.event.ModuleStateEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.MathUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

@Aliases(aliases = {"Full Bright", "Gamma Boost", "Bright Vision", "Night Vision", "No Darkness", "Max Brightness", "Brightness Control", "Enhanced Gamma"})
public class FullBrightModule extends Module {
    private static volatile boolean restoreLightmapRequested;
    public final ModeSetting<BrightnessMode> modeSetting;
    public final BooleanSetting dynamicSetting;
    public final NumberSetting brightnessSetting;
    public final NumberSetting minBrightnessSetting;
    public final NumberSetting maxBrightnessSetting;
    public float smoothedLight = 1.0f;
    public static final float LIGHT_FACTOR = 0.12f;
    public final Mc mc = Mc.INSTANCE;

    public FullBrightModule() {
        super(ModuleTab.RENDER, "Full Bright");
        this.modeSetting = new ModeSetting<BrightnessMode>(
                Translation.clearText("Mode"),
                Translation.clearText("Full Bright makes every area bright; Night Vision preserves local light differences")
        ).values(BrightnessMode.class).currentValue(BrightnessMode.FULL_BRIGHT);
        this.dynamicSetting = new BooleanSetting(
                Lang.FULLBRIGHT_DYNAMIC,
                Lang.FULLBRIGHT_DYNAMIC_DESC
        ).visible(this::isFullBrightMode);
        this.brightnessSetting = new NumberSetting(Lang.FULLBRIGHT_BRIGHTNESS)
                .range(1.0f, 10.0f)
                .currentValue(10.0f)
                .step(0.05f)
                .visible(() -> isFullBrightMode() && !this.dynamicSetting.isValue());
        this.minBrightnessSetting = new NumberSetting(Lang.FULLBRIGHT_MIN_BRIGHTNESS)
                .range(1.0f, 10.0f)
                .currentValue(1.0f)
                .step(0.05f)
                .visible(() -> isFullBrightMode() && this.dynamicSetting.isValue());
        this.maxBrightnessSetting = new NumberSetting(Lang.FULLBRIGHT_MAX_BRIGHTNESS)
                .range(1.0f, 10.0f)
                .currentValue(10.0f)
                .step(0.05f)
                .visible(() -> isFullBrightMode() && this.dynamicSetting.isValue());
        addSettings(
                this.modeSetting,
                this.brightnessSetting,
                this.dynamicSetting,
                this.minBrightnessSetting,
                this.maxBrightnessSetting
        );

        register(PlayerTickEvent.class, event -> updateAmbientLight());
        register(ClientTickEvent.class, event -> {
            if (isState() && this.mc.isWorldLoaded()) {
                // Gamma is sampled only while the lightmap is dirty. Keep it
                // dirty while Full Bright is active so toggling the module or
                // changing dimensions cannot leave a stale lightmap behind.
                markLightmapDirty();
            }
        });
        register(ModuleStateEvent.class, event -> {
            if (event.module() != this) {
                return;
            }
            // Old configs used 0.5 as the default, which is darker than
            // vanilla's maximum gamma and made an enabled Full Bright appear
            // broken. Migrate that stale value once on activation.
            if (event.moduleState() && this.brightnessSetting.currentValue() < 1.0f) {
                this.brightnessSetting.setCurrentValue(10.0f);
            }
            markLightmapDirty();
        });
        register(GammaEvent.class, event -> {
            if (!isState() || !isFullBrightMode() || !this.mc.isWorldLoaded()) {
                // Let Minecraft read its own gamma option. Do not keep
                // overriding it during a fade after the module is disabled.
                return;
            }
            event.setGamma(currentFullBrightGamma());
            event.cancel();
        });
    }

    private void updateAmbientLight() {
        if (!isState() || !isFullBrightMode() || !this.mc.isWorldLoaded()) {
            return;
        }
        ClientPlayerEntity player = this.mc.getPlayer();
        World world = player.getWorld();
        BlockPos eye = BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ());
        DimensionType dimension = world.getDimension();
        float block = LightmapTextureManager.getBrightness(
                dimension,
                world.getLightLevel(LightType.BLOCK, eye)
        );
        float sky = LightmapTextureManager.getBrightness(
                dimension,
                world.getLightLevel(LightType.SKY, eye)
        );
        float light = Math.max(block, sky);
        this.smoothedLight = this.smoothedLight * (1.0f - LIGHT_FACTOR)
                + light * LIGHT_FACTOR;
    }

    private void markLightmapDirty() {
        if (this.mc.getMinecraft() == null || this.mc.getGameRenderer() == null) {
            return;
        }
        this.mc.getGameRenderer().getLightmapTextureManager().tick();
    }

    @Override
    public void activate() {
        if (this.brightnessSetting.currentValue() < 1.0f) {
            this.brightnessSetting.setCurrentValue(10.0f);
        }
        this.smoothedLight = 1.0f;
        markLightmapDirty();
        super.activate();
    }

    @Override
    public void deactivate() {
        // update() is still invoked every frame, but BadOptimizations cancels
        // ordinary lightmap tick() calls. Ask the lightmap mixin to mark one
        // frame dirty after Full Bright is already off so vanilla values are
        // regenerated immediately.
        restoreLightmapRequested = true;
        markLightmapDirty();
        super.deactivate();
    }

    public static boolean consumeRestoreLightmapRequest() {
        if (!restoreLightmapRequested) {
            return false;
        }
        restoreLightmapRequested = false;
        return true;
    }

    public float computeDynamicGamma(float light) {
        float minimum = Math.max(1.0f, this.minBrightnessSetting.currentValue());
        float maximum = Math.max(minimum, this.maxBrightnessSetting.currentValue());
        float darkness = 1.0f - MathUtil.clamp(light, 0.0f, 1.0f);
        return minimum + (maximum - minimum) * darkness;
    }

    public float currentFullBrightGamma() {
        return this.dynamicSetting.isValue()
                ? computeDynamicGamma(this.smoothedLight)
                : Math.max(1.0f, this.brightnessSetting.currentValue());
    }

    public boolean isFullBrightMode() {
        return this.modeSetting.isSelected(BrightnessMode.FULL_BRIGHT);
    }

    public boolean isNightVisionMode() {
        return this.modeSetting.isSelected(BrightnessMode.NIGHT_VISION);
    }

    public enum BrightnessMode implements DisplayNamed {
        FULL_BRIGHT("Full Bright"),
        NIGHT_VISION("Night Vision");

        private final Translation displayName;

        BrightnessMode(String displayName) {
            this.displayName = Translation.clearText(displayName);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }
}
