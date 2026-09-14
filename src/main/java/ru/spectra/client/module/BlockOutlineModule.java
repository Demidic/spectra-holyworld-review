package ru.spectra.client.module;

import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.BlockShaderRenderer;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.ShaderEffectMode;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.ColorUtil;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public final class BlockOutlineModule extends Module {
    public final ModeSetting<ShaderEffectMode> mode = new ModeSetting<ShaderEffectMode>(
            Translation.clearText("Mode")).values(ShaderEffectMode.class);
    public final ColorSetting color = new ColorSetting(Translation.clearText("Color")).value(0xFF8C7CFF);
    public final BooleanSetting fill = new BooleanSetting(Translation.clearText("Fill")).setValue(false)
            .visible(() -> mode.isSelected(ShaderEffectMode.CLASSIC));
    public final BooleanSetting throughWalls = new BooleanSetting(Translation.clearText("Through walls"));
    public final NumberSetting lineWidth = number("Line width", 2.0f, 0.5f, 5.0f, 0.1f);
    public final NumberSetting fillOpacity = number("Fill opacity", 0.3f, 0.05f, 1.0f, 0.05f)
            .visible(() -> mode.isSelected(ShaderEffectMode.CLASSIC) && fill.isValue());
    public final NumberSetting outlineOpacity = number("Outline opacity", 1.0f, 0.05f, 1.0f, 0.05f);
    public final NumberSetting shaderOpacity = number("Shader opacity", 0.75f, 0.05f, 1.0f, 0.05f)
            .visible(() -> !mode.isSelected(ShaderEffectMode.CLASSIC));
    public final BooleanSetting animation = new BooleanSetting(Translation.clearText("Animation")).setValue(true)
            .visible(() -> !mode.isSelected(ShaderEffectMode.CLASSIC));
    public final NumberSetting animationSpeed = number("Animation speed", 1.0f, 0.1f, 5.0f, 0.1f)
            .visible(() -> !mode.isSelected(ShaderEffectMode.CLASSIC) && animation.isValue());
    public final BooleanSetting smoothSwitch = new BooleanSetting(Translation.clearText("Smooth block switch"))
            .setValue(true);
    public final NumberSetting switchSpeed = number("Switch speed", 6.0f, 1.0f, 15.0f, 0.5f)
            .visible(smoothSwitch::isValue);

    private final BlockShaderRenderer shaderRenderer = new BlockShaderRenderer();
    private Box animatedBox;
    private long lastFrameNanos;

    public BlockOutlineModule() {
        super(ModuleTab.RENDER, "Block Outline");
        addSettings(mode, color, fill, throughWalls, lineWidth, fillOpacity, outlineOpacity,
                shaderOpacity, animation, animationSpeed, smoothSwitch, switchSpeed);
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            animatedBox = null;
            lastFrameNanos = 0L;
            return;
        }
        HitResult hit = Mc.INSTANCE.getCrosshairTarget();
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            animatedBox = null;
            return;
        }
        Box targetBox = getBlockBox(blockHit.getBlockPos());
        animatedBox = animateBox(targetBox);
        int outline = ColorUtil.replAlpha(color.getColor(), Math.round(outlineOpacity.currentValue() * 255.0f));
        MatrixStack matrices = event.matrixStack();
        if (mode.isSelected(ShaderEffectMode.CLASSIC)) {
            if (fill.isValue()) {
                int fillColor = ColorUtil.replAlpha(color.getColor(), Math.round(fillOpacity.currentValue() * 255.0f));
                ShapeRenderer.INSTANCE.addBox(matrices.peek().getPositionMatrix(), animatedBox, fillColor,
                        !throughWalls.isValue(), true);
            }
        } else {
            Box cameraRelative = animatedBox.offset(Mc.INSTANCE.getCamera().getPos().negate());
            float time = animation.isValue()
                    ? (System.currentTimeMillis() % 1_000_000L) / 1000.0f * animationSpeed.currentValue() : 0.0f;
            shaderRenderer.render(cameraRelative, event.modelViewMatrix(), event.projectionMatrix(),
                    color.getColor(), shaderOpacity.currentValue(), time, mode.currentValue(),
                    throughWalls.isValue());
        }
        ShapeRenderer.INSTANCE.addOutline(matrices.peek().getPositionMatrix(), animatedBox, outline,
                lineWidth.currentValue(), !throughWalls.isValue(), true);
    }

    private Box getBlockBox(BlockPos pos) {
        BlockState state = Mc.INSTANCE.getWorld().getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(Mc.INSTANCE.getWorld(), pos);
        if (shape.isEmpty()) {
            return new Box(pos).expand(0.002);
        }
        return shape.getBoundingBox().offset(pos).expand(0.002);
    }

    private Box animateBox(Box target) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 1.0f / 60.0f
                : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        if (!smoothSwitch.isValue() || animatedBox == null) {
            return target;
        }
        float factor = MathHelper.clamp(dt * switchSpeed.currentValue(), 0.0f, 1.0f);
        return new Box(
                MathHelper.lerp(factor, animatedBox.minX, target.minX),
                MathHelper.lerp(factor, animatedBox.minY, target.minY),
                MathHelper.lerp(factor, animatedBox.minZ, target.minZ),
                MathHelper.lerp(factor, animatedBox.maxX, target.maxX),
                MathHelper.lerp(factor, animatedBox.maxY, target.maxY),
                MathHelper.lerp(factor, animatedBox.maxZ, target.maxZ));
    }

    private static NumberSetting number(String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name)).range(min, max).currentValue(value).step(step);
    }
}
