package ru.spectra.client.module;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.SkyEffectRenderer;
import ru.spectra.client.render.BlurFogRenderer;
import ru.spectra.client.type.CustomSkyMode;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.SkyShaderMode;
import ru.spectra.client.type.WorldTime;
import ru.spectra.client.type.WorldWeather;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ExpandableSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Aliases(aliases = {"World Tweaks", "Ambience", "Fog Color", "Custom Fog", "Sky Color", "World Time", "Custom Sky"})
public class WorldTweaksModule extends Module {
    public final ExpandableSetting changeTime =
            new ExpandableSetting(Lang.WORLD_TWEAKS_CHANGE_TIME, Lang.WORLD_TWEAKS_CHANGE_TIME_DESC);
    public final ModeSetting<WorldTime> timeOfDay =
            new ModeSetting<WorldTime>(Lang.WORLD_TWEAKS_TIME_OF_DAY)
                    .values(WorldTime.class).currentValue(WorldTime.NIGHT);

    public final ExpandableSetting changeWeather = new ExpandableSetting(
            Translation.clearText("Change weather"),
            Translation.clearText("Overrides the visible world weather")
    );
    public final ModeSetting<WorldWeather> weather = new ModeSetting<WorldWeather>(
            Translation.clearText("Weather"),
            Translation.clearText("Selects clear, rainy or thunder weather")
    ).values(WorldWeather.class);

    public final ExpandableSetting changeFogColor =
            new ExpandableSetting(Lang.WORLD_TWEAKS_FOG_COLOR, Lang.WORLD_TWEAKS_FOG_COLOR_DESC);
    public final NumberSetting fogDistance =
            new NumberSetting(Lang.WORLD_TWEAKS_FOG_COLOR_DISTANCE,
                    Lang.WORLD_TWEAKS_FOG_COLOR_DISTANCE_DESC)
                    .currentValue(90.0f).range(10.0f, 256.0f).step(1.0f);
    public final BooleanSetting blurFog = new BooleanSetting(
            Translation.clearText("Blur fog"),
            Translation.clearText("Blurs distant scenery instead of replacing it with a solid fog color")
    );
    public final ColorSetting fogColor =
            new ColorSetting(Lang.WORLD_TWEAKS_FOG_COLOR_COLOR,
                    Lang.WORLD_TWEAKS_FOG_COLOR_COLOR_DESC).setColor(7238883)
                    .visible(() -> !this.blurFog.isValue());

    public final ExpandableSetting changeSky = new ExpandableSetting(
            Translation.clearText("Change Sky"),
            Translation.clearText("Changes the sky color or applies a sky shader")
    );
    public final ModeSetting<CustomSkyMode> skyMode =
            new ModeSetting<CustomSkyMode>(Translation.clearText("Sky mode"))
                    .values(CustomSkyMode.class);
    public final ColorSetting skyColor =
            new ColorSetting(Translation.clearText("Sky color"),
                    Translation.clearText("Sets the custom sky color"))
                    .value(0xFF3366FF)
                    .visible(() -> this.changeSky.isValue()
                            && this.skyMode.isSelected(CustomSkyMode.CUSTOM));
    public final ModeSetting<SkyShaderMode> skyShader =
            new ModeSetting<SkyShaderMode>(Translation.clearText("Sky shader"),
                    Translation.clearText("Selects the animated sky shader"))
                    .values(SkyShaderMode.class)
                    .visible(() -> this.changeSky.isValue()
                            && this.skyMode.isSelected(CustomSkyMode.SHADERS));
    public final NumberSetting shaderOpacity =
            number("Shader opacity", 0.68f, 0.1f, 1.0f, 0.05f)
                    .visible(() -> this.changeSky.isValue()
                            && this.skyMode.isSelected(CustomSkyMode.SHADERS));
    public final NumberSetting animationSpeed =
            number("Animation speed", 1.0f, 0.1f, 20.0f, 0.1f)
                    .visible(() -> this.changeSky.isValue()
                            && this.skyMode.isSelected(CustomSkyMode.SHADERS));
    public final NumberSetting skyPatternScale =
            number("Pattern scale", 1.0f, 0.35f, 2.5f, 0.05f)
                    .visible(() -> this.changeSky.isValue()
                            && this.skyMode.isSelected(CustomSkyMode.SHADERS));

    private final Mc mc = Mc.INSTANCE;
    private final SkyEffectRenderer skyRenderer = new SkyEffectRenderer();
    private final BlurFogRenderer blurFogRenderer = new BlurFogRenderer();
    private double skyAnimationTime;
    private long lastSkyFrameNanos;

