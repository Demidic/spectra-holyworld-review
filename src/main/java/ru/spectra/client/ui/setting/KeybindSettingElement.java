package ru.spectra.client.ui.setting;
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
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.type.KeybindCaptureState;
import ru.spectra.client.event.LanguageChangeEvent;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.ui.ModuleFrame;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.util.StringUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KeybindSettingElement extends ModuleFrame {
    static final int nameFontSize = 14;
    static final int descriptionFontSize = 13;
    static final int bindingFontSize = 12;
    static final float l = 3.0f;
    static final float m = 10.0f;
    static final float n = 25.0f;
    static final float o = 8.0f;
    static final float controlWidth = 72.0f;
    static final float controlHeight = 24.0f;
    static final float controlGap = 14.0f;
    final Translation name;
    final Translation description;
    Supplier<Boolean> visibilitySupplier;
    String wrappedDescription;
    String cachedDescriptionSource;
    float descriptionHeight;
    public final Supplier<List<Integer>> keysSupplier;
    final MsdfFont semiBoldFont = Fonts.INTER_SEMIBOLD.get();
    final MsdfFont mediumFont = Fonts.INTER_MEDIUM.get();
    final MsdfFont menuIconFont = Fonts.MENU_ICON.get();
    final GlTexture keyboardIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
    final EventCallback<LanguageChangeEvent> languageChangeCallback = class226Var -> {
        invalidateDescriptionCache();
    };
    final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    final ClickableBehavior clearBindingButton = new ClickableBehavior();
    final WidgetBounds bindingBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final KeybindCaptureState captureState = new KeybindCaptureState();
    final ToggleAnimator activeToggleAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    final ToggleAnimator clearButtonAnimator = new ToggleAnimator(160, Easings.EASE_IN_OUT_CUBIC);
    float scaleFactor = 1.0f;
    float cachedLayoutWidth = -1.0f;
    boolean descriptionDirty = true;
    boolean clearButtonVisible = true;

    public KeybindSettingElement(Translation class254Var, Translation class254Var2, Supplier<List<Integer>> supplier) {
        this.name = class254Var;
        this.description = class254Var2 != null
                ? class254Var2
                : Translation.clearText("Sets the keyboard shortcut for this action");
        this.keysSupplier = supplier;
        ClickableBehavior class766Var = this.clickableBehavior;
        KeybindCaptureState class770Var = this.captureState;
        Objects.requireNonNull(class770Var);
        class766Var.clickCallback(class770Var::toggleCapture);
        this.clearBindingButton.clickCallback(this.captureState::clearBinding);
        Spectra.INSTANCE.eventDispatcher().register(LanguageChangeEvent.class, this.languageChangeCallback);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.captureState.onChange(consumer);
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
        if (this.captureState.captureKey()) {
            class699Var.window().interceptKeyboard(true);
        }
        updateDescriptionLayout(textWidth(f));
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        ThemePalette class764VarPalette = class699Var.theme().palette();
        float fScaleFactor = class699Var.layoutContext().scaleFactor();
        drawDescriptionText(class699Var, drawNameLabel(class699Var, y()));
        String bindingText = this.captureState.formatBindingText();
        float fMethod007 = measureTextWidth(bindingText, bindingFontSize, fScaleFactor);
        boolean zHasAnyKeys = this.captureState.hasAnyKeys();
        float bindingX = x() + f - controlWidth;
        float bindingY = y() + (height() - controlHeight) / 2.0f;
        this.bindingBounds.withPosition(bindingX, bindingY).withSize(controlWidth, controlHeight);
        int iInterpolate = class115VarColorStack.interpolate(
                class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(400).argb()),
                class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(300).argb()),
                this.clickableBehavior.hoverAnimation()
        );
        int iInterpolate2 = class115VarColorStack.interpolate(
                class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(600).argb()),
                class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(500).argb()),
                this.clickableBehavior.hoverAnimation()
        );
        float iconGap = !zHasAnyKeys ? 5.0f : 0.0f;
        float iconWidth = !zHasAnyKeys ? this.keyboardIcon.width() : 0.0f;
        float availableTextWidth = controlWidth - 16.0f - iconWidth - iconGap;
        String strMethod005 = bindingText;
        if (fMethod007 > availableTextWidth) {
            strMethod005 = truncateWithEllipsis(bindingText, availableTextWidth, fScaleFactor);
        }
        float fMethod008 = measureTextWidth(strMethod005, bindingFontSize, fScaleFactor);
        float contentWidth = iconWidth + iconGap + fMethod008;
        float fY2 = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
        int iInterpolate3 = class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(600).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()), this.activeToggleAnimator.smoothAnimation());
        int iInterpolate4 = class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(600).argb()), class115VarColorStack.white(), this.activeToggleAnimator.smoothAnimation());
        class699Var.fillOutlinedRoundedRect(
                this.bindingBounds.x(), this.bindingBounds.y(),
                this.bindingBounds.width(), this.bindingBounds.height(),
                6.0f, 2.5f, iInterpolate, iInterpolate2
        );
        float contentX = this.bindingBounds.x() + (this.bindingBounds.width() - contentWidth) / 2.0f;
        if (!zHasAnyKeys) {
            class699Var.textureVerticalC(
                    this.keyboardIcon, contentX, fY2,
                    this.keyboardIcon.width(), this.keyboardIcon.height(), iInterpolate4
            );
        }
        class699Var.text(
                this.semiBoldFont, strMethod005, bindingFontSize,
                contentX + iconWidth + iconGap,
                fY2 - this.semiBoldFont.getHeight(bindingFontSize) / 2.0f,
                iInterpolate3
        );
        renderClearBindingButton(class699Var, class115VarColorStack, zHasAnyKeys);
        this.clickableBehavior.setDimensions(
                this.bindingBounds.x(), this.bindingBounds.y(),
                this.bindingBounds.width(), this.bindingBounds.height());
    }

    private void renderClearBindingButton(DrawCtx ctx, ColorStack colors, boolean visible) {
        if (!visible || !this.clearButtonVisible) {
            this.clearBindingButton.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
            return;
        }
        float buttonX = this.bindingBounds.x() - 23.0f;
        float buttonY = this.bindingBounds.y() + (this.bindingBounds.height() - 18.0f) / 2.0f;
        float hover = this.clearBindingButton.hoverAnimation().smoothAnimation();
        String glyph = "\u044C";
        ctx.text(this.menuIconFont, glyph, 12,
                buttonX + (18.0f - this.menuIconFont.getWidth(glyph, 12.0f)) / 2.0f,
                buttonY + 4.0f,
                colors.computeColor(0xD6D6DE, Math.round(180.0f + 75.0f * hover)));
        this.clearBindingButton.setDimensions(buttonX, buttonY, 18.0f, 18.0f);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.scaleFactor = class698Var.scaleFactor();
        this.keyboardIcon.setDimensions(bindingFontSize, bindingFontSize);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = super.handleInput(class688Var, z);
        if (this.clearButtonVisible && !zHandleInput && this.captureState.hasAnyKeys()
                && this.clearBindingButton.handleInput(class688Var, z)) {
            return true;
        }
        boolean zHandleInput2 = zHandleInput | this.clickableBehavior.handleInput(class688Var, zHandleInput);
        if (!z) {
            if (this.captureState.captureKey()) {
                InputEvent class691VarInputEvent = class688Var.inputEvent();
                if (class691VarInputEvent instanceof MouseButtonInput) {
                    MouseButtonInput class693Var = (MouseButtonInput) class691VarInputEvent;
                    if (class693Var.action().press() && class693Var.button() == 0 && !class688Var.inArea(this.clickableBehavior.ownerX(), this.clickableBehavior.ownerY(), this.clickableBehavior.ownerW(), this.clickableBehavior.ownerH())) {
                        this.captureState.toggleCapture();
                        zHandleInput2 = true;
                    }
                }
            }
            if (!zHandleInput2) {
                InputEvent class691VarInputEvent2 = class688Var.inputEvent();
                if (class691VarInputEvent2 instanceof KeyInput) {
                    zHandleInput2 = this.captureState.handleKeyInput((KeyInput) class691VarInputEvent2);
                }
                if (class691VarInputEvent2 instanceof MouseButtonInput) {
                    zHandleInput2 = this.captureState.handleMouseInput((MouseButtonInput) class691VarInputEvent2);
                }
            }
            zHandleInput2 |= this.captureState.captureKey() && !zHandleInput2;
        }
        return zHandleInput2;
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.captureState.syncKeys(this.keysSupplier.get());
        this.activeToggleAnimator.state(this.captureState.hasAnyKeys());
        this.activeToggleAnimator.animate(class141Var);
        this.clearButtonAnimator.state(this.captureState.hasAnyKeys());
        this.clearButtonAnimator.animate(class141Var);
        this.clearBindingButton.animate(class141Var);
        this.clickableBehavior.animate(class141Var);
        this.highlightAnimation.animate(class141Var);
        super.animation(class141Var);
    }

    @Override
    public void handleClose() {
        Spectra.INSTANCE.eventDispatcher().unregister(LanguageChangeEvent.class, this.languageChangeCallback);
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescriptionLayout(textWidth(this.parent.width()));
        }
        float textHeight = this.semiBoldFont.getHeight(nameFontSize);
        if (this.wrappedDescription != null) {
            textHeight += l + this.descriptionHeight;
        }
        return Math.max(controlHeight, textHeight);
    }

    public String truncateWithEllipsis(String str, float f, float f2) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        float fMethod006 = f - measureTextWidth("...", bindingFontSize, f2);
        if (fMethod006 <= 0.0f) {
            return "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (measureTextWidth(sb.toString() + str.charAt(i), bindingFontSize, f2) > fMethod006) {
                return String.valueOf(sb) + "...";
            }
            sb.append(str.charAt(i));
        }
        return str;
    }

    public float measureTextWidth(String str, int i, float f) {
        return this.semiBoldFont.getWidth(str, Math.max(1, Math.round(i * f))) / f;
    }

    public float drawNameLabel(DrawCtx class699Var, float f) {
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        class699Var.text(this.semiBoldFont, this.name.effective(), nameFontSize, x(), f, class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(200).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(50).argb()), this.highlightAnimation.value()));
        return f + this.semiBoldFont.getHeight(14.0f);
    }

    public float drawDescriptionText(DrawCtx class699Var, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        int iInterpolate = class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(500).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()), this.highlightAnimation.value());
        float f2 = f + l;
        SettingTextRenderer.drawWrapped(
                class699Var, this.mediumFont, this.wrappedDescription,
                descriptionFontSize, x(), f2, iInterpolate
        );
        return f2 + this.descriptionHeight;
    }

    public void invalidateDescriptionCache() {
        this.descriptionDirty = true;
        this.cachedLayoutWidth = -1.0f;
        this.cachedDescriptionSource = null;
    }

    public void updateDescriptionLayout(float f) {
        if (this.description == null) {
            clearDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCacheValid(f, strEffective)) {
            return;
        }
        formatDescription(f, strEffective == null ? "" : strEffective);
    }

    public void clearDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = null;
    }

    public boolean isDescriptionCacheValid(float f, String str) {
        return !this.descriptionDirty && f == this.cachedLayoutWidth && str != null && str.equals(this.cachedDescriptionSource);
    }

    public void formatDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.mediumFont, descriptionFontSize);
        this.descriptionHeight = this.mediumFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionFontSize);
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = str;
        this.descriptionDirty = false;
    }

    private float textWidth(float totalWidth) {
        return Math.max(64.0f, totalWidth - controlWidth - controlGap - 23.0f);
    }

    public Translation name() {
        return this.name;
    }

    public KeybindSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public KeybindSettingElement showClearButton(boolean visible) {
        this.clearButtonVisible = visible;
        if (!visible) {
            this.clearBindingButton.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
        }
        return this;
    }
}

