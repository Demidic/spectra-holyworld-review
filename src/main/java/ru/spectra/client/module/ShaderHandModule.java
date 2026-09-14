package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.HandShaderRenderer;
import ru.spectra.client.type.HandShaderMode;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class ShaderHandModule extends Module {
    public final ModeSetting<HandShaderMode> mode = new ModeSetting<HandShaderMode>(
            Translation.clearText("Mode")).values(HandShaderMode.class);
    public final NumberSetting intensity = number("Intensity", 0.78f, 0.1f, 1.25f, 0.01f);
    public final NumberSetting speed = number("Speed", 0.85f, 0.15f, 2.0f, 0.05f);
    public final NumberSetting patternScale = number("Pattern scale", 1.0f, 0.45f, 2.2f, 0.05f);
    public final BooleanSetting smokeEnabled = new BooleanSetting(
            Translation.clearText("Smoke"),
            Translation.clearText("Adds animated smoke around the hand and held item")
    ).setValue(true);
    public final ColorSetting smokeColor = new ColorSetting(
            Translation.clearText("Smoke color"),
            Translation.clearText("Changes the color and opacity of the contour smoke")
    ).value(0xFF8C7CFF).visible(smokeEnabled::isValue);
    public final NumberSetting smokeAmount =
            number("Edge smoke", 0.62f, 0.0f, 1.0f, 0.05f).visible(smokeEnabled::isValue);
    public final NumberSetting smokeLength =
            number("Smoke length", 0.58f, 0.1f, 1.0f, 0.05f).visible(smokeEnabled::isValue);
    public final NumberSetting smokePersistence =
            number("Smoke persistence", 0.91f, 0.65f, 0.97f, 0.01f).visible(smokeEnabled::isValue);

    public final BooleanSetting useItemColor = new BooleanSetting(Translation.clearText("Item color")).setValue(true);
    public final BooleanSetting useThemeColor = new BooleanSetting(Translation.clearText("Client color")).setValue(false);

    private final HandShaderRenderer renderer = new HandShaderRenderer();

    public ShaderHandModule() {
        super(ModuleTab.RENDER, "Shader Hand");
        mode.select(HandShaderMode.NEBULA);
        addSettings(mode, intensity, speed, patternScale,
                smokeEnabled, smokeColor, smokeAmount, smokeLength, smokePersistence,
                useItemColor, useThemeColor);
    }

    public boolean beginHandCapture() {
        try {
            renderer.beginHandCapture();
            return true;
        } catch (RuntimeException error) {
            disableAfterRenderFailure(error);
            return false;
        }
    }

    public void beginIrisHandCapture() {
        try {
            renderer.beginBoundHandCapture();
        } catch (RuntimeException error) {
            disableAfterRenderFailure(error);
        }
    }

    public void finishIrisHandCapture() {
        try {
            renderer.finishHandCaptureForDeferredRender();
        } catch (RuntimeException error) {
            disableAfterRenderFailure(error);
        }
    }

    public boolean hasDeferredHandCapture() {
        return renderer.hasDeferredHandCapture();
    }

    public void renderDeferredHandCapture() {
        try {
            renderer.renderDeferredHandCapture(this);
        } catch (RuntimeException error) {
            disableAfterRenderFailure(error);
        }
    }

    public void finishHandCaptureAndRender() {
        try {
            renderer.finishHandCaptureAndRender(this);
        } catch (RuntimeException error) {
            disableAfterRenderFailure(error);
        }
    }

    public void abortHandCapture() {
        renderer.abortHandCapture();
    }

    private void disableAfterRenderFailure(RuntimeException error) {
        renderer.invalidateState();
        Spectra.LOGGER.error("Shader Hand framebuffer pass failed; disabling the effect safely", error);
        if (isState()) {
            setState(false);
        }
    }

    public float[] glowColor() {
        int color;
        if (useThemeColor.isValue()) {
            color = Spectra.INSTANCE.theme().palette().accent().argb();
        } else if (useItemColor.isValue()) {
            color = heldItemColor();
        } else {
            color = 0xFF6633FF;
        }
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f
        };
    }

    public float[] smokeColor() {
        int color = smokeColor.getColor();
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                ((color >>> 24) & 0xFF) / 255.0f
        };
    }

    private int heldItemColor() {
        if (ru.spectra.client.type.Mc.INSTANCE.getPlayer() == null) {
            return 0xFFFFFFFF;
        }
        ItemStack stack = ru.spectra.client.type.Mc.INSTANCE.getPlayer().getMainHandStack();
        if (stack.isEmpty()) {
            stack = ru.spectra.client.type.Mc.INSTANCE.getPlayer().getOffHandStack();
        }
        if (stack.isEmpty()) {
            return 0xFFFFFFFF;
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.contains("diamond")) return 0xFF55DDE0;
        if (path.contains("netherite")) return 0xFF5B4A67;
        if (path.contains("gold")) return 0xFFFFD45A;
        if (path.contains("iron")) return 0xFFD8DEE8;
        if (path.contains("emerald")) return 0xFF35D06F;
        if (path.contains("redstone")) return 0xFFE23B3B;
        if (path.contains("lapis")) return 0xFF3156D4;
        return 0xFFE6E6E6;
    }

    @Override
    public void deactivate() {
        renderer.invalidateState();
        super.deactivate();
    }

    private static NumberSetting number(String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name)).range(min, max).currentValue(value).step(step);
    }
}
