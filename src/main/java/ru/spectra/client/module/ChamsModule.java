package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.type.ChamsTargetType;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.type.EntityCategory;
import ru.spectra.client.model.EntityFilter;
import ru.spectra.client.render.DeferredChamsRenderer;
import ru.spectra.client.Lang;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.MultiSelectSetting;

import net.minecraft.entity.Entity;
import net.minecraft.client.render.BuiltBuffer;

@Aliases(aliases = {"Chams", "Model", "Entity ESP", "ESP"})
public class ChamsModule extends Module {
    public final MultiSelectSetting<ChamsTargetType> selectTargets;
    public final ColorSetting colorSetting;
    public final BooleanSetting blendingSetting;
    public Entity currentEntity;
    private final DeferredChamsRenderer deferredRenderer = new DeferredChamsRenderer();

    public ChamsModule() {
        super(ModuleTab.RENDER, "Chams");
        this.selectTargets = new MultiSelectSetting(Lang.SELECTTARGETS, Lang.SELECTTARGETS_DESC).values(ChamsTargetType.class);
        this.colorSetting = new ColorSetting(Lang.CHAMS_COLOR);
        this.blendingSetting = new BooleanSetting(Lang.CHAMS_BLENDING).setValue(true);
        this.currentEntity = null;
        addSettings(this.selectTargets, this.colorSetting);
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null) {
            return false;
        }
        EntityFilter class095VarMethod001 = buildEntityFilter();
        return !class095VarMethod001.isEmpty() && class095VarMethod001.matches(entity);
    }

    public EntityFilter buildEntityFilter() {
        EntityFilter class095Var = new EntityFilter();
        if (this.selectTargets.isSelected(ChamsTargetType.SELF)) {
            class095Var.add(EntityCategory.SELF);
        }
        if (this.selectTargets.isSelected(ChamsTargetType.PLAYERS)) {
            class095Var.add(EntityCategory.PLAYER);
        }
        if (this.selectTargets.isSelected(ChamsTargetType.FRIENDS)) {
            class095Var.add(EntityCategory.FRIEND);
        }
        if (this.selectTargets.isSelected(ChamsTargetType.MOBS)) {
            class095Var.add(EntityCategory.MOB);
        }
        if (this.selectTargets.isSelected(ChamsTargetType.ANIMALS)) {
            class095Var.add(EntityCategory.ANIMAL);
        }
        return class095Var;
    }

    public MultiSelectSetting<ChamsTargetType> selectTargets() {
        return this.selectTargets;
    }

    public ColorSetting colorSetting() {
        return this.colorSetting;
    }

    public BooleanSetting blendingSetting() {
        return this.blendingSetting;
    }

    public Entity currentEntity() {
        return this.currentEntity;
    }

    public ChamsModule currentEntity(Entity entity) {
        this.currentEntity = entity;
        return this;
    }

    public void beginWorldFrame(boolean blurredFogEnabled) {
        this.deferredRenderer.beginFrame(blurredFogEnabled);
    }

    public boolean shouldDeferColorForBlurredFog() {
        return this.deferredRenderer.shouldDeferColor();
    }

    public void drawDepthAndDeferColor(BuiltBuffer buffer, int color, boolean additiveBlend) {
        this.deferredRenderer.drawDepthAndDefer(buffer, color, additiveBlend);
    }

    public void drawDeferredColor() {
        this.deferredRenderer.drawDeferredColor();
    }
}
