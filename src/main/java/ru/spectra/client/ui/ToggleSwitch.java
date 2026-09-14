package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;

import java.util.function.Supplier;

public class ToggleSwitch extends AbstractWidget {
    public final float switchWidth;
    public final float switchHeight;
    public final Supplier<Boolean> stateSupplier;
    public Runnable clickRunnable;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator toggleSwitchAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public boolean lastState = false;

    public ToggleSwitch(float f, float f2, Supplier<Boolean> supplier) {
        this.switchWidth = f;
        this.switchHeight = f2;
        this.stateSupplier = supplier;
        this.clickable.clickCallback(this::invert);
    }

    @Override
    public void render(DrawCtx class699Var) {
        boolean zBooleanValue = this.stateSupplier.get().booleanValue();
        if (zBooleanValue != this.lastState) {
            this.toggleSwitchAnimation.state(zBooleanValue);
            this.lastState = zBooleanValue;
        }
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        int background = class115VarColorStack.interpolate(
                class115VarColorStack.computeColor(0xFFFFFF, 11),
                class115VarColorStack.computeColor(0x8B87FF, 194),
                this.toggleSwitchAnimation
        );
        class699Var.fillRoundedRect(x(), y(), this.switchWidth, this.switchHeight,
                this.switchHeight / 2.0f, background);
        float knobRadius = Math.max(2.0f, (this.switchHeight - 4.0f) / 2.0f);
        float knobX = x() + 2.0f + knobRadius
                + ((this.switchWidth - 4.0f - knobRadius * 2.0f)
                * this.toggleSwitchAnimation.smoothAnimation());
        int knobColor = class115VarColorStack.interpolate(
                class115VarColorStack.computeColor(0x7D7D89),
                class115VarColorStack.white(),
                this.toggleSwitchAnimation
        );
        class699Var.circle(knobX, y() + this.switchHeight / 2.0f, knobRadius, knobColor);
    }

    public void invert() {
        if (this.clickRunnable != null) {
            this.clickRunnable.run();
        }
    }

    public void setClickableArea(float f, float f2, float f3, float f4) {
        this.clickable.setDimensions(f, f2, f3, f4);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        setSize(this.switchWidth, this.switchHeight);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.toggleSwitchAnimation.animate(class141Var);
        this.clickable.animate(class141Var);
        super.animation(class141Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = super.handleInput(class688Var, z);
        if (!zHandleInput) {
            zHandleInput = this.clickable.handleInput(class688Var, z);
        }
        return zHandleInput;
    }

    public ToggleAnimator toggleSwitchAnimation() {
        return this.toggleSwitchAnimation;
    }

    public ToggleSwitch runnable(Runnable runnable) {
        this.clickRunnable = runnable;
        return this;
    }
}
