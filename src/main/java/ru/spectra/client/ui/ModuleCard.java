package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.type.BindMode;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ButtonSetting;
import ru.spectra.client.ui.setting.ButtonSettingElement;
import ru.spectra.client.ui.setting.CheckboxSettingElement;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ColorSettingElement;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.util.ConfigAutoSaveScheduler;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.ui.setting.EnumSetting;
import ru.spectra.client.ui.setting.ExpandableSetting;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.ui.setting.GroupSettingElement;
import ru.spectra.client.render.HighlightAnimation;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.ui.setting.KeybindSetting;
import ru.spectra.client.ui.setting.KeybindSettingElement;
import ru.spectra.client.Lang;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.ModeSettingElement;
import ru.spectra.client.module.Module;
import ru.spectra.client.module.ServerAccessPolicy;
import ru.spectra.client.module.SoundsModule;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.MultiSelectSettingElement;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.ui.setting.OrderedEnumSetting;
import ru.spectra.client.ui.setting.OrderedListSettingElement;
import ru.spectra.client.util.OverlayCommandQueue;
import ru.spectra.client.util.RenderCommandQueue;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.ui.setting.SeparatorSetting;
import ru.spectra.client.ui.setting.Setting;
import ru.spectra.client.ui.setting.SettingDescription;
import ru.spectra.client.ui.setting.SliderSettingElement;
import ru.spectra.client.ui.setting.SoundModeSettingElement;
import ru.spectra.client.ui.setting.TextFieldSetting;
import ru.spectra.client.ui.setting.TextFieldSettingElement;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.type.Language;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.model.WidgetBounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gl.Framebuffer;

public class ModuleCard extends AbstractFrame {
    private static final int BLURRED_BORDER = 0xFF17181C;
    private static final int BLURRED_EXPANDED_BORDER = 0xFF1D1F24;
    public final GlTexture frameIcon;
    public final GlTexture starIcon;
    public final GlTexture starFilledIcon;
    public final MsdfFont font;
    static final float BASE_HEIGHT = 64.0f;
    static final float DESCRIPTION_WIDTH = 165.0f;
    static final float DESCRIPTION_LINE_HEIGHT = 13.0f;
    static final float SETTINGS_CONTENT_TOP = 20.0f;
    static final float SETTINGS_SEPARATOR_Y = 9.0f;
    static final float SETTINGS_HEADER_OVERLAP = 18.0f;
    static final float SERVER_POLICY_HIT_SIZE = 20.0f;
    public final Module module;
    public final String[] aliases;
    public final String name;
    public final ToggleSwitch toggleSwitch;

    public final KeybindField keybindField;
    public final MsdfFont clearBindFont;
    public final ClickableBehavior clearBindButton =
            new ClickableBehavior().clickCallback(this::clearModuleBind);
    public final ToggleAnimator clearBindAnimation =
            new ToggleAnimator(160, Easings.EASE_IN_OUT_CUBIC);
    private List<String> descriptionLines = List.of();
    private Language descriptionLanguage;
    private float headerHeight = BASE_HEIGHT;
    static final float columnWidth = 271.0f;
    static final float padding = 13.0f;
    public boolean favorite;
    public final TextureToggleWidget favoriteToggle;
    public final ToggleAnimator nameHoverAnimation;
    public final ToggleAnimator serverPolicyHoverAnimation;
    public float nameX;
    public float nameY;
    public float nameWidth;
    public float nameHeight;
    private float serverPolicyIndicatorX;
    private float serverPolicyIndicatorY;
    private boolean serverPolicyIndicatorHovered;
    public final AnimatedFloat xAnimation;
    public final AnimatedFloat yAnimation;

    public final KeybindConfigWindow keybindWindow;
    public final WidgetBounds keybindBounds;
    public float scrollOffset;

    public final HighlightAnimation highlightAnimation;
    public final ToggleAnimator expansionAnimation;

