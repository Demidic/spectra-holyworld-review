package ru.spectra.client.ui.setting;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.event.EventCallback;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.HighlightAnimation;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.Lang;
import ru.spectra.client.event.LanguageChangeEvent;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.ui.ModuleFrame;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.model.MultiSelectOption2;
import ru.spectra.client.ui.ScrollArea;
import ru.spectra.client.model.ScrollInput;
import ru.spectra.client.ui.ScrollbarWidget;
import ru.spectra.client.util.StringUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.lang.Enum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.joml.Quaternionf;

public class MultiSelectSettingElement<T extends Enum<T>> extends ModuleFrame {
    public static final float X = 140.0f;
    public static final int titleTextSize = 14;
    public static final int descTextSize = 13;
    public static final float Y = 3.0f;
    public static final float Z = 12.0f;
    public static final float aa = 25.0f;
    public static final float controlWidth = 132.0f;
    public static final float controlGap = 14.0f;
    public static final float ab = 10.0f;
    public static final float ac = 15.0f;
    public static final float ad = 10.0f;
    public static final float ae = 22.0f;
    public static final float af = 15.0f;
    public final Translation name;
    public final Translation description;
    public final Supplier<Set<T>> selectedSupplier;
    public Supplier<Boolean> visibleCondition;
    public boolean expanded;
    public final ScrollbarWidget scrollbar;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator expandAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture multiEnumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/multienum.png"));
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow.png"));
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<MultiSelectOption2<T>> options = new ArrayList();
    public final WidgetBounds boxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float cachedWidth = -1.0f;

    public String cachedDescription = null;
    public float descriptionHeight = 0.0f;
    public final ScrollArea scrollArea = new ScrollArea();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final EventCallback<LanguageChangeEvent> languageChangeCallback = class226Var -> {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescription = null;
    };

