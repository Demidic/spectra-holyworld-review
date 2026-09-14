package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.type.KeybindCaptureState;
import ru.spectra.client.render.KeybindColors;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindField extends AbstractWidget {
    public final MsdfFont font;
    public final float paddingX;
    public final float paddingY;
    public final float cornerRadius;
    public final int fontSize;
    public boolean initialized;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final AnimatedFloat widthAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    public final MsdfFont menuIconFont = ru.spectra.client.render.Fonts.MENU_ICON.get();
    public static final String KEYBOARD_GLYPH = "Л";
    public static final float KEYBOARD_ICON_SIZE = 12.0f;
    public final KeybindCaptureState captureState = new KeybindCaptureState();
    public Supplier<Float> rawXSupplier;

    public KeybindColors colorsOverride;

    public KeybindField(MsdfFont class161Var, Runnable runnable, float f, float f2, float f3, int i) {
        this.font = class161Var;
        this.paddingX = f;
        this.paddingY = f2;
        this.cornerRadius = f3;
        this.fontSize = i;
        if (runnable != null) {
            this.clickBehavior.rightClickCallback(runnable);
        }
        ClickableBehavior class766Var = this.clickBehavior;
        KeybindCaptureState class770Var = this.captureState;
        Objects.requireNonNull(class770Var);
        class766Var.clickCallback(class770Var::toggleCapture);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.captureState.onChange(consumer);
    }

    public KeybindField addMutableKeys(List<Integer> list) {
        this.captureState.addMutableKeys(list);
        return this;
    }

    public void syncKeys(List<Integer> list) {
        this.captureState.syncKeys(list);
    }

    public void addImmutableKeys(List<Integer> list) {
        this.captureState.addImmutableKeys(list);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        float fComputeDesiredHeight = computeDesiredHeight();
        float fComputeDesiredWidth = computeDesiredWidth(class698Var);
        if (!this.initialized) {
            this.widthAnimation.set(fComputeDesiredWidth);
            this.initialized = true;
        }
        this.widthAnimation.destination(fComputeDesiredWidth);
        setSize(this.widthAnimation.animatedValue(), fComputeDesiredHeight);
        this.clickBehavior.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void render(DrawCtx class699Var) {
        int iTextEmpty;
        float fWidth;
        float fX;
        if (this.captureState.captureKey()) {
            class699Var.window().interceptKeyboard(true);
        }
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        String bindingText = this.captureState.formatBindingText();
        float fTextWidthPhysical = class699Var.textWidthPhysical(this.font, bindingText, this.fontSize);
        float height = this.font.getHeight(this.fontSize);
        KeybindColors class744VarFromPalette = this.colorsOverride != null ? this.colorsOverride : KeybindColors.fromPalette(class764VarPalette, class115VarColorStack);
        ToggleAnimator class323VarHoverAnimation = this.clickBehavior.hoverAnimation();
        int iBrightenedInterpolatedColor = class115VarColorStack.brightenedInterpolatedColor(class744VarFromPalette.outline(), 0.05f, class323VarHoverAnimation);
        int iBrightenedInterpolatedColor2 = class115VarColorStack.brightenedInterpolatedColor(class744VarFromPalette.background(), 0.05f, class323VarHoverAnimation);
        class699Var.fillOutlinedRoundedRect(x(), y(), width(), height(), this.cornerRadius, 2.5f, iBrightenedInterpolatedColor, iBrightenedInterpolatedColor2);
        float fY = (y() + (height() / 2.0f)) - (height / 2.0f);
        float fX2 = x() + this.widthAnimation.animatedValue();
        if (this.captureState.hasAnyKeys()) {
            float fComputeDesiredWidth = computeDesiredWidth(class699Var.layoutContext());
            iTextEmpty = class744VarFromPalette.text();
            fWidth = ((this.rawXSupplier != null ? this.rawXSupplier.get().floatValue() : x()) + (fComputeDesiredWidth / 2.0f)) - (fTextWidthPhysical / 2.0f);
            fX = (x() + (width() / 2.0f)) - (fTextWidthPhysical / 2.0f);
        } else {
            iTextEmpty = class744VarFromPalette.textEmpty();
            float fX3 = x() + this.paddingX;
            class699Var.text(
                    this.menuIconFont, KEYBOARD_GLYPH, Math.round(KEYBOARD_ICON_SIZE),
                    fX3,
                    (y() + height() / 2.0f)
                            - this.menuIconFont.getHeight(KEYBOARD_ICON_SIZE) / 2.0f,
                    iTextEmpty
            );
            fWidth = fX3 + this.menuIconFont.getWidth(KEYBOARD_GLYPH, KEYBOARD_ICON_SIZE) + 6.0f;
            fX = fWidth;
        }
        if (fX + fTextWidthPhysical <= fX2 - 5.0f) {
            class699Var.text(this.font, bindingText, this.fontSize, fWidth, fY, iTextEmpty);
            return;
        }
        class699Var.drawEngine().beginStencil();
        class699Var.fillOutlinedRoundedRect(x(), y(), width(), height(), this.cornerRadius, 2.5f, iBrightenedInterpolatedColor, iBrightenedInterpolatedColor2);
        class699Var.drawEngine().prepareStencil(1);
        class699Var.text(this.font, bindingText, this.fontSize, fWidth, fY, iTextEmpty);
        class699Var.drawEngine().endStencil();
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.widthAnimation.animate(class141Var);
        this.clickBehavior.animate(class141Var);
        super.animation(class141Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = super.handleInput(class688Var, z);
        if (!z) {
            if (this.captureState.captureKey()) {
                InputEvent class691VarInputEvent = class688Var.inputEvent();
                if (class691VarInputEvent instanceof MouseButtonInput) {
                    MouseButtonInput class693Var = (MouseButtonInput) class691VarInputEvent;
                    if (class693Var.action().press() && class693Var.button() == 0 && !class688Var.inArea(x(), y(), width(), height())) {
                        this.captureState.toggleCapture();
                        zHandleInput = true;
                    }
                }
            }
            boolean zHandleInput2 = zHandleInput | this.clickBehavior.handleInput(class688Var, zHandleInput);
            if (!zHandleInput2) {
                InputEvent class691VarInputEvent2 = class688Var.inputEvent();
                if (class691VarInputEvent2 instanceof KeyInput) {
                    zHandleInput2 = this.captureState.handleKeyInput((KeyInput) class691VarInputEvent2);
                }
                if (class691VarInputEvent2 instanceof MouseButtonInput) {
                    zHandleInput2 = this.captureState.handleMouseInput((MouseButtonInput) class691VarInputEvent2);
                }
            }
            zHandleInput = zHandleInput2 | (this.captureState.captureKey() && !zHandleInput2);
        }
        return zHandleInput;
    }

    public float computeDesiredWidth(LayoutScaleContext class698Var) {
        float fTextWidthPhysical = class698Var.textWidthPhysical(this.font, this.captureState.formatBindingText(), Math.max(1, Math.round(this.fontSize * class698Var.scaleFactor()))) / class698Var.scaleFactor();
        if (this.captureState.hasAnyKeys()) {
            return fTextWidthPhysical + (this.paddingX * 2.0f);
        }
        return (this.paddingX * 2.0f)
                + this.menuIconFont.getWidth(KEYBOARD_GLYPH, KEYBOARD_ICON_SIZE)
                + 6.0f + fTextWidthPhysical;
    }

    public float computeDesiredHeight() {
        float height = this.font.getHeight(this.fontSize);
        return (this.captureState.hasAnyKeys()
                ? height
                : Math.max(this.menuIconFont.getHeight(KEYBOARD_ICON_SIZE), height))
                + (this.paddingY * 2.0f);
    }

    public KeybindField rawX(Supplier<Float> supplier) {
        this.rawXSupplier = supplier;
        return this;
    }

    public KeybindField colors(KeybindColors class744Var) {
        this.colorsOverride = class744Var;
        return this;
    }
}