    public ScreenResolution resolution;
    public boolean positionInitialized;
    public static final float gap = 8.0f;
    public static final float screenMargin = 5.0f;
    public boolean dragging;
    public boolean expanded;
    private boolean cardHovered;
    public final ClickableBehavior headerClick = new ClickableBehavior();

    public ModuleCard(Module class605Var, String[] strArr) {
        super(new FrameElementColumn(columnWidth, 11.0f));
        this.frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png"));
        this.starIcon = new GlTexture(new ClasspathResource("/icons/menu/new/star.png"));
        this.starFilledIcon = new GlTexture(new ClasspathResource("/icons/menu/new/star_fill.png"));
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.nameHoverAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
        this.serverPolicyHoverAnimation = new ToggleAnimator(160, Easings.EASE_IN_OUT_CUBIC);
        this.xAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.yAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
        this.keybindBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.expansionAnimation = new ToggleAnimator(210, Easings.EASE_IN_OUT_CUBIC);
        this.module = class605Var;
        this.aliases = strArr;
        this.name = class605Var.getName();
        updateDescriptionLayout();
        this.clearBindFont = Fonts.MENU_ICON.get();
        this.favorite = Spectra.INSTANCE.configManager().menuStateConfig().isModuleFavorite(this.name);
        Objects.requireNonNull(class605Var);
        this.toggleSwitch = new ToggleSwitch(34.0f, 20.0f, class605Var::isState);
        ToggleSwitch class754Var = this.toggleSwitch;
        Objects.requireNonNull(class605Var);
        class754Var.runnable(wrapWithAutoSave(class605Var::switchState));
        Translation class254Var = Lang.BINDING_MODULE;
        String name = class605Var.getName();
        Objects.requireNonNull(class605Var);
        this.keybindWindow = new KeybindConfigWindow(class254Var, name, class605Var::getKeyBind);
        this.keybindWindow.availability = this.module::isVisibleInMenu;
        KeybindConfigWindow class755Var = this.keybindWindow;
        Objects.requireNonNull(class605Var);
        Supplier<BindMode> supplier = class605Var::getType;
        Objects.requireNonNull(class605Var);
        class755Var.bindType(supplier, class605Var::setType);
        KeybindConfigWindow class755Var2 = this.keybindWindow;
        Objects.requireNonNull(class605Var);
        class755Var2.onChange(wrapConsumerWithAutoSave(class605Var::setKey));
        Spectra.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        this.favoriteToggle = new TextureToggleWidget(this.starIcon, this.starFilledIcon, () -> {
            return Boolean.valueOf(this.favorite);
        }, padding, padding);
        this.favoriteToggle.runnable(this::invertFavorite);
        this.keybindField = new KeybindField(Fonts.INTER_SEMIBOLD.get(), () -> {
            toggleKeybindWindow();
        }, 7.0f, 4.0f, 4.0f, 10).addMutableKeys(class605Var.getKeyBind());
        KeybindField class743Var = this.keybindField;
        Objects.requireNonNull(class605Var);
        class743Var.onChange(wrapConsumerWithAutoSave(class605Var::setKey));
        this.headerClick.clickCallback(this::toggleExpanded);
        class605Var.getSettings().forEach(this::addSettingElement);
        addChild(this.toggleSwitch);
        addChild(this.favoriteToggle);
        addChild(this.keybindField);
        this.favoriteToggle.visible(false);
        this.frameElementsContainer.visible(false);
    }