    public MultiSelectSettingElement(Translation class254Var, Translation class254Var2, T[] tArr, Supplier<Set<T>> supplier, Consumer<T> consumer) {
        ScrollArea class789Var = this.scrollArea;
        Objects.requireNonNull(class789Var);
        Supplier supplier2 = class789Var::scrollY;
        Supplier supplier3 = this::computeContentHeight;
        ScrollArea class789Var2 = this.scrollArea;
        Objects.requireNonNull(class789Var2);
        Supplier supplier4 = class789Var2::height;
        ScrollArea class789Var3 = this.scrollArea;
        Objects.requireNonNull(class789Var3);
        this.scrollbar = new ScrollbarWidget(supplier2, supplier3, supplier4, (v1) -> {
            class789Var.scrollTo(v1);
        }, 4.0f, 4.0f, 30.0f);
        this.name = class254Var;
        this.description = class254Var2 != null
                ? class254Var2
                : Translation.clearText("Selects which options are active");
        this.selectedSupplier = supplier;
        for (T t : tArr) {
            this.options.add(new MultiSelectOption2<>(t));
        }
        Set<T> set = supplier.get();
        for (MultiSelectOption2<T> class837Var : this.options) {
            class837Var.toggleAnimation().force(set.contains(class837Var.option()));
            class837Var.clickableBehavior().clickCallback(() -> {
                boolean z = !((Set) supplier.get()).contains(class837Var.option());
                consumer.accept(class837Var.option());
                class837Var.toggleAnimation().state(z);
            });
        }
        this.clickBehavior.clickCallback(this::invert);
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
        updateDescription(textWidth(f));
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        class699Var.text(this.titleFont, this.name.effective(), titleTextSize, x(), fY, class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(200).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(50).argb()), fValue));
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + Y;
            SettingTextRenderer.drawWrapped(
                    class699Var, this.descriptionFont, this.wrappedDescription,
                    descTextSize, x(), fY,
                    class115VarColorStack.interpolate(
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(500).argb()),
                            class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()),
                            fValue
                    )
            );
        }
        float boxX = x() + f - controlWidth;
        float boxY = y() + (height() - aa) / 2.0f;
        float fMethod007 = computeContentHeight();
        this.boxBounds.withPosition(boxX, boxY).withSize(controlWidth, aa);
        class699Var.fillOutlinedRoundedRect(
                boxX, boxY, controlWidth, aa, 6.0f, 2.5f,
                class115VarColorStack.interpolate(
                        class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(400).argb()),
                        class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(300).argb()),
                        this.clickBehavior.hoverAnimation()
                ),
                class115VarColorStack.interpolate(
                        class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(600).argb()),
                        class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(500).argb()),
                        this.clickBehavior.hoverAnimation()
                )
        );
        float fX = boxX + 10.0f;
        float fX2 = ((boxX + controlWidth) - 9.0f) - this.arrowIcon.width();
        float fRound = (boxY + 12.5f) - Math.round(this.arrowIcon.height() / 2.0f);
        float fWidth = fX2 + (this.arrowIcon.width() / 2.0f);
        float fHeight = fRound + (this.arrowIcon.height() / 2.0f);
        float fSmoothAnimation = (float) (3.141592653589793d * ((double) this.expandAnimation.smoothAnimation()));
        float physical = class699Var.layoutContext().toPhysical(fWidth);
        float physical2 = class699Var.layoutContext().toPhysical(fHeight);
        class699Var.matrixStack().push();
        class699Var.matrixStack().translate(physical, physical2, 0.0f);
        class699Var.matrixStack().multiply(new Quaternionf().rotateZ(fSmoothAnimation));
        class699Var.matrixStack().translate(-physical, -physical2, 0.0f);
        class699Var.texture(this.arrowIcon, fX2, fRound, this.arrowIcon.width(), this.arrowIcon.height(), class115VarColorStack.computeColor(class764VarPalette.text().tone(600).argb()));
        class699Var.matrixStack().pop();
        if (this.selectedSupplier == null) {
            return;
        }
        Set<T> set = this.selectedSupplier.get();
        String strEffective = set.isEmpty() ? Lang.NOTHING_SELECTED.effective() : (String) set.stream().map(r2 -> {
            return r2 instanceof DisplayNamed ? ((DisplayNamed) r2).getDisplayName().effective() : r2.name();
        }).collect(Collectors.joining(", "));
        float fWidth2 = fX;
        int iComputeColor = class115VarColorStack.computeColor(set.isEmpty() ? class764VarPalette.text().tone(500).argb() : class764VarPalette.text().tone(300).argb());
        float f4 = (fX2 - 8.0f) - fWidth2;
        if (class699Var.textWidthPhysical(this.valueFont, strEffective, 12) > f4) {
            int i = iComputeColor & 16777215;
            String str = "";
            for (int length = strEffective.length(); length > 0; length--) {
                String strSubstring = strEffective.substring(0, length);
                if (class699Var.textWidthPhysical(this.valueFont, strSubstring, 12) <= f4) {
                    str = strSubstring;
                    break;
                }
            }
            class699Var.textWithHorizontalGradient(this.valueFont, str, 12, fWidth2, (boxY + 12.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f), iComputeColor, i);
        } else {
            class699Var.text(this.valueFont, strEffective, 12, fWidth2, (boxY + 12.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f), iComputeColor);
        }
        float f5 = boxY + aa + Y;
        float dropdownWidth = computeAdaptiveDropdownWidth(class699Var, f);
        float dropdownX = boxX + controlWidth - dropdownWidth;
        if (this.options.isEmpty() || (!this.expanded && this.expandAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            this.dropdownBounds.withPosition(dropdownX, f5).withSize(dropdownWidth, Math.min(fMethod007, X));
        }
        this.clickBehavior.setDimensions(boxX, boxY, controlWidth, aa);
    }

    @Override
    public void drawOverlay(DrawCtx class699Var) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.expandAnimation.isZero() || this.expanded) {
            ThemePalette class764VarPalette = class699Var.theme().palette();
            ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
            int iComputeColor = class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(500).argb());
            int iComputeColor2 = class115VarColorStack.computeColor(class764VarPalette.accent().argb());
            int iComputeColor3 = class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(300).argb());
            int iComputeColor4 = class115VarColorStack.computeColor(class764VarPalette.accentBright().argb());
            int iComputeColor5 = class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(200).argb());
            int iWhite = class115VarColorStack.white();
            class115VarColorStack.push();
            class115VarColorStack.alpha(this.expandAnimation.smoothAnimation());
            float fY = this.dropdownBounds.y();
            float fHeight = this.dropdownBounds.height();
            float fWidth = this.dropdownBounds.width();
            class699Var.fillOutlinedRoundedRect(this.dropdownBounds.x(), fY, fWidth, fHeight, 7.0f, 2.5f, class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(400).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(600).argb()));
            this.scrollArea.beginArea(class699Var, this.dropdownBounds.x(), this.dropdownBounds.y() + 10.0f, this.dropdownBounds.width(), this.dropdownBounds.height() - 10.0f);
            float f = fY + 10.0f;
            float fVisibleTop = f + this.scrollArea.visibleTop();
            float fVisibleBottom = f + this.scrollArea.visibleBottom();
            int i = 0;
            while (i < this.options.size()) {
                MultiSelectOption2<T> class837Var = this.options.get(i);
                ClickableBehavior class766Var = ((MultiSelectOption2) class837Var).clickableBehavior;
                float f2 = fY + 10.0f + (i * aa);
                if (f2 + 15.0f < fVisibleTop || f2 > fVisibleBottom) {
                    ((MultiSelectOption2) class837Var).clickableBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
                } else {
                    float fX = ((this.dropdownBounds.x() + fWidth) - (needsScrollbar() ? 20.0f : 10.0f)) - ae;
                    ((MultiSelectOption2) class837Var).clickableBehavior.setDimensions(this.dropdownBounds.x(), f2, fWidth, 15.0f + (i == this.options.size() - 1 ? 0.0f : 10.0f));
                    ToggleAnimator class323Var = class837Var.toggleAnimation();
                    class699Var.fillOutlinedRoundedRect(fX, f2, ae, 15.0f, 8.0f, 2.5f, class115VarColorStack.toggleDarkenedInterpolateColor(iComputeColor3, iComputeColor4, 0.05f, class766Var.hoverAnimation(), class323Var), class115VarColorStack.toggleDarkenedInterpolateColor(iComputeColor, iComputeColor2, 0.05f, class766Var.hoverAnimation(), class323Var));
                    float f3 = (15.0f - (2.0f * Y)) / 2.0f;
                    class699Var.circle(fX + Y + f3 + (((ae - (2.0f * Y)) - (2.0f * f3)) * class323Var.smoothAnimation()), f2 + 7.5f, f3, class115VarColorStack.interpolate(iComputeColor5, iWhite, class323Var));
                    int iInterpolate = class115VarColorStack.interpolate(class115VarColorStack.computeColor(class764VarPalette.text().tone(500).argb()), class115VarColorStack.computeColor(class764VarPalette.text().tone(100).argb()), class323Var);
                    T t = (T) (((MultiSelectOption2) class837Var).option);
                    if (t instanceof DisplayNamed) {
                        String optionName = ((DisplayNamed) t).getDisplayName().effective();
                        float textX = this.dropdownBounds.x() + 10.0f;
                        float textWidth = Math.max(16.0f, fX - textX - 10.0f);
                        float textY = (f2 + 7.5f) - Math.round(this.valueFont.getHeight(Z) / 2.0f);
                        drawDropdownOptionText(
                                class699Var, optionName, textX, textY, textWidth, iInterpolate
                        );
                    }
                }
                i++;
            }
            this.scrollArea.endArea(class699Var, computeContentHeight() - 10.0f);
            if (needsScrollbar()) {
                this.scrollbar.render(class699Var);
            }
            class115VarColorStack.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.enumIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(12, 12);
        this.multiEnumIcon.setDimensions(15, 15);
        this.scrollArea.layout(class698Var);
        if (needsScrollbar()) {
            this.scrollbar.layout(class698Var);
            this.scrollbar.setSize(4.0f, Math.min(computeContentHeight(), X));
            this.scrollbar.setPosition(((this.dropdownBounds.x() + this.dropdownBounds.width()) - 10.0f) - this.scrollbar.width(), (this.dropdownBounds.y() + 10.0f) - 4.0f);
        }
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        if (this.visibleCondition != null) {
            visible(this.visibleCondition.get().booleanValue());
        }
        this.expandAnimation.animate(class141Var);
        this.clickBehavior.animate(class141Var);
        if (needsScrollbar()) {
            this.scrollbar.animation(class141Var);
        }
        this.highlightAnimation.animate(class141Var);
        for (MultiSelectOption2<T> class837Var : this.options) {
            class837Var.clickableBehavior().hoverAnimation().force(
                    class837Var.clickableBehavior().hoverAnimation().state());
            class837Var.toggleAnimation().animate(class141Var);
        }
        Set<T> setOf = this.selectedSupplier != null ? this.selectedSupplier.get() : Set.of();
        for (MultiSelectOption2<T> class837Var2 : this.options) {
            class837Var2.toggleAnimation().state(setOf.contains(class837Var2.option()));
            class837Var2.toggleAnimation().animate(class141Var);
            class837Var2.clickableBehavior().hoverAnimation().force(
                    class837Var2.clickableBehavior().hoverAnimation().state());
        }
        this.scrollArea.animation(class141Var);
        super.animation(class141Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean z2 = false;
        if (this.expanded) {
            z2 = this.scrollArea.handleInput(class688Var, z);
            if (needsScrollbar() && this.scrollbar.handleInput(class688Var, z)) {
                z2 = true;
            }
            InputEventContext class688VarWithMouseOffset = class688Var.withMouseOffset(0.0f, this.scrollArea.scrollY());
            for (int size = this.options.size() - 1; size >= 0; size--) {
                if (this.options.get(size).clickableBehavior().handleInput(class688VarWithMouseOffset, z || z2)) {
                    z2 = true;
                }
            }
            if (!z && !z2) {
                if ((class688Var.inputEvent() instanceof MouseButtonInput) && class688Var.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                if ((class688Var.inputEvent() instanceof CursorMoveInput) && class688Var.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
            }
            if ((class688Var.inputEvent() instanceof ScrollInput) && !class688Var.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                closeDropdown();
                return true;
            }
            if (!z && (class688Var.inputEvent() instanceof MouseButtonInput)) {
                boolean zInArea = class688Var.inArea(this.clickBehavior.ownerX(), this.clickBehavior.ownerY(), this.clickBehavior.ownerW(), this.clickBehavior.ownerH());
                boolean zInArea2 = class688Var.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
                if (!zInArea && !zInArea2) {
                    closeDropdown();
                    return true;
                }
            }
        }
        if (this.clickBehavior.handleInput(class688Var, z || z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void handleClose() {
        this.scrollArea.scrollTo(0.0f);
        closeDropdown();
        Spectra.INSTANCE.eventDispatcher().unregister(LanguageChangeEvent.class, this.languageChangeCallback);
        super.handleClose();
    }

    @Override
    public void handleViewportVisibility(float f, float f2, float f3) {
        if (this.expanded || !this.expandAnimation.isZero()) {
            float fY = y() - f3;
            if (fY + height() < f || fY > f2) {
                closeDropdown();
            }
        }
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(textWidth(this.parent.width()));
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += Y + this.descriptionHeight;
        }
        return Math.max(aa, height);
    }

    public float computeContentHeight() {
        if (this.expandAnimation.isZero() && !this.expanded) {
            return 0.0f;
        }
        int size = this.options.size();
        return 20.0f + (size * 15.0f) + (Math.max(0, size - 1) * 10.0f);
    }

    public void closeDropdown() {
        if (this.expanded || !this.expandAnimation.isZero()) {
            this.expanded = false;
            this.expandAnimation.state(false);
            this.scrollArea.scrollTo(0.0f);
        }
    }

    public boolean needsScrollbar() {
        return computeContentHeight() > X;
    }

    public void invert() {
        this.expanded = !this.expanded;
        this.expandAnimation.state(this.expanded);
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
            this.cachedWidth = f;
            this.cachedDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    private float textWidth(float totalWidth) {
        return Math.max(68.0f, totalWidth - controlWidth - controlGap);
    }

    private float computeAdaptiveDropdownWidth(DrawCtx context, float availableWidth) {
        float desiredWidth = controlWidth;
        for (MultiSelectOption2<T> option : this.options) {
            T value = option.option();
            String label = value instanceof DisplayNamed displayNamed
                    ? displayNamed.getDisplayName().effective()
                    : value.name();
            desiredWidth = Math.max(
                    desiredWidth,
                    context.textWidthPhysical(this.valueFont, label, 12) + 62.0f
            );
        }
        return Math.min(availableWidth, desiredWidth);
    }

    private void drawDropdownOptionText(
            DrawCtx context, String text, float x, float y, float maxWidth, int color
    ) {
        if (context.textWidthPhysical(this.valueFont, text, 12) <= maxWidth) {
            context.text(this.valueFont, text, 12, x, y, color);
            return;
        }
        String ellipsis = "...";
        float ellipsisWidth = context.textWidthPhysical(this.valueFont, ellipsis, 12);
        String truncated = "";
        for (int length = text.length(); length > 0; length--) {
            String candidate = text.substring(0, length);
            if (context.textWidthPhysical(this.valueFont, candidate, 12) + ellipsisWidth <= maxWidth) {
                truncated = candidate;
                break;
            }
        }
        context.text(this.valueFont, truncated + ellipsis, 12, x, y, color);
    }

    public Translation name() {
        return this.name;
    }

    public MultiSelectSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}