    public WorldTweaksModule() {
        super(ModuleTab.RENDER, "World Tweaks");
        this.changeTime.setSubSettings(List.of(this.timeOfDay));
        this.changeWeather.setSubSettings(List.of(this.weather));
        this.changeFogColor.setSubSettings(List.of(this.blurFog, this.fogColor, this.fogDistance));
        this.changeSky.setSubSettings(List.of(
                this.skyMode, this.skyColor, this.skyShader,
                this.shaderOpacity, this.animationSpeed, this.skyPatternScale
        ));
        addSettings(this.changeTime, this.changeWeather, this.changeFogColor, this.changeSky);

        register(PacketReceiveEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || !this.changeTime.isValue()
                    || !(event.getPacket() instanceof WorldTimeUpdateS2CPacket packet)) {
                return;
            }
            WorldTime selectedTime = this.timeOfDay.currentValue();
            if (selectedTime.ticks() != -1) {
                packet.timeOfDay = selectedTime.ticks();
            }
        });
        register(WorldRenderEvent.class, event -> applyWeather());
    }

    private void applyWeather() {
        if (!isState() || !this.changeWeather.isValue() || !this.mc.isWorldLoaded()) {
            return;
        }
        switch (this.weather.currentValue()) {
            case CLEAR -> {
                this.mc.getWorld().setRainGradient(0.0f);
                this.mc.getWorld().setThunderGradient(0.0f);
            }
            case RAIN -> {
                this.mc.getWorld().setRainGradient(1.0f);
                this.mc.getWorld().setThunderGradient(0.0f);
            }
            case THUNDER -> {
                this.mc.getWorld().setRainGradient(1.0f);
                this.mc.getWorld().setThunderGradient(1.0f);
            }
        }
    }

    public int modifySkyColor(int originalColor) {
        if (!isState() || !this.changeSky.isValue()) {
            return originalColor;
        }
        if (!this.skyMode.isSelected(CustomSkyMode.SHADERS)) {
            return selectedSkyColor();
        }
        int base = shaderBaseColor();
        float blend = switch (this.skyShader.currentValue()) {
            case STORM_VEIL -> 0.72f;
            case NORTHERN_LIGHTS -> 0.62f;
            case ASTRAL_RIFT -> 0.58f;
            case COSMIC_NIGHT -> 0.55f;
        };
        return mixColors(originalColor, base, blend);
    }

    public void renderSkyShader(float tickDelta) {
        if (!isState() || !this.changeSky.isValue()
                || !this.skyMode.isSelected(CustomSkyMode.SHADERS)
                || !this.mc.isWorldLoaded()) {
            resetSkyAnimationClock();
            return;
        }
        long now = System.nanoTime();
        if (this.lastSkyFrameNanos == 0L) {
            this.lastSkyFrameNanos = now;
        } else {
            double deltaSeconds = MathHelper.clamp(
                    (now - this.lastSkyFrameNanos) / 1_000_000_000.0, 0.0, 0.1);
            this.lastSkyFrameNanos = now;
            this.skyAnimationTime += deltaSeconds * this.animationSpeed.currentValue();
        }
        float skyAngle = this.mc.getWorld().getSkyAngle(tickDelta);
        float brightness = 1.0f
                - (MathHelper.cos(skyAngle * MathHelper.TAU) * 2.0f + 0.25f);
        brightness = MathHelper.clamp(brightness, 0.0f, 1.0f);
        float starBrightness = brightness * brightness * 0.5f;
        float rainStrength = MathHelper.clamp(
                this.mc.getWorld().getRainGradient(tickDelta), 0.0f, 1.0f);
        this.skyRenderer.render(
                this.skyShader.currentValue(), selectedSkyColor(),
                this.shaderOpacity.currentValue(), (float) this.skyAnimationTime,
                skyAngle, starBrightness, rainStrength, this.skyPatternScale.currentValue()
        );
    }

    public boolean shouldHideDefaultStars() {
        return isState() && this.changeSky.isValue()
                && this.skyMode.isSelected(CustomSkyMode.SHADERS);
    }

    private int selectedSkyColor() {
        return switch (this.skyMode.currentValue()) {
            case STATIC -> 0xFF3366FF;
            case CUSTOM -> this.skyColor.getColor();
            case CLIENT, SHADERS -> Spectra.INSTANCE.theme().palette().accent().argb();
        };
    }

    private int shaderBaseColor() {
        return switch (this.skyShader.currentValue()) {
            case STORM_VEIL -> 0xFF0B0D18;
            case NORTHERN_LIGHTS -> 0xFF11162B;
            case ASTRAL_RIFT -> 0xFF140D20;
            case COSMIC_NIGHT -> mixColors(0xFF0F1431, selectedSkyColor(), 0.18f);
        };
    }

    private void resetSkyAnimationClock() {
        this.skyAnimationTime = 0.0d;
        this.lastSkyFrameNanos = 0L;
    }

    private static NumberSetting number(
            String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name))
                .currentValue(value).range(min, max).step(step);
    }

    private static int mixColors(int first, int second, float factor) {
        factor = MathHelper.clamp(factor, 0.0f, 1.0f);
        float inverse = 1.0f - factor;
        int red = Math.round(((first >> 16) & 0xFF) * inverse
                + ((second >> 16) & 0xFF) * factor);
        int green = Math.round(((first >> 8) & 0xFF) * inverse
                + ((second >> 8) & 0xFF) * factor);
        int blue = Math.round((first & 0xFF) * inverse
                + (second & 0xFF) * factor);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public void reload() {
        WorldRenderer renderer;
        if (this.mc.isWorldLoaded()
                && (renderer = this.mc.getMinecraft().worldRenderer) != null
                && isState()) {
            renderer.reload();
        }
    }

    public ExpandableSetting changeFogColor() {
        return this.changeFogColor;
    }

    public NumberSetting fogDistance() {
        return this.fogDistance;
    }

    public ColorSetting fogColor() {
        return this.fogColor;
    }

    public boolean shouldRenderBlurFog() {
        return isState() && this.changeFogColor.isValue() && this.blurFog.isValue()
                && this.mc.isWorldLoaded();
    }

    public void renderBlurFog(float farPlane) {
        if (!shouldRenderBlurFog()) {
            return;
        }
        try {
            this.blurFogRenderer.render(this.fogDistance.currentValue(), farPlane);
        } catch (RuntimeException error) {
            this.blurFogRenderer.release();
            this.blurFog.setValue(false);
            Spectra.LOGGER.error("Blur fog framebuffer pass failed; disabling the effect safely", error);
        }
    }

    @Override
    public void deactivate() {
        resetSkyAnimationClock();
        this.blurFogRenderer.release();
        super.deactivate();
    }
}