    private void addSettingElement(Setting setting) {
        if (setting instanceof BooleanSetting booleanSetting) {
            CheckboxSettingElement element = new CheckboxSettingElement(
                    booleanSetting.getName(), descriptionFor(booleanSetting),
                    booleanSetting::getKeyBind, false);
            element.toggledSupplier(booleanSetting::isValue);
            element.changeRunnable(wrapWithAutoSave(() -> {
                booleanSetting.switchValue();
                SoundsModule.playSettingToggle(booleanSetting.isValue());
            }));
            element.visibleSupplier(booleanSetting.getVisible());
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof NumberSetting numberSetting) {
            SliderSettingElement element = new SliderSettingElement(
                    numberSetting.getName(), descriptionFor(numberSetting), numberSetting.valueUnit(),
                    numberSetting.max(), numberSetting.min(), numberSetting.step());
            element.currentValueSupplier(numberSetting::currentValue);
            element.onValueChanged(wrapConsumerWithAutoSave(value -> {
                numberSetting.setCurrentValue(value);
                SoundsModule.playSliderMove();
            }));
            element.visibleSupplier(numberSetting.getVisible());
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof ModeSetting modeSetting) {
            addModeElement(modeSetting);
            return;
        }
        if (setting instanceof EnumSetting enumSetting) {
            addSoundModeElement(enumSetting);
            return;
        }
        if (setting instanceof MultiSelectSetting multiSelectSetting) {
            addMultiSelectElement(multiSelectSetting);
            return;
        }
        if (setting instanceof TextFieldSetting textSetting) {
            TextFieldSettingElement element = new TextFieldSettingElement(
                    textSetting.getName(), descriptionFor(textSetting), textSetting.getPlaceholder(),
                    textSetting.isOnlyDigits(), textSetting.getMax(), textSetting.isPassword(), textSetting::getText);
            element.onChange(wrapConsumerWithAutoSave(textSetting::setText));
            element.visibleSupplier(textSetting.getVisible());
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof ButtonSetting buttonSetting) {
            ButtonSettingElement element = new ButtonSettingElement(
                    buttonSetting.getName(), descriptionFor(buttonSetting), buttonSetting.getButtonName(),
                    wrapWithAutoSave(buttonSetting.getRunnable()));
            element.visibleSupplier(buttonSetting.getVisible());
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof OrderedEnumSetting orderedSetting) {
            addOrderedListElement(orderedSetting);
            return;
        }
        if (setting instanceof ColorSetting colorSetting) {
            ColorSettingElement element = new ColorSettingElement(
                    colorSetting.getName(), descriptionFor(colorSetting),
                    colorSetting::getColorWithoutAlpha, colorSetting::getAlpha);
            element.visibleSupplier(colorSetting.getVisible());
            element.colorConsumer(colorSetting::setColor);
            element.alphaConsumer(colorSetting::setAlpha);
            element.commitRunnable(this::scheduleAutoSave);
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof ExpandableSetting expandableSetting) {
            CheckboxSettingElement groupToggle = new CheckboxSettingElement(
                    expandableSetting.getName(), descriptionFor(expandableSetting),
                    expandableSetting::getKeybinds, false);
            groupToggle.toggledSupplier(expandableSetting::isValue);
            groupToggle.changeRunnable(wrapWithAutoSave(() -> {
                expandableSetting.switchValue();
                SoundsModule.playSettingToggle(expandableSetting.isValue());
            }));
            groupToggle.visibleSupplier(expandableSetting.getVisible());
            this.frameElementsContainer.addFrameElement(groupToggle);
            if (!expandableSetting.getSubSettings().isEmpty()) {
                expandableSetting.getSubSettings().forEach(this::addSettingElement);
            }
            return;
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            KeybindSettingElement element = new KeybindSettingElement(
                    keybindSetting.getName(), descriptionFor(keybindSetting), keybindSetting::getKeyBind);
            element.visibleSupplier(keybindSetting.getVisible());
            element.onChange(wrapConsumerWithAutoSave(keybindSetting::setKey));
            this.frameElementsContainer.addFrameElement(element);
            return;
        }
        if (setting instanceof SeparatorSetting) {
            return;
        }
    }

    private Translation descriptionFor(Setting setting) {
        return SettingDescription.resolve(this.module.getName(), setting);
    }

