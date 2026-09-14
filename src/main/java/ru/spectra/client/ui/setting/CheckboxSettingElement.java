package ru.spectra.client.ui.setting;
import ru.spectra.client.type.BindMode;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.event.EventCallback;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.HighlightAnimation;
import ru.spectra.client.ui.IconButtonWidget;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.ui.KeybindConfigWindow;
import ru.spectra.client.Lang;
import ru.spectra.client.event.LanguageChangeEvent;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.ui.ModuleFrame;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.util.RenderCommandQueue;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.util.StringUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckboxSettingElement extends ModuleFrame {
    public static final float descriptionGap = 3.0f;
    public static final int nameTextSize = 14;
    public static final int descriptionTextSize = 13;
    public final MsdfFont titleFont;
    public final MsdfFont descriptionFont;
    public final GlTexture checkmarkIcon;
    public final GlTexture keybindIcon;
    public final Translation name;
    public final Translation description;
    public final ToggleAnimator checkAnimator;
    public final ClickableBehavior clickBehavior;
    public final HighlightAnimation highlightAnimation;
    public Supplier<Boolean> toggledStateSupplier;
    public Supplier<Boolean> visibilitySupplier;
    public Runnable changeAction;

    public final IconButtonWidget keybindButton;
    public boolean showKeybindButton;
    public boolean lastToggledState;
    public String wrappedDescription;
    public boolean descriptionDirty;
    public float cachedWidth;
    public String cachedDescriptionText;
    public float descriptionHeight;
    public final WidgetBounds keybindButtonBounds;
    public final EventCallback<LanguageChangeEvent> languageChangeCallback;
    public final KeybindConfigWindow keybindWindow;
    public ScreenResolution resolution;
    public static final float windowGap = 8.0f;
    public static final float margin = 5.0f;

    public CheckboxSettingElement(Translation class254Var, Translation class254Var2, Supplier<List<Integer>> supplier) {
        this(class254Var, class254Var2, supplier, true);
    }

    public CheckboxSettingElement(Translation class254Var, Translation class254Var2, Supplier<List<Integer>> supplier, boolean z) {
        this.titleFont = Fonts.INTER_SEMIBOLD.get();
        this.descriptionFont = Fonts.INTER_MEDIUM.get();
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.keybindIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
        this.checkAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
        this.clickBehavior = new ClickableBehavior();
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.lastToggledState = false;
        this.wrappedDescription = null;
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
        this.descriptionHeight = 0.0f;
        this.keybindButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.languageChangeCallback = class226Var -> {
            this.descriptionDirty = true;
            this.cachedWidth = -1.0f;
            this.cachedDescriptionText = null;
        };
        this.name = class254Var;
        this.description = class254Var2 != null
                ? class254Var2
                : Translation.clearText("Enables or disables this option");
        this.showKeybindButton = z;
        this.keybindWindow = new KeybindConfigWindow(Lang.BINDING_MODULE, class254Var.effective(), supplier);
        Spectra.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        this.clickBehavior.clickCallback(this::invert);
        this.keybindButton = new IconButtonWidget(this.keybindIcon, this::toggleKeybindWindow, 16.0f, 16.0f);
        Spectra.INSTANCE.eventDispatcher().register(LanguageChangeEvent.class, this.languageChangeCallback);
    }

    public void setupBindType(Supplier<BindMode> supplier, Consumer<BindMode> consumer) {
        if (this.keybindWindow != null) {
            this.keybindWindow.bindType(supplier, consumer);
        }
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindWindow.onChange(consumer);
    }

    public void toggleKeybindWindow() {
        float fDpiScaleFactor = Spectra.INSTANCE.windowController().dpiScaleFactor();
        if (this.keybindWindow.isOpen() || this.resolution == null) {
            this.keybindWindow.closeWindow();
            return;
        }
        float fScreenWidth = this.resolution.screenWidth() / fDpiScaleFactor;
        float fScreenHeight = this.resolution.screenHeight() / fDpiScaleFactor;
        float fWidth = this.keybindWindow.width();
        float f = this.keybindWindow.totalHeight();
        float fX = this.keybindButtonBounds.x();
        float fY = this.keybindButtonBounds.y() - this.viewportTop;
        float fWidth2 = this.keybindButtonBounds.width();
        float fHeight = this.keybindButtonBounds.height();
        float f2 = fX + fWidth2 + windowGap;
        float f3 = (fX - windowGap) - fWidth;
        if (f2 + fWidth > fScreenWidth - margin && f3 >= margin) {
            f2 = f3;
        }
        float fMax = Math.max(margin, Math.min(f2, (fScreenWidth - fWidth) - margin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - margin) {
            f4 = (fScreenHeight - f) - margin;
        }
        if (f4 < margin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
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
        this.resolution = class699Var.resolution();
        updateWrappedDescription(textWidth(f));
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        float switchWidth = 27.0f;
        float switchHeight = 17.0f;
        float fX = (x() + f) - switchWidth - 4.0f;
        float fY = (y() + (height() / 2.0f)) - switchHeight / 2.0f;
        float fY2 = y();
        float fValue = this.highlightAnimation.value();
        class699Var.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY2, class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(200).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(50).argb()), fValue));
        if (this.wrappedDescription != null) {
            SettingTextRenderer.drawWrapped(
                    class699Var, this.descriptionFont, this.wrappedDescription,
                    descriptionTextSize, x(),
                    fY2 + this.titleFont.getHeight(14.0f) + descriptionGap,
                    class115VarColorStack.interpolate(
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(500).argb()),
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()),
                            fValue
                    )
            );
        }
        if (this.showKeybindButton) {
            float fX2 = fX - 8.0f - this.keybindButton.width();
            float fHeight = y() + (height() - this.keybindButton.height()) / 2.0f;
            this.keybindButton.setPosition(fX2, fHeight);
            this.keybindButton.setColor(class764VarPalette.text().tone(800).argb());
            this.keybindButton.render(class699Var);
            this.keybindButtonBounds.withPosition(fX2, fHeight).withSize(this.keybindButton.width(), this.keybindButton.height());
        } else {
            this.keybindButtonBounds.withSize(0.0f, 0.0f);
        }
        int switchBackground = class115VarColorStack.interpolate(
                class115VarColorStack.computeColor(0xFFFFFF, 11),
                class115VarColorStack.computeColor(class764VarPalette.accent().argb(), 0.82f),
                this.checkAnimator
        );
        class699Var.fillRoundedRect(fX, fY, switchWidth, switchHeight,
                switchHeight / 2.0f, switchBackground);
        float knobRadius = (switchHeight - 4.0f) / 2.0f;
        float knobX = fX + 2.0f + knobRadius
                + (switchWidth - 4.0f - knobRadius * 2.0f) * this.checkAnimator.smoothAnimation();
        class699Var.circle(knobX, fY + switchHeight / 2.0f, knobRadius,
                class115VarColorStack.interpolate(
                        class115VarColorStack.computeColor(0x7D7D89),
                        class115VarColorStack.white(),
                        this.checkAnimator
                ));
        this.clickBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.keybindWindow.layout(class698Var);
        if (this.showKeybindButton) {
            this.keybindButton.layout(class698Var);
        }
        this.checkmarkIcon.setDimensions(nameTextSize, nameTextSize);
    }

    public float textHeight(String str, int i, int i2) {
        float height = this.titleFont.getHeight(i);
        if (this.description != null) {
            height = height + descriptionGap + this.descriptionFont.getHeightWithLineBreaks(str, i2);
        }
        return height;
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        if (this.showKeybindButton && this.keybindButton.handleInput(class688Var, z)) {
            return true;
        }
        return this.clickBehavior.handleInput(class688Var, z);
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateWrappedDescription(textWidth(this.parent.width()));
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descriptionGap + this.descriptionHeight;
        }
        return Math.max(height, 18.0f);
    }

    private float textWidth(float totalWidth) {
        float controls = 27.0f + 12.0f;
        if (this.showKeybindButton) {
            controls += this.keybindButton.width() + 8.0f;
        }
        return Math.max(48.0f, totalWidth - controls);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        boolean zBooleanValue;
        if (this.toggledStateSupplier != null && (zBooleanValue = this.toggledStateSupplier.get().booleanValue()) != this.lastToggledState) {
            this.checkAnimator.state(zBooleanValue);
            this.lastToggledState = zBooleanValue;
        }
        if (this.showKeybindButton) {
            this.keybindButton.animation(class141Var);
        }
        this.keybindWindow.animation(class141Var);
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.highlightAnimation.animate(class141Var);
        this.checkAnimator.animate(class141Var);
        this.clickBehavior.animate(class141Var);
        super.animation(class141Var);
    }

    @Override
    public void handleClose() {
        this.keybindWindow.closeWindow();
        Spectra.INSTANCE.eventDispatcher().unregister(LanguageChangeEvent.class, this.languageChangeCallback);
        super.handleClose();
    }

    @Override
    public void collectBloomElements(RenderCommandQueue class676Var) {
        this.keybindWindow.collectBloomElements(class676Var);
        super.collectBloomElements(class676Var);
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescriptionText = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescriptionText)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descriptionTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
            this.cachedWidth = f;
            this.cachedDescriptionText = strEffective;
            this.descriptionDirty = false;
        }
    }

    public void invert() {
        if (this.changeAction != null) {
            this.changeAction.run();
        }
    }

    public Translation name() {
        return this.name;
    }

    public CheckboxSettingElement toggledSupplier(Supplier<Boolean> supplier) {
        this.toggledStateSupplier = supplier;
        return this;
    }

    public CheckboxSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public CheckboxSettingElement changeRunnable(Runnable runnable) {
        this.changeAction = runnable;
        return this;
    }
}
