package ru.spectra.client.ui.setting;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.event.EventCallback;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.HighlightAnimation;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.event.LanguageChangeEvent;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.ui.ModuleFrame;
import ru.spectra.client.type.MouseButtonAction;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.util.StringUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joml.Vector4f;

public class SliderSettingElement extends ModuleFrame {
    public static final float p = 3.0f;
    public static final float q = 12.0f;
    public static final int titleFontSize = 14;
    public static final int descFontSize = 13;
    public static final float r = 3.0f;
    public static final float s = 7.0f;
    public static final float t = 1.5f;
    public static final float u = 2.0f;
    public final Translation name;
    public final Translation description;
    public final SettingUnit unit;
    public final float v;
    public final float z;
    public final float A;
    public float currentValue;
    public Supplier<Float> valueSupplier;

    public Supplier<Boolean> visibleCondition;
    public Consumer<Float> changeConsumer;
    public float B;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descFont = Fonts.INTER_MEDIUM.get();
    public final AnimatedFloat valueAnimation = new AnimatedFloat(140, Easings.LINEAR);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public String wrappedDescription = null;
    public boolean dragging = false;
    public final WidgetBounds trackBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds interactionBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean descriptionDirty = true;
    public float lastLayoutWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = class226Var -> {
        this.descriptionDirty = true;
        this.lastLayoutWidth = -1.0f;
        this.lastDescription = null;
    };