    public <E extends Enum<E>> void addOrderedListElement(OrderedEnumSetting<E> class609Var) {
        Objects.requireNonNull(class609Var);
        Supplier<List<E>> supplier = class609Var::orderedValues;
        Objects.requireNonNull(class609Var);
        Supplier<Set<E>> supplier2 = class609Var::getSelected;
        Objects.requireNonNull(class609Var);
        OrderedListSettingElement<E> class831Var = new OrderedListSettingElement<>(supplier, supplier2, class609Var::optionName, wrapConsumerWithAutoSave(e -> {
            if (class609Var.isSelected(e)) {
                class609Var.deselect(e);
            } else {
                class609Var.select(e);
            }
        }), (num, num2) -> {
            class609Var.move(num.intValue(), num2.intValue());
            scheduleAutoSave();
        });
        class831Var.visibleSupplier(class609Var.getVisible());
        this.frameElementsContainer.addFrameElement(class831Var);
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
        float fX = this.keybindBounds.x();
        float fY = this.keybindBounds.y() - this.scrollOffset;
        float fWidth2 = this.keybindBounds.width();
        float fHeight = this.keybindBounds.height();
        float f2 = fX + fWidth2 + gap;
        float f3 = (fX - gap) - fWidth;
        if (f2 + fWidth > fScreenWidth - screenMargin && f3 >= screenMargin) {
            f2 = f3;
        }
        float fMax = Math.max(screenMargin, Math.min(f2, (fScreenWidth - fWidth) - screenMargin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - screenMargin) {
            f4 = (fScreenHeight - f) - screenMargin;
        }
        if (f4 < screenMargin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
    }

    public <E extends Enum<E>> void addModeElement(ModeSetting<E> class669Var) {
        ModeSettingElement class825Var = new ModeSettingElement(
                class669Var.getName(), descriptionFor(class669Var), class669Var.options());
        Objects.requireNonNull(class669Var);
        class825Var.onChangeCallback(wrapConsumerWithAutoSave(class669Var::setCurrentValue));
        Objects.requireNonNull(class669Var);
        class825Var.currentValueSupplier(class669Var::currentValue);
        class825Var.visibleSupplier(class669Var.getVisible());
        this.frameElementsContainer.addFrameElement(class825Var);
    }

    public <E extends Enum<E>> void addMultiSelectElement(MultiSelectSetting<E> class671Var) {
        Translation name = class671Var.getName();
        Translation description = descriptionFor(class671Var);
        Enum[] enumArrOptions = class671Var.options();
        Objects.requireNonNull(class671Var);
        Supplier supplier = class671Var::selectedValues;
        Objects.requireNonNull(class671Var);
        MultiSelectSettingElement class836Var = new MultiSelectSettingElement(name, description, enumArrOptions, supplier, wrapConsumerWithAutoSave(class671Var::toggle));
        class836Var.visibleSupplier(class671Var.getVisible());
        this.frameElementsContainer.addFrameElement(class836Var);
    }

    public <E extends Enum<E>> void addSoundModeElement(EnumSetting<E> class610Var) {
        SoundModeSettingElement class833Var = new SoundModeSettingElement(
                class610Var.getName(), descriptionFor(class610Var),
                class610Var.options(), class610Var.soundAction());
        Objects.requireNonNull(class610Var);
        class833Var.onChangeCallback(wrapConsumerWithAutoSave(class610Var::setCurrentValue));
        Objects.requireNonNull(class610Var);
        class833Var.currentValueSupplier(class610Var::currentValue);
        class833Var.visibleSupplier(class610Var.getVisible());
        this.frameElementsContainer.addFrameElement(class833Var);
    }

    public void scheduleAutoSave() {
        ConfigAutoSaveScheduler.scheduleAutoSave();
    }

    public Runnable wrapWithAutoSave(Runnable runnable) {
        return () -> {
            if (runnable != null) {
                runnable.run();
            }
            scheduleAutoSave();
        };
    }

    public <T> Consumer<T> wrapConsumerWithAutoSave(Consumer<T> consumer) {
        return obj -> {
            consumer.accept(obj);
            scheduleAutoSave();
        };
    }

    public List<Setting> settings() {
        return this.module.getSettings();
    }

    @Override
    public void updateViewportVisibility(float f, float f2, float f3) {
        this.scrollOffset = f;
        super.updateViewportVisibility(f, f2, f3);
    }

    @Override
    public void render(DrawCtx class699Var) {
        if (!this.module.isVisibleInMenu()) {
            return;
        }
        if (this.descriptionLanguage != ClientLocalization.language()) {
            updateDescriptionLayout();
        }
        this.resolution = class699Var.resolution();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        float reveal = this.expansionAnimation.smoothAnimation();
        float cardHeight = height() + contentHeight();
        if (Spectra.INSTANCE.configManager().menuStateConfig().menuSurfaceStyle()
                == SurfaceStyle.BLURRED) {
            Framebuffer blurred = Spectra.INSTANCE.windowController()
                    .headerBlur().getBlurFramebuffer();
            int blurTexture = blurred == null ? -1 : blurred.getColorAttachment();
            int border = class115VarColorStack.interpolate(
                    BLURRED_BORDER, BLURRED_EXPANDED_BORDER, reveal);
            class699Var.glassPanel(
                    blurTexture,
                    x(), y(), width(), cardHeight,
                    9.0f, SpectraHudStyle.BORDER_WIDTH,
                    border,
                    SpectraHudStyle.color(class115VarColorStack, MenuWindow.BLURRED_SURFACE)
            );
        } else {
            int borderAlpha = Math.round(33.0f + 7.0f * reveal);
            class699Var.fillRoundedRect(x(), y(), width(), cardHeight, 9.0f,
                    class115VarColorStack.computeColor(0x696976, borderAlpha));
            class699Var.fillRoundedRect(
                    x() + 1.0f, y() + 1.0f, width() - 2.0f, cardHeight - 2.0f, 8.0f,
                    class115VarColorStack.computeColor(0xFF080809)
            );
        }
        this.nameX = x() + padding;
        this.nameY = y() + 11.0f;
        this.nameWidth = class699Var.textWidthPhysical(this.font, this.name, 14);
        this.nameHeight = this.font.getHeight(14.0f);
        class699Var.text(this.font, this.name, 14, this.nameX, this.nameY,
                class115VarColorStack.computeColor(0xF6F6FA, 245));
        renderServerPolicyIndicator(class699Var, class115VarColorStack);
        for (int index = 0; index < this.descriptionLines.size(); index++) {
            class699Var.text(
                    this.font,
                    this.descriptionLines.get(index),
                    12,
                    this.nameX,
                    y() + 32.0f + index * DESCRIPTION_LINE_HEIGHT,
                    class115VarColorStack.computeColor(0xAAAAB4, 184)
            );
        }
        float revealHeight = contentHeight();
        if (!this.expansionAnimation.isZero() && revealHeight > 0.0f) {
            LayoutScaleContext layout = class699Var.layoutContext();
            class699Var.drawEngine().beginScissor(
                    class699Var.matrixStack().peek().getPositionMatrix(),
                    layout.toPhysical(x() + 1.0f),
                    layout.toPhysical(settingsContentOriginY()),
                    layout.toPhysical(width() - 2.0f),
                    layout.toPhysical(revealHeight + SETTINGS_HEADER_OVERLAP)
            );
            class115VarColorStack.push();
            class115VarColorStack.alpha(reveal);
            class699Var.fillRect(
                    x() + padding,
                    settingsContentOriginY() + SETTINGS_SEPARATOR_Y,
                    width() - padding * 2.0f,
                    1.0f,
                    class115VarColorStack.computeColor(0x696976, 52)
            );
            renderFrameElements(class699Var);
            class115VarColorStack.pop();
            class699Var.drawEngine().endScissor();
        }
        this.keybindField.render(class699Var);
        this.keybindBounds.withPosition(this.keybindField.x(), this.keybindField.y()).withSize(this.keybindField.width(), this.keybindField.height());
        renderClearBindButton(class699Var, class115VarColorStack);
        this.toggleSwitch.render(class699Var);
        this.toggleSwitch.setClickableArea(this.toggleSwitch.x(), this.toggleSwitch.y(),
                this.toggleSwitch.width(), this.toggleSwitch.height());
        this.headerClick.setDimensions(x(), y(), width(), height());
    }

    private void renderServerPolicyIndicator(DrawCtx ctx, ColorStack colors) {
        if (!this.module.serverAccessPolicy().isRestricted()) {
            this.serverPolicyIndicatorHovered = false;
            return;
        }
        this.serverPolicyIndicatorX = this.nameX + this.nameWidth + 8.0f;
        this.serverPolicyIndicatorY = this.nameY + this.nameHeight / 2.0f;
        float hover = this.serverPolicyHoverAnimation.smoothAnimation();
        ctx.circle(
                this.serverPolicyIndicatorX,
                this.serverPolicyIndicatorY,
                3.7f + hover * 0.8f,
                colors.computeColor(ctx.theme.palette().accent().argb())
        );
    }

    private void updateDescriptionLayout() {
        this.descriptionLanguage = ClientLocalization.language();
        String value = ModuleDescriptions.forModule(this.name);
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.strip().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && this.font.getWidth(candidate, 12.0f) > DESCRIPTION_WIDTH) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        this.descriptionLines = List.copyOf(lines);
        this.headerHeight = BASE_HEIGHT
                + Math.max(0, this.descriptionLines.size() - 1) * DESCRIPTION_LINE_HEIGHT;
    }

    private void renderClearBindButton(DrawCtx ctx, ColorStack colors) {
        float animation = this.clearBindAnimation.smoothAnimation();
        if (animation <= 0.01f) {
            return;
        }
        colors.push();
        colors.alpha(animation);
        float buttonX = this.clearBindButton.ownerX();
        float buttonY = this.clearBindButton.ownerY();
        float hover = this.clearBindButton.hoverAnimation().smoothAnimation();
        String clearGlyph = "\u044C";
        ctx.text(this.clearBindFont, clearGlyph, 12,
                buttonX + (18.0f - this.clearBindFont.getWidth(clearGlyph, 12.0f)) / 2.0f,
                buttonY + 4.0f,
                colors.computeColor(0xD6D6DE, Math.round(180.0f + 75.0f * hover)));
        colors.pop();
    }

    private void clearModuleBind() {
        this.module.setKey(List.of());
        this.keybindField.syncKeys(this.module.getKeyBind());
        scheduleAutoSave();
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.dragging = z;
        if (z && this.positionInitialized) {
            this.xAnimation.set(x());
            this.yAnimation.set(y());
            this.xAnimation.destination(x());
            this.yAnimation.destination(y());
        }
    }

    @Override
    public void renderOverlays(DrawCtx class699Var) {
        if (!this.module.isVisibleInMenu()) {
            return;
        }
        if (!this.expansionAnimation.isZero() && !this.expansionAnimation.isOne()) {
            LayoutScaleContext layout = class699Var.layoutContext();
            ColorStack colors = class699Var.drawEngine().colorStack();
            class699Var.drawEngine().beginScissor(
                    class699Var.matrixStack().peek().getPositionMatrix(),
                    layout.toPhysical(x() + 1.0f),
                    layout.toPhysical(settingsContentOriginY()),
                    layout.toPhysical(width() - 2.0f),
                    layout.toPhysical(contentHeight() + SETTINGS_HEADER_OVERLAP)
            );
            colors.push();
            colors.alpha(this.expansionAnimation.smoothAnimation());
            renderFrameOverlays(class699Var);
            colors.pop();
            class699Var.drawEngine().endScissor();
        } else if (!this.expansionAnimation.isZero()) {
            renderFrameOverlays(class699Var);
        }
        renderServerPolicyTooltip(class699Var);
    }

    private void renderServerPolicyTooltip(DrawCtx ctx) {
        float animation = this.serverPolicyHoverAnimation.smoothAnimation();
        if (!this.module.serverAccessPolicy().isRestricted() || animation <= 0.01f) {
            return;
        }
        ServerAccessPolicy.AvailabilityNotice notice =
                this.module.serverAccessPolicy().availabilityNotice();
        String servers = String.join(", ", notice.servers());
        MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
        MsdfFont textFont = Fonts.INTER_MEDIUM.get();
        float titleSize = 11.0f;
        float textSize = 10.0f;
        float contentWidth = titleFont.getWidth(notice.title(), titleSize);
        if (!servers.isBlank()) {
            contentWidth = Math.max(contentWidth, textFont.getWidth(servers, textSize));
        }
        float tooltipWidth = contentWidth + 18.0f;
        float tooltipHeight = servers.isBlank() ? 28.0f : 43.0f;
        float tooltipX = this.serverPolicyIndicatorX - 6.0f;
        float tooltipY = this.serverPolicyIndicatorY + 11.0f;
        ColorStack colors = ctx.drawEngine().colorStack();
        colors.push();
        colors.alpha(animation);
        ctx.fillRoundedRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 7.0f,
                colors.computeColor(0x202027, 235));
        ctx.fillRoundedRect(tooltipX + 1.35f, tooltipY + 1.35f,
                tooltipWidth - 2.7f, tooltipHeight - 2.7f, 5.65f,
                colors.computeColor(0xFF080809));
        ctx.text(titleFont, notice.title(), 11, tooltipX + 9.0f, tooltipY + 7.0f,
                colors.computeColor(0xF1F1F5));
        if (!servers.isBlank()) {
            ctx.text(textFont, servers, 10, tooltipX + 9.0f, tooltipY + 23.0f,
                    colors.computeColor(ctx.theme.palette().accent().argb()));
        }
        colors.pop();
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.keybindWindow.layout(class698Var);
        this.frameElementsContainer.visible(!this.expansionAnimation.isZero());
        this.toggleSwitch.setSize(34.0f, 20.0f);
        this.toggleSwitch.setPosition(((x() + width()) - padding) - this.toggleSwitch.width(), (y() + (height() / 2.0f)) - (this.toggleSwitch.height() / 2.0f));
        this.keybindField.layout(class698Var);
        float bindWidth = this.keybindField.width();
        float fX = this.toggleSwitch.x() - 9.0f - bindWidth;
        this.keybindField.setPosition(fX, (y() + (height() / 2.0f)) - (this.keybindField.height() / 2.0f));
        this.clearBindButton.setDimensions(
                this.keybindField.x() - 23.0f,
                (y() + height() / 2.0f) - 9.0f,
                18.0f,
                18.0f
        );
        this.frameElementsContainer.setPosition(
                x() + padding,
                settingsContentOriginY() + SETTINGS_CONTENT_TOP
        );
        super.layout(class698Var);
    }

