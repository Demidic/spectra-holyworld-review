package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.ColorUtil;
import ru.spectra.client.util.FriendManager;
import ru.spectra.client.util.PlayerVisualFilter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class HitBoxCustomizerModule extends Module {
    public final ModeSetting<ColorMode> colorMode =
            new ModeSetting<ColorMode>(Translation.clearText("Color mode")).values(ColorMode.class);
    public final ColorSetting globalColor = color("Global color", 0xFF8C7CFF)
            .visible(() -> colorMode.isSelected(ColorMode.SINGLE));
    public final ColorSetting playerColor = color("Player color", 0xFF7CA8FF)
            .visible(() -> colorMode.isSelected(ColorMode.BY_TYPE));
    public final ColorSetting friendColor = color("Friend color", 0xFF74E6A4)
            .visible(() -> colorMode.isSelected(ColorMode.BY_TYPE));
    public final ColorSetting monsterColor = color("Monster color", 0xFFFF6868)
            .visible(() -> colorMode.isSelected(ColorMode.BY_TYPE));
    public final ColorSetting animalColor = color("Animal color", 0xFFFFD27C)
            .visible(() -> colorMode.isSelected(ColorMode.BY_TYPE));
    public final ColorSetting entityColor = color("Other entity color", 0xFFC7C7D1)
            .visible(() -> colorMode.isSelected(ColorMode.BY_TYPE));
    public final BooleanSetting fill =
            new BooleanSetting(Translation.clearText("Fill enabled")).setValue(true);
    public final BooleanSetting outline =
            new BooleanSetting(Translation.clearText("Outline enabled")).setValue(true);
    public final BooleanSetting showOwn =
            new BooleanSetting(Translation.clearText("Show own hitbox")).setValue(false);
    public final NumberSetting lineWidth =
            setting("Line width", 2.0f, 0.5f, 5.0f, 0.1f)
                    .visible(outline::isValue);
    public final NumberSetting outlineOpacity =
            setting("Outline opacity", 0.8f, 0.1f, 1.0f, 0.05f)
                    .visible(outline::isValue);
    public final NumberSetting fillOpacity =
            setting("Fill opacity", 0.3f, 0.1f, 1.0f, 0.05f)
                    .visible(fill::isValue);

    public HitBoxCustomizerModule() {
        super(ModuleTab.RENDER, "HitBox Customizer");
        addSettings(colorMode, globalColor, playerColor, friendColor, monsterColor, animalColor, entityColor,
                fill, outline, showOwn, lineWidth, outlineOpacity, fillOpacity);
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        for (Entity entity : Mc.INSTANCE.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity) || !entity.isAlive()
                    || entity == Mc.INSTANCE.getPlayer()
                    && (!showOwn.isValue()
                    || Mc.INSTANCE.getGameOptions().getPerspective() == Perspective.FIRST_PERSON)
                    || !PlayerVisualFilter.shouldRender(entity)
                    || entity.squaredDistanceTo(Mc.INSTANCE.getPlayer()) > 128.0 * 128.0) {
                continue;
            }
            Vec3d renderOffset = entity.getLerpedPos(Mc.INSTANCE.getTickDelta())
                    .subtract(entity.getPos());
            Box renderBox = entity.getBoundingBox().offset(renderOffset).expand(0.002);
            int base = resolveColor(entity);
            if (fill.isValue()) {
                ShapeRenderer.INSTANCE.addBox(event.matrixStack().peek().getPositionMatrix(),
                        renderBox,
                        ColorUtil.replAlpha(base, Math.round(fillOpacity.currentValue() * 255.0f)),
                        true, true);
            }
            if (outline.isValue()) {
                ShapeRenderer.INSTANCE.addOutline(event.matrixStack().peek().getPositionMatrix(),
                        renderBox,
                        ColorUtil.replAlpha(base, Math.round(outlineOpacity.currentValue() * 255.0f)),
                        lineWidth.currentValue(), true, true);
            }
        }
    }

    private int resolveColor(Entity entity) {
        if (colorMode.isSelected(ColorMode.CLIENT)) {
            return Spectra.INSTANCE.theme().palette().accent().argb();
        }
        if (colorMode.isSelected(ColorMode.SINGLE)) {
            return globalColor.getColor();
        }
        if (entity instanceof PlayerEntity player) {
            return FriendManager.isFriend(player.getName().getString())
                    ? friendColor.getColor() : playerColor.getColor();
        }
        if (entity instanceof HostileEntity) {
            return monsterColor.getColor();
        }
        if (entity instanceof AnimalEntity) {
            return animalColor.getColor();
        }
        return entityColor.getColor();
    }

    private static ColorSetting color(String name, int value) {
        return new ColorSetting(Translation.clearText(name)).setColor(value);
    }

    private static NumberSetting setting(String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name)).currentValue(value).range(min, max).step(step);
    }

    public enum ColorMode implements DisplayNamed {
        BY_TYPE("By type"),
        SINGLE("Single"),
        CLIENT("Client");

        private final Translation name;

        ColorMode(String name) {
            this.name = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