    public SliderSettingElement(Translation class254Var, Translation class254Var2, SettingUnit class614Var, float f, float f2, float f3) {
        this.name = class254Var;
        this.description = class254Var2;
        this.unit = class614Var;
        this.v = f;
        this.z = f2;
        this.A = f3;
        Spectra.INSTANCE.eventDispatcher().register(LanguageChangeEvent.class, this.languageChangeCallback);
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx class699Var, float f, float f2) {
        updateDescription(f);
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        float fY = y();
        String str = formatValue(this.currentValue);
        String strEffective = this.name.effective();
        float fTextWidthPhysical = class699Var.textWidthPhysical(this.titleFont, str, 12);
        float fTextWidthPhysical2 = class699Var.textWidthPhysical(this.titleFont, strEffective, titleFontSize);
        float fX = (x() + f) - fTextWidthPhysical;
        float f3 = fX - 10.0f;
        boolean z = x() + fTextWidthPhysical2 > f3;
        int iComputeColor = class115VarColorStack.computeColor(class764VarPalette.accent().argb());
        String strCutoff = StringUtil.cutoff(strEffective, x() + fTextWidthPhysical2, f3, fTextWidthPhysical2, z);
        float fValue = this.highlightAnimation.value();
        class699Var.text(this.titleFont, strCutoff, titleFontSize, x(), fY, class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(200).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(50).argb()), fValue));
        class699Var.text(this.titleFont, str, 12, fX, fY + 1.0f, iComputeColor);
        float f4 = 0.0f;
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + 3.0f;
            SettingTextRenderer.drawWrapped(
                    class699Var, this.descFont, this.wrappedDescription,
                    descFontSize, x(), fY,
                    class115VarColorStack.interpolate(
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(500).argb()),
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()),
                            fValue
                    )
            );
            f4 = this.descriptionHeight;
        }
        this.trackBounds.withPosition(x(), fY + (this.description != null ? f4 : this.titleFont.getHeight(14.0f)) + q).withSize(f, 3.0f);
        float f5 = 5.0f + u;
        float fX2 = this.trackBounds.x() - f5;
        float fWidth = this.trackBounds.width() + (f5 * u);
        float fY2 = this.trackBounds.y() - f5;
        float fHeight = this.trackBounds.height() + (f5 * u);
        this.interactionBounds.withPosition(x(), y()).withSize(f, height());
        int iComputeColor2 = class115VarColorStack.computeColor(0xFFFFFF, 18);
        int iInterpolate = class115VarColorStack.interpolate(iComputeColor2, class115VarColorStack.autoBrightenDarken(iComputeColor2, 0.01f), this.hoverAnimator);
        int iDarkenedInterpolatedColor = class115VarColorStack.darkenedInterpolatedColor(iComputeColor, 0.1f, this.hoverAnimator);
        float fClamp = MathUtil.clamp((this.valueAnimation.animatedValue() - this.z) / (this.v - this.z), 0.0f, 1.0f);
        float f6 = 3.5f + t;
        float fX3 = this.trackBounds.x() + f6;
        float fMax = Math.max(0.0f, this.trackBounds.width() - (f6 * u));
        float f7 = (fX3 - 1.0f) + (fClamp * fMax);
        float fY3 = this.trackBounds.y() + (this.trackBounds.height() / u);
        class699Var.fillRoundedRect(this.trackBounds.x(), this.trackBounds.y(), this.trackBounds.width(), this.trackBounds.height(), new Vector4f(u, u, u, u), iInterpolate);
        class699Var.fillRoundedRect(this.trackBounds.x(), this.trackBounds.y(), fClamp * fMax, this.trackBounds.height(), new Vector4f(u, u, u, u), iComputeColor);
        class699Var.circle(f7, fY3, 3.5f + t, iDarkenedInterpolatedColor);
        class699Var.circle(f7, fY3, 3.5f, class115VarColorStack.white());
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        if (super.handleInput(class688Var, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        InputEvent class691VarInputEvent = class688Var.inputEvent();
        if (!(class691VarInputEvent instanceof MouseButtonInput)) {
            if (!(class688Var.inputEvent() instanceof CursorMoveInput)) {
                return false;
            }
            if (this.dragging) {
                float fMethod001 = roundToStep(positionToValue(this.z, this.v, this.trackBounds.x(), this.trackBounds.width(), class688Var.logicalMousePosition().x()), this.A);
                if (fMethod001 != this.currentValue) {
                    this.currentValue = fMethod001;
                    if (this.changeConsumer != null) {
                        this.changeConsumer.accept(Float.valueOf(this.currentValue));
                    }
                }
            }
            boolean z2 = class688Var.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height()) || this.dragging;
            this.hoverAnimator.state(z2);
            return z2;
        }
        MouseButtonInput class693Var = (MouseButtonInput) class691VarInputEvent;
        if (class693Var.button() != 0) {
            return false;
        }
        MouseButtonAction class706VarAction = class693Var.action();
        if (!class706VarAction.press() || !class688Var.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height())) {
            if (!class706VarAction.release() || !this.dragging) {
                return false;
            }
            this.dragging = false;
            return true;
        }
        this.dragging = true;
        float fMethod002 = roundToStep(positionToValue(this.z, this.v, this.trackBounds.x(), this.trackBounds.width(), class688Var.logicalMousePosition().x()), this.A);
        if (fMethod002 == this.currentValue) {
            return true;
        }
        this.currentValue = fMethod002;
        if (this.changeConsumer == null) {
            return true;
        }
        this.changeConsumer.accept(Float.valueOf(this.currentValue));
        return true;
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        if (this.valueSupplier != null) {
            Float f = this.valueSupplier.get();
            if (Math.abs(f.floatValue() - this.B) > 1.0E-6f) {
                this.currentValue = roundToStep(f.floatValue(), this.A);
                this.B = f.floatValue();
            }
        }
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.highlightAnimation.animate(class141Var);
        this.valueAnimation.destination(this.currentValue);
        this.valueAnimation.animate(class141Var);
        this.hoverAnimator.animate(class141Var);
        super.animation(class141Var);
    }

    @Override
    public void handleClose() {
        this.dragging = false;
        Spectra.INSTANCE.eventDispatcher().unregister(LanguageChangeEvent.class, this.languageChangeCallback);
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(this.parent.width());
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += 3.0f + this.descriptionHeight;
        }
        return height + 18.0f;
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastLayoutWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastLayoutWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descFont, descFontSize);
            this.descriptionHeight = this.descFont.getHeightWithLineBreaks(this.wrappedDescription, descFontSize);
            this.lastLayoutWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public float positionToValue(float f, float f2, float f3, float f4, double d) {
        return (Math.clamp((float) ((d - ((double) f3)) / ((double) f4)), 0.0f, 1.0f) * (f2 - f)) + f;
    }

    public float roundToStep(float f, float f2) {
        if (!Float.isFinite(f) || !Float.isFinite(f2) || f2 <= 0.0f) {
            return MathUtil.clamp(f, this.z, this.v);
        }
        BigDecimal value = new BigDecimal(Float.toString(f));
        BigDecimal minimum = new BigDecimal(Float.toString(this.z));
        BigDecimal step = new BigDecimal(Float.toString(f2));
        BigDecimal stepCount = value.subtract(minimum)
                .divide(step, 0, RoundingMode.HALF_UP);
        float rounded = minimum.add(step.multiply(stepCount)).floatValue();
        return MathUtil.clamp(rounded, this.z, this.v);
    }

    private String formatValue(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        float normalized = roundToStep(value, this.A);
        return new BigDecimal(Float.toString(normalized)).stripTrailingZeros().toPlainString();
    }

    public Translation name() {
        return this.name;
    }

    public SliderSettingElement currentValueSupplier(Supplier<Float> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public SliderSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }

    public SliderSettingElement onValueChanged(Consumer<Float> consumer) {
        this.changeConsumer = consumer;
        return this;
    }
}