    public void invertFavorite() {
        this.favorite = !this.favorite;
        try {
            Spectra.INSTANCE.configManager().menuStateConfig().setModuleFavorite(this.name, this.favorite);
            Spectra.INSTANCE.configManager().saveMenuState();
        } catch (IOException e) {
            Spectra.LOGGER.error("Failed to save module favorite state", e);
        }
        Spectra.INSTANCE.tabsController().current().markFramesDirty();
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (!this.positionInitialized || !z) {
            this.positionInitialized = true;
            snapTo(f, f2);
        } else if (this.dragging) {
            snapTo(f, f2);
        } else {
            this.xAnimation.destination(f);
            this.yAnimation.destination(f2);
        }
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.xAnimation.set(f);
        this.yAnimation.set(f2);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        if (!this.module.isVisibleInMenu()) {
            return false;
        }
        if (class688Var.inputEvent() instanceof CursorMoveInput) {
            this.cardHovered = !z
                    && class688Var.inArea(x(), y(), width(), height());
            this.serverPolicyIndicatorHovered = !z
                    && this.module.serverAccessPolicy().isRestricted()
                    && class688Var.inArea(
                    this.serverPolicyIndicatorX - SERVER_POLICY_HIT_SIZE / 2.0f,
                    this.serverPolicyIndicatorY - SERVER_POLICY_HIT_SIZE / 2.0f,
                    SERVER_POLICY_HIT_SIZE,
                    SERVER_POLICY_HIT_SIZE
            );
        }
        this.nameHoverAnimation.state(class688Var.inArea(this.nameX, this.nameY, this.nameWidth, this.nameHeight) && !z);
        if (!z && this.module.hasKeyBind() && !this.clearBindAnimation.isZero()
                && this.clearBindButton.handleInput(class688Var, false)) {
            return true;
        }
        boolean handled = super.handleInput(class688Var, z);
        return handled | this.headerClick.handleInput(class688Var, handled);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue class677Var) {
        super.collectBlurElements(class677Var);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue class676Var) {
        this.keybindWindow.collectBloomElements(class676Var);
        super.collectBloomElements(class676Var);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.nameHoverAnimation.animate(class141Var);
        this.serverPolicyHoverAnimation
                .state(this.serverPolicyIndicatorHovered)
                .animate(class141Var);
        this.headerClick.animate(class141Var);
        this.clearBindAnimation
                .state(this.module.hasKeyBind()
                        && (this.cardHovered
                        || this.clearBindButton.hoverAnimation().smoothAnimation() > 0.01f))
                .animate(class141Var);
        this.clearBindButton.animate(class141Var);
        this.keybindWindow.animation(class141Var);
        this.expansionAnimation.animate(class141Var);
        if (!this.expansionAnimation.finished()) {
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.keybindField.syncKeys(this.module.getKeyBind());
        if (this.positionInitialized) {
            this.xAnimation.animate(class141Var);
            this.yAnimation.animate(class141Var);
            setPosition(this.xAnimation.animatedValue(), this.yAnimation.animatedValue());
        }
        this.highlightAnimation.animate(class141Var);
        super.animation(class141Var);
    }

    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public float width() {
        return 297.0f;
    }

    @Override
    public float height() {
        return this.headerHeight;
    }

    @Override
    public float contentHeight() {
        if (this.expansionAnimation.isZero() || this.frameElementsContainer.height() <= 0.0f) {
            return 0.0f;
        }
        return (SETTINGS_CONTENT_TOP + this.frameElementsContainer.height() + 12.0f
                - SETTINGS_HEADER_OVERLAP)
                * this.expansionAnimation.smoothAnimation();
    }

    private float settingsContentOriginY() {
        return y() + height() - SETTINGS_HEADER_OVERLAP;
    }

    public void toggleExpanded() {
        if (this.module.getSettings().isEmpty()) {
            return;
        }
        boolean next = !this.expanded;
        for (AbstractFrame frame : Spectra.INSTANCE.tabsController().current().frames()) {
            if (frame instanceof ModuleCard card && card != this) {
                card.setExpanded(false);
            }
        }
        setExpanded(next);
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }
        this.expanded = expanded;
        this.expansionAnimation.state(expanded);
        Spectra.INSTANCE.tabsController().current().markFramesDirty();
    }

    public Module module() {
        return this.module;
    }

    public String[] aliases() {
        return this.aliases;
    }

    public String name() {
        return this.name;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public HighlightAnimation highlightAnimation() {
        return this.highlightAnimation;
    }
}
