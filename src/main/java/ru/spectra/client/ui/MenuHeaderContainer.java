package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.SpectraLogoRenderer;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.type.ModuleCategory;
import ru.spectra.client.type.Language;
import ru.spectra.client.type.MenuBackdrop;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.type.Mc;
import ru.spectra.client.math.Easings;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.KeyboardUtil;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.setting.KeybindSettingElement;
import ru.spectra.client.ui.setting.ModeSettingElement;
import ru.spectra.client.ui.setting.MultiSelectSettingElement;
import ru.spectra.client.ui.setting.SliderSettingElement;
import ru.spectra.client.util.ClientLocalization;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Native Spectra shell. The module and setting widgets remain the existing
 * client widgets; this class only owns the reference menu's navigation chrome.
 */
public class MenuHeaderContainer extends WidgetContainer {
    private static final int LEFT_BACKGROUND = 0xFF080809;
    private static final int RIGHT_BACKGROUND = 0xFF080809;
    // Precomposed opaque divider colors keep them on the same root fade as the
    // menu shell. A translucent divider multiplied by the opening alpha looked
    // like it appeared a frame later than the rest of the window.
    private static final int DIVIDER = 0xFF161619;
    private static final int TOPBAR_DIVIDER = 0xFF121214;
    private static final int PROFILE_DIVIDER = 0xFF161618;
    private static final int ACTIVE_ROW = 0x0BFFFFFF;
    private static final int TEXT = 0xF2F6F6FA;
    private static final int MUTED = 0xB8AAAAB4;
    private static final int WEAK = 0x7AA5A5B4;
    private static final int SEARCH_BACKGROUND = 0x0AFFFFFF;
    private static final int SEARCH_BORDER = 0x2E5A5A68;

    private static final float NAV_X = 8.0f;
    private static final float NAV_WIDTH = 172.0f;
    private static final float NAV_HEIGHT = 26.0f;
    private static final float SEARCH_X = 626.0f;
    private static final float SEARCH_Y = 11.0f;
    private static final float SEARCH_WIDTH = 176.0f;
    private static final float SEARCH_HEIGHT = 24.0f;
    private static final float GENERAL_LABEL_Y = 64.0f;
    private static final float DISPLAY_ROW_Y = 81.0f;
    private static final float VISUALS_ROW_Y = 111.0f;
    private static final float UTILITIES_ROW_Y = 141.0f;
    private static final float SETTINGS_LABEL_Y = 189.0f;
    private static final float CONFIGS_ROW_Y = 206.0f;
    private static final float THEMES_ROW_Y = 236.0f;
    private static final float SETTINGS_PANEL_WIDTH = 224.0f;
    private static final float SETTINGS_PANEL_HEIGHT = 220.0f;
    private static final float SETTINGS_TRACK_WIDTH = 202.0f;
    private static final float SETTINGS_PAGE_INSET = 22.0f;
    private static final float SETTINGS_ROW_HEIGHT = 88.0f;
    private static final float SETTINGS_ROW_START = 82.0f;

    private final MsdfFont regular = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont menuIcon = Fonts.MENU_ICON.get();
    private final TextInputField searchField = new TextInputField(this.regular, 12);
    private final ClickableBehavior[] navigation = new ClickableBehavior[View.values().length];
    private final String[] navigationIcons = new String[] {
            "j", "q", "W", "x", "\u042A", "\u0421"
    };
    private final ClickableBehavior hudPreviewToggle = new ClickableBehavior()
            .clickCallback(this::toggleHudPreview);
    private final ClickableBehavior profileSettingsButton = new ClickableBehavior()
            .clickCallback(this::openProfilePopup);
    private final ClickableBehavior profileSettingsCloseButton = new ClickableBehavior()
            .clickCallback(this::toggleProfileSettings);
    private final ToggleAnimator hudPreviewAnimation =
            new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator profileSettingsAnimation =
            new ToggleAnimator(180, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator settingsContentAnimation =
            new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    private final GlTexture settingsIcon = icon("/icons/menu/new/property.png");
    private final GlTexture profileSettingsCloseIcon = icon("/icons/menu/new/cross.png");
    private View selectedView = View.VISUALS;
    private boolean viewInitialized;
    private boolean profileSettingsOpen;
    private boolean draggingMenuScale;
    private boolean draggingHudScale;
    private boolean capturingMenuKey;
    private float pendingMenuScale = Float.NaN;
    private long lastMenuScaleMoveAt;
    private boolean menuScaleDirty;
    private final FrameElementColumn settingsColumn;
    private final SliderSettingElement menuScaleSetting;
    private final SliderSettingElement hudScaleSetting;
    private final ModeSettingElement<SurfaceStyle> hudStyleSetting;
    private final ModeSettingElement<SurfaceStyle> menuStyleSetting;
    private final MultiSelectSettingElement<MenuBackdrop> menuBackdropSetting;
    private final ModeSettingElement<Language> languageSetting;
    private final KeybindSettingElement menuKeySetting;

    public MenuHeaderContainer() {
        View[] values = View.values();
        for (int i = 0; i < values.length; i++) {
            View view = values[i];
            this.navigation[i] = new ClickableBehavior().clickCallback(() -> selectView(view));
        }
        this.searchField.maxLength(48).changeText(this::applySearch);

        this.settingsColumn = new FrameElementColumn(
                MenuWindow.CONTENT_WIDTH - SETTINGS_PAGE_INSET * 2.0f, 18.0f);
        this.menuScaleSetting = new SliderSettingElement(
                Translation.clearText("Menu scale"),
                Translation.clearText("Changes the size of the client menu."),
                SettingUnit.PERCENTS, 200.0f, 75.0f, 5.0f)
                .currentValueSupplier(() -> Spectra.INSTANCE.windowController().dpiScaleFactor() * 100.0f)
                .onValueChanged(value -> {
                    float scale = value / 100.0f;
                    Spectra.INSTANCE.windowController().setManualDpiScaleFactor(scale);
                    Spectra.INSTANCE.configManager().menuStateConfig().setDpiScale(false, scale);
                    saveMenuPreferences();
                });
        this.hudScaleSetting = new SliderSettingElement(
                Translation.clearText("HUD scale"),
                Translation.clearText("Changes the size of HUD elements."),
                SettingUnit.PERCENTS, 200.0f, 75.0f, 5.0f)
                .currentValueSupplier(() -> WidgetStack.hudScale() * 100.0f)
                .onValueChanged(value -> {
                    float scale = value / 100.0f;
                    WidgetStack.setHudScale(scale);
                    Spectra.INSTANCE.configManager().menuStateConfig().setHudScale(scale);
                    saveMenuPreferences();
                });
        this.hudStyleSetting = new ModeSettingElement<>(
                Translation.clearText(ClientLocalization.text("HUD style", "Стиль HUD")),
                Translation.clearText(ClientLocalization.text(
                        "Chooses whether HUD panels are normal or blurred.",
                        "Выбирает обычный фон HUD или фон с размытием.")),
                SurfaceStyle.values())
                .currentValueSupplier(() -> Spectra.INSTANCE.configManager()
                        .menuStateConfig().hudSurfaceStyle())
                .onChangeCallback(style -> {
                    Spectra.INSTANCE.configManager().menuStateConfig().setHudSurfaceStyle(style);
                    saveMenuPreferences();
                });
        this.menuStyleSetting = new ModeSettingElement<>(
                Translation.clearText(ClientLocalization.text("Menu style", "Стиль меню")),
                Translation.clearText(ClientLocalization.text(
                        "Chooses whether the menu panel is normal or blurred.",
                        "Выбирает обычный фон меню или фон с размытием.")),
                SurfaceStyle.values())
                .currentValueSupplier(() -> Spectra.INSTANCE.configManager()
                        .menuStateConfig().menuSurfaceStyle())
                .onChangeCallback(style -> {
                    Spectra.INSTANCE.configManager().menuStateConfig().setMenuSurfaceStyle(style);
                    saveMenuPreferences();
                });
        this.menuBackdropSetting = new MultiSelectSettingElement<>(
                Translation.clearText(ClientLocalization.text(
                        "World behind menu", "Мир за меню")),
                Translation.clearText(ClientLocalization.text(
                        "Dims or blurs the game world behind the menu.",
                        "Затемняет или размывает игровой мир за меню.")),
                MenuBackdrop.values(), this::selectedMenuBackdrops, this::toggleMenuBackdrop);
        this.languageSetting = new ModeSettingElement<>(
                Translation.clearText("Language"),
                Translation.clearText("Changes the language of descriptions and interface text."),
                Language.values())
                .currentValueSupplier(() -> Spectra.INSTANCE.languages().current())
                .onChangeCallback(language -> Spectra.INSTANCE.menuWindow().contentArea.requestLanguage(language));
        this.menuKeySetting = new KeybindSettingElement(
                Translation.clearText("Menu key"),
                Translation.clearText("Key used to open and close the client menu."),
                () -> List.of(Spectra.INSTANCE.menuWindow().openKey()))
                .showClearButton(false);
        this.menuKeySetting.onChange(keys -> {
            if (keys == null || keys.isEmpty()) {
                return;
            }
            int key = keys.get(0);
            if (key > 0 && key != 256) {
                Spectra.INSTANCE.menuWindow().setOpenKey(key);
                Spectra.INSTANCE.configManager().menuStateConfig().setMenuOpenKey(key);
                saveMenuPreferences();
            }
        });
        this.settingsColumn.addFrameElement(this.menuScaleSetting);
        this.settingsColumn.addFrameElement(this.hudScaleSetting);
        this.settingsColumn.addFrameElement(this.hudStyleSetting);
        this.settingsColumn.addFrameElement(this.menuStyleSetting);
        this.settingsColumn.addFrameElement(this.menuBackdropSetting);
        this.settingsColumn.addFrameElement(this.languageSetting);
        this.settingsColumn.addFrameElement(this.menuKeySetting);
        addChild(this.settingsColumn);
    }

    private Set<MenuBackdrop> selectedMenuBackdrops() {
        EnumSet<MenuBackdrop> selected = EnumSet.noneOf(MenuBackdrop.class);
        var preferences = Spectra.INSTANCE.configManager().menuStateConfig();
        if (preferences.isMenuDimBackgroundEnabled()) {
            selected.add(MenuBackdrop.DIM);
        }
        if (preferences.isMenuBlurBackgroundEnabled()) {
            selected.add(MenuBackdrop.BLUR);
        }
        return selected;
    }

    private void toggleMenuBackdrop(MenuBackdrop backdrop) {
        var preferences = Spectra.INSTANCE.configManager().menuStateConfig();
        if (backdrop == MenuBackdrop.DIM) {
            preferences.setMenuDimBackgroundEnabled(!preferences.isMenuDimBackgroundEnabled());
        } else {
            preferences.setMenuBlurBackgroundEnabled(!preferences.isMenuBlurBackgroundEnabled());
        }
        saveMenuPreferences();
    }

    private static GlTexture icon(String path) {
        return new GlTexture(new ClasspathResource(path)).setDimensions(12, 12);
    }

    private static int color(ColorStack colors, int argb) {
        return colors.computeColor(argb & 0xFFFFFF, (argb >>> 24) & 0xFF);
    }

    public void showDefaultView() {
        selectView(View.VISUALS);
    }

    private void selectView(View view) {
        if (this.viewInitialized && this.selectedView == view) {
            return;
        }
        if (view != View.SETTINGS) {
            this.capturingMenuKey = false;
            boolean hadPendingScale = this.menuScaleDirty;
            applyPendingMenuScale();
            this.draggingMenuScale = false;
            this.draggingHudScale = false;
            if (hadPendingScale) {
                saveMenuPreferences();
            }
        }
        this.selectedView = view;
        this.viewInitialized = true;
        switch (view) {
            case DISPLAY -> {
                Spectra.INSTANCE.tabsController().open(Spectra.INSTANCE.tabsController().render());
                Spectra.INSTANCE.tabsController().render().applyCategoryFilter(Set.of(ModuleCategory.INTERFACE));
            }
            case VISUALS -> {
                Spectra.INSTANCE.tabsController().open(Spectra.INSTANCE.tabsController().render());
                Spectra.INSTANCE.tabsController().render().applyCategoryFilter(Set.of(ModuleCategory.VISUALIZATION, ModuleCategory.WORLD));
            }
            case UTILITIES -> {
                Spectra.INSTANCE.tabsController().open(Spectra.INSTANCE.tabsController().misc());
                Spectra.INSTANCE.tabsController().misc().applyCategoryFilter(Set.of());
            }
            case CONFIGS -> Spectra.INSTANCE.tabsController().open(Spectra.INSTANCE.tabsController().config());
            case THEMES -> Spectra.INSTANCE.tabsController().open(Spectra.INSTANCE.tabsController().theme());
            case SETTINGS -> {
                this.capturingMenuKey = false;
                this.draggingMenuScale = false;
                this.draggingHudScale = false;
            }
        }
        if (view == View.SETTINGS) {
            this.settingsContentAnimation.force(false);
            this.settingsContentAnimation.state(true);
        } else {
            MenuTabElement current = Spectra.INSTANCE.tabsController().current();
            if (current != null) {
                current.restartContentAnimation();
            }
        }
        applySearch(this.searchField.text());
    }

    private void applySearch(String query) {
        MenuTabElement current = Spectra.INSTANCE.tabsController().current();
        if (current != null) {
            current.setSearchQuery(query);
        }
    }

    public void setSearchText(String text) {
        this.searchField.setText(text);
        this.searchField.focused(false);
    }

    @Override
    public void render(DrawCtx ctx) {
        ColorStack colors = ctx.colorStack();
        boolean blurredSurface = Spectra.INSTANCE.configManager().menuStateConfig()
                .menuSurfaceStyle() == SurfaceStyle.BLURRED;
        if (!blurredSurface) {
            ctx.fillRoundedRect(
                    x() + 1.0f, y() + 1.0f,
                    MenuWindow.SIDEBAR_WIDTH + 11.0f, MenuWindow.MENU_HEIGHT - 2.0f,
                    11.0f, color(colors, LEFT_BACKGROUND)
            );
            ctx.fillRect(x() + MenuWindow.SIDEBAR_WIDTH, y() + 1.0f, 11.0f,
                    MenuWindow.MENU_HEIGHT - 2.0f, color(colors, RIGHT_BACKGROUND));
        }
        ctx.fillRect(x() + MenuWindow.SIDEBAR_WIDTH, y() + 1.0f, 1.0f,
                MenuWindow.MENU_HEIGHT - 2.0f, color(colors, DIVIDER));
        ctx.fillRect(x() + 1.0f, y() + MenuWindow.EXPANDED_HEADER_HEIGHT - 1.0f,
                MenuWindow.MENU_WIDTH - 2.0f, 1.0f, color(colors, TOPBAR_DIVIDER));

        renderBrand(ctx, colors);
        renderNavigation(ctx, colors);
        renderProfile(ctx, colors);
        renderTopbar(ctx, colors);
        if (this.selectedView == View.SETTINGS) {
            float progress = this.settingsContentAnimation.smoothAnimation();
            colors.push();
            colors.alpha(progress);
            renderSettingsPage(ctx, colors);
            colors.pop();
        }
        // settingsColumn is rendered explicitly above so it shares the page fade.
    }

    /**
     * Rendered by MenuWindow after the module content so the popup cannot be
     * covered by cards from the right-hand scrolling layer.
     */
    public void renderProfileSettingsOverlay(DrawCtx ctx) {
        renderProfileSettings(ctx, ctx.colorStack());
    }

    private void renderBrand(DrawCtx ctx, ColorStack colors) {
        float logoWidth = 19.0f;
        float logoHeight = 16.0f;
        float logoX = x() + (MenuWindow.SIDEBAR_WIDTH - logoWidth) / 2.0f;
        float logoY = y() + (MenuWindow.EXPANDED_HEADER_HEIGHT - logoHeight) / 2.0f;
        SpectraLogoRenderer.drawCentered(ctx, logoX, logoY + logoHeight * 0.5f,
                logoWidth, colors.white());
    }

    private void renderNavigation(DrawCtx ctx, ColorStack colors) {
        ctx.text(this.regular, "General", 11, x() + 15.0f, y() + GENERAL_LABEL_Y, color(colors, WEAK));
        renderNavigationRow(ctx, colors, View.DISPLAY, DISPLAY_ROW_Y);
        renderNavigationRow(ctx, colors, View.VISUALS, VISUALS_ROW_Y);
        renderNavigationRow(ctx, colors, View.UTILITIES, UTILITIES_ROW_Y);

        ctx.text(this.regular, "Settings", 11, x() + 15.0f, y() + SETTINGS_LABEL_Y, color(colors, WEAK));
        renderNavigationRow(ctx, colors, View.CONFIGS, CONFIGS_ROW_Y);
        renderNavigationRow(ctx, colors, View.THEMES, THEMES_ROW_Y);
        renderNavigationRow(ctx, colors, View.SETTINGS, 405.0f);
    }

    private void renderNavigationRow(DrawCtx ctx, ColorStack colors, View view, float offsetY) {
        int index = view.ordinal();
        ClickableBehavior behavior = this.navigation[index];
        float rowX = x() + NAV_X;
        float rowY = y() + offsetY;
        if (view == this.selectedView) {
            ctx.fillRoundedRect(rowX, rowY, NAV_WIDTH, NAV_HEIGHT, 5.0f, color(colors, ACTIVE_ROW));
        } else if (behavior.hoverAnimation().smoothAnimation() > 0.01f) {
            ctx.fillRoundedRect(rowX, rowY, NAV_WIDTH, NAV_HEIGHT, 5.0f,
                    colors.computeColor(0xFFFFFF, (7.0f / 255.0f) * behavior.hoverAnimation().smoothAnimation()));
        }
        int color = color(colors, view == this.selectedView ? TEXT : MUTED);
        String icon = this.navigationIcons[index];
        ctx.text(this.menuIcon, icon, 12, rowX + 7.0f, rowY + 6.0f, color);
        ctx.text(this.regular, view.label, 12, rowX + 27.0f, rowY + 5.0f, color);
    }

    private void renderHudPreviewToggle(DrawCtx ctx, ColorStack colors) {
        if (Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        boolean enabled = HudEditorScreen.isMenuPreviewEnabled();
        this.hudPreviewAnimation.state(enabled);
        float panelX = hudPreviewX();
        float panelY = hudPreviewY();
        float panelWidth = 174.0f;
        float panelHeight = 32.0f;
        float hover = this.hudPreviewToggle.hoverAnimation().smoothAnimation();
        int panel = colors.computeColor(0x0F0F12, Math.round(236.0f + hover * 12.0f));
        // The switch itself communicates the enabled state. Keeping the outer
        // panel border neutral prevents the whole control from flashing bright.
        int border = colors.computeColor(0x757584, 42);
        ctx.fillOutlinedRoundedRect(
                panelX, panelY, panelWidth, panelHeight, 10.0f, 1.0f, border, panel
        );
        ctx.text(this.regular, "\u041E\u0442\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 HUD", 11,
                panelX + 11.0f, panelY + 8.0f, color(colors, TEXT));

        float switchWidth = 30.0f;
        float switchHeight = 16.0f;
        float switchX = panelX + panelWidth - switchWidth - 8.0f;
        float switchY = panelY + (panelHeight - switchHeight) / 2.0f;
        int switchBackground = colors.interpolate(
                colors.computeColor(0xFFFFFF, 11),
                colors.computeColor(0x8B87FF, 194),
                this.hudPreviewAnimation
        );
        ctx.fillRoundedRect(
                switchX, switchY, switchWidth, switchHeight,
                switchHeight / 2.0f, switchBackground
        );
        float knobRadius = 6.0f;
        float knobX = switchX + 2.0f + knobRadius
                + (switchWidth - 4.0f - knobRadius * 2.0f)
                * this.hudPreviewAnimation.smoothAnimation();
        int knob = colors.interpolate(
                colors.computeColor(0x7D7D89),
                colors.white(),
                this.hudPreviewAnimation
        );
        ctx.circle(knobX, switchY + switchHeight / 2.0f, knobRadius, knob);
    }

    private void toggleHudPreview() {
        HudEditorScreen.toggleMenuPreview();
    }

    private float hudPreviewX() {
        return x() + MenuWindow.MENU_WIDTH - 174.0f;
    }

    private float hudPreviewY() {
        return y() - 40.0f;
    }

    private void renderProfile(DrawCtx ctx, ColorStack colors) {
        float profileX = x() + 15.0f;
        float profileY = y() + 452.0f;
        ctx.fillRect(x() + 1.0f, y() + 441.0f, MenuWindow.SIDEBAR_WIDTH - 1.0f, 1.0f,
                color(colors, PROFILE_DIVIDER));
        float hover = this.profileSettingsButton.hoverAnimation().smoothAnimation();
        if (hover > 0.01f) {
            ctx.fillRoundedRect(x() + 8.0f, y() + 447.0f,
                    NAV_WIDTH, 38.0f, 7.0f,
                    colors.computeColor(0xFFFFFFFF, 0.055f * hover));
        }
        var session = Spectra.INSTANCE.userSession();
        if (session.avatarUrl() == null || session.avatarUrl().isBlank()) {
            String userGlyph = "4";
            int glyphSize = 28;
            ctx.text(this.menuIcon, userGlyph, glyphSize,
                    profileX + 14.0f - this.menuIcon.getWidth(userGlyph, glyphSize) / 2.0f,
                    profileY + (28.0f - this.menuIcon.getHeight(glyphSize)) / 2.0f,
                    colors.computeColor(0x55555F));
        } else {
            GlTexture avatar = session.texture();
            avatar.magFilter(9729);
            avatar.minFilter(9729);
            ctx.roundedTexture(avatar, profileX, profileY, 28.0f, 28.0f, 14.0f, colors.white());
        }
        float nameHeight = this.regular.getHeight(12.0f);
        float expireHeight = this.regular.getHeight(10.0f);
        float textGap = 2.0f;
        float textY = profileY + (28.0f - nameHeight - textGap - expireHeight) / 2.0f;
        ctx.text(this.regular, session.username(), 12, profileX + 35.0f, textY, color(colors, TEXT));
        ctx.text(this.regular, ru.spectra.client.util.SubscriptionFormatter.display(session.expire()), 10,
                profileX + 35.0f, textY + nameHeight + textGap,
                color(colors, WEAK));
    }

    private void openProfilePopup() {
        Spectra.INSTANCE.menuWindow().popupWindow().openAnchored(
                new ProfilePopupContent(),
                x() + MenuWindow.SIDEBAR_WIDTH - 14.0f,
                y() + 466.0f
        );
    }

    private void renderSettingsPage(DrawCtx ctx, ColorStack colors) {
        float pageX = settingsPageX();
        ctx.text(this.regular, "Interface", 11, pageX, y() + 61.0f,
                color(colors, WEAK));
        this.settingsColumn.render(ctx);
        this.settingsColumn.renderOverlays(ctx);
    }

    private void renderSettingsScalePageRow(
            DrawCtx ctx, ColorStack colors, float rowY,
            String title, String description, float value) {
        renderSettingsRowText(ctx, colors, rowY, title, description);
        float sliderX = settingsPageSliderX();
        String percent = Math.round(value * 100.0f) + "%";
        ctx.text(this.regular, percent, 10,
                sliderX + SETTINGS_TRACK_WIDTH - this.regular.getWidth(percent, 10.0f),
                rowY + 17.0f, color(colors, WEAK));
        float trackY = rowY + 43.0f;
        float progress = Math.max(0.0f, Math.min(1.0f,
                (value - 0.75f) / 1.25f));
        ctx.fillRoundedRect(sliderX, trackY, SETTINGS_TRACK_WIDTH, 3.0f,
                1.5f, colors.computeColor(0xFFFFFF, 18));
        ctx.fillRoundedRect(sliderX, trackY, SETTINGS_TRACK_WIDTH * progress, 3.0f,
                1.5f, colors.computeColor(
                        Spectra.INSTANCE.theme().palette().accentBright().argb()));
        ctx.circle(sliderX + SETTINGS_TRACK_WIDTH * progress,
                trackY + 1.5f, 3.5f, colors.white());
        renderSettingsPageSeparator(ctx, colors, rowY);
    }

    private void renderSettingsRowText(
            DrawCtx ctx, ColorStack colors, float rowY,
            String title, String description) {
        ctx.text(this.regular, title, 13, settingsPageX(), rowY + 12.0f,
                color(colors, TEXT));
        ctx.text(this.regular, description, 11, settingsPageX(), rowY + 34.0f,
                color(colors, MUTED));
    }

    private void renderSettingsPageSeparator(
            DrawCtx ctx, ColorStack colors, float rowY) {
        ctx.fillRect(settingsPageX(), rowY + SETTINGS_ROW_HEIGHT - 1.0f,
                settingsPageWidth(), 1.0f, colors.computeColor(0x696976, 38));
    }

    private void renderProfileSettings(DrawCtx ctx, ColorStack colors) {
        float animation = this.profileSettingsAnimation.smoothAnimation();
        if (animation <= 0.01f) {
            return;
        }
        if (this.capturingMenuKey) {
            ctx.window().interceptKeyboard(true);
        }
        colors.push();
        colors.alpha(animation);
        float panelX = settingsPanelX();
        float panelY = settingsPanelY();
        float panelWidth = SETTINGS_PANEL_WIDTH;
        float panelHeight = SETTINGS_PANEL_HEIGHT;
        ThemePalette palette = ctx.theme().palette();
        ctx.fillRoundedRect(panelX - 10.0f, panelY - 9.0f,
                panelWidth + 20.0f, panelHeight + 22.0f,
                16.0f, colors.computeColor(0x000000, 20));
        ctx.fillRoundedRect(panelX - 6.0f, panelY - 5.0f,
                panelWidth + 12.0f, panelHeight + 14.0f,
                13.0f, colors.computeColor(0x000000, 34));
        ctx.fillRoundedRect(panelX - 3.0f, panelY - 2.0f,
                panelWidth + 6.0f, panelHeight + 8.0f,
                11.0f, colors.computeColor(0x000000, 48));
        var bloomFramebuffer = Spectra.INSTANCE.windowController().bloom().getBloomFramebuffer();
        if (bloomFramebuffer != null) {
            ctx.bloom(panelX, panelY, panelWidth, panelHeight, 24.0f,
                    bloomFramebuffer.getColorAttachment());
        }
        ctx.fillOutlinedRoundedRect(panelX, panelY, panelWidth, panelHeight,
                8.0f, 2.5f,
                colors.computeColor(ThemePalette.white.argb(), 0.03f),
                colors.computeColor(palette.frameBackground().argb()));

        float badgeX = panelX + 11.0f;
        float badgeY = panelY + 13.0f;
        String badgeText = "Settings";
        int badgeIconSize = 9;
        float badgeWidth = 6.0f + badgeIconSize + 5.0f
                + this.regular.getWidth(badgeText, 9.0f) + 7.0f;
        ctx.fillRoundedRect(badgeX, badgeY, badgeWidth, 18.0f, 5.0f,
                colors.computeColor(palette.accent().argb(), 42));
        int badgeForeground = colors.computeColor(palette.accentBright().argb());
        ctx.textureVerticalC(this.settingsIcon, badgeX + 6.0f, badgeY + 9.0f,
                badgeIconSize, badgeIconSize, badgeForeground);
        ctx.text(this.regular, badgeText, 9,
                badgeX + 6.0f + badgeIconSize + 5.0f,
                badgeY + 4.5f, badgeForeground);

        float closeHover = this.profileSettingsCloseButton.hoverAnimation().smoothAnimation();
        int closeColor = colors.interpolate(
                colors.computeColor(palette.text().tone(500).argb()),
                colors.computeColor(palette.text().tone(100).argb()),
                closeHover
        );
        ctx.texture(
                this.profileSettingsCloseIcon,
                this.profileSettingsCloseButton.ownerX()
                        + (this.profileSettingsCloseButton.ownerW() - 9.0f) / 2.0f,
                this.profileSettingsCloseButton.ownerY()
                        + (this.profileSettingsCloseButton.ownerH() - 9.0f) / 2.0f,
                9.0f, 9.0f, closeColor
        );

        ctx.text(this.regular, "\u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 \u0438\u043D\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430", 13,
                panelX + 11.0f, panelY + 43.0f, color(colors, TEXT));

        renderScaleRow(ctx, colors, "\u0420\u0430\u0437\u043C\u0435\u0440 \u043C\u0435\u043D\u044E",
                previewMenuScale(),
                0.75f, 2.0f, panelX + 11.0f, panelY + 70.0f);
        renderScaleRow(ctx, colors, "\u0420\u0430\u0437\u043C\u0435\u0440 HUD",
                WidgetStack.hudScale(),
                0.75f, 2.0f, panelX + 11.0f, panelY + 121.0f);

        float keyY = panelY + 178.0f;
        ctx.text(this.regular, "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043C\u0435\u043D\u044E", 11, panelX + 11.0f, keyY,
                color(colors, MUTED));
        String key = this.capturingMenuKey
                ? "\u041D\u0430\u0436\u043C\u0438\u0442\u0435..."
                : KeyboardUtil.keyToString(Spectra.INSTANCE.menuWindow().openKey());
        float boxWidth = 77.0f;
        float boxX = panelX + panelWidth - boxWidth - 10.0f;
        int keyBorder = this.capturingMenuKey
                ? colors.computeColor(Spectra.INSTANCE.theme().palette().accentBright().argb())
                : colors.computeColor(0x757584, 48);
        ctx.fillOutlinedRoundedRect(boxX, keyY - 5.0f, boxWidth, 25.0f,
                7.0f, 1.0f, keyBorder, colors.computeColor(0xFFFFFF, 5));
        float keyWidth = this.regular.getWidth(key, 10.0f);
        ctx.text(this.regular, key, 10, boxX + (boxWidth - keyWidth) / 2.0f,
                keyY + 2.0f, color(colors, TEXT));
        colors.pop();
    }

    private void renderScaleRow(DrawCtx ctx, ColorStack colors, String label,
                                float value, float min, float max, float x, float y) {
        ctx.text(this.regular, label, 11, x, y, color(colors, MUTED));
        String percent = Math.round(value * 100.0f) + "%";
        ctx.text(this.regular, percent, 10,
                x + SETTINGS_TRACK_WIDTH - this.regular.getWidth(percent, 10.0f),
                y + 1.0f, color(colors, WEAK));
        float trackY = y + 20.0f;
        float progress = (value - min) / (max - min);
        ctx.fillRoundedRect(x, trackY, SETTINGS_TRACK_WIDTH, 3.0f, 1.5f,
                colors.computeColor(0xFFFFFF, 18));
        ctx.fillRoundedRect(x, trackY, SETTINGS_TRACK_WIDTH * progress, 3.0f, 1.5f,
                colors.computeColor(Spectra.INSTANCE.theme().palette().accentBright().argb()));
        ctx.circle(x + SETTINGS_TRACK_WIDTH * progress, trackY + 1.5f, 3.5f, colors.white());
    }

    private void toggleProfileSettings() {
        this.profileSettingsOpen = !this.profileSettingsOpen;
        this.profileSettingsAnimation.state(this.profileSettingsOpen);
        if (!this.profileSettingsOpen) {
            this.capturingMenuKey = false;
            boolean hadPendingScale = this.menuScaleDirty;
            applyPendingMenuScale();
            if (hadPendingScale) {
                saveMenuPreferences();
            }
            this.draggingMenuScale = false;
            this.draggingHudScale = false;
        }
    }

    private void renderTopbar(DrawCtx ctx, ColorStack colors) {
        float rightX = x() + MenuWindow.SIDEBAR_WIDTH;
        float titleX = rightX + 19.0f;
        float textY = y() + 15.0f;
        float searchX = x() + SEARCH_X;
        String title = this.selectedView.label;
        String icon = this.navigationIcons[this.selectedView.ordinal()];
        ctx.text(this.menuIcon, icon, 12, titleX, textY + 3.0f, color(colors, TEXT));
        ctx.text(this.regular, title, 13, titleX + 20.0f, textY, color(colors, TEXT));

        if (!this.selectedView.moduleView) {
            return;
        }
        float searchY = y() + SEARCH_Y;
        ctx.fillOutlinedRoundedRect(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 5.0f, 1.0f,
                color(colors, SEARCH_BORDER), color(colors, SEARCH_BACKGROUND));
        renderSearchText(ctx, colors, searchX, searchY);
    }

    private void renderSearchText(DrawCtx ctx, ColorStack colors, float searchX, float searchY) {
        String text = this.searchField.text();
        float originX = searchX + 9.0f - this.searchField.viewportOffset();
        float textY = searchY + 5.0f;
        ctx.drawEngine().beginStencil();
        ctx.fillRect(searchX + 7.0f, searchY, SEARCH_WIDTH - 14.0f, SEARCH_HEIGHT, colors.white());
        ctx.drawEngine().prepareStencil(1);
        if (!text.isEmpty() && this.searchField.hasSelection()) {
            float selectionX = originX + ctx.textWidthPhysical(
                    this.regular, text.substring(0, this.searchField.selMin()), 12
            );
            float selectionRight = originX + ctx.textWidthPhysical(
                    this.regular, text.substring(0, this.searchField.selMax()), 12
            );
            ctx.fillRoundedRect(
                    selectionX, textY - 1.0f,
                    selectionRight - selectionX + 1.0f,
                    this.regular.getHeight(12.0f) + 2.0f,
                    3.0f,
                    colors.computeColor(ctx.theme().palette().accent().argb(), 128)
            );
        }
        if (text.isEmpty()) {
            ctx.text(this.regular, ClientLocalization.text("Search", "\u041F\u043E\u0438\u0441\u043A"), 12,
                    originX, textY, color(colors, 0x738E8E9C));
        } else {
            ctx.text(this.regular, text, 12, originX, textY, color(colors, TEXT));
        }
        if (this.searchField.isCursorVisible() && this.searchField.focused()
                && !this.searchField.hasSelection()) {
            float cursor = originX + ctx.textWidthPhysical(this.regular,
                    text.substring(0, this.searchField.cursorIndex()), 12);
            ctx.fillRect(cursor, textY, 1.0f, this.regular.getHeight(12.0f), color(colors, TEXT));
        }
        ctx.drawEngine().endStencil();
        if (this.searchField.focused()) {
            ctx.window().interceptKeyboard(true);
        }
    }

    @Override
    public void layout(LayoutScaleContext context) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.MENU_HEIGHT);
        this.settingsIcon.setDimensions(12, 12);
        this.navigation[View.DISPLAY.ordinal()].setDimensions(x() + NAV_X, y() + DISPLAY_ROW_Y, NAV_WIDTH, NAV_HEIGHT);
        this.navigation[View.VISUALS.ordinal()].setDimensions(x() + NAV_X, y() + VISUALS_ROW_Y, NAV_WIDTH, NAV_HEIGHT);
        this.navigation[View.UTILITIES.ordinal()].setDimensions(x() + NAV_X, y() + UTILITIES_ROW_Y, NAV_WIDTH, NAV_HEIGHT);
        this.navigation[View.CONFIGS.ordinal()].setDimensions(x() + NAV_X, y() + CONFIGS_ROW_Y, NAV_WIDTH, NAV_HEIGHT);
        this.navigation[View.THEMES.ordinal()].setDimensions(x() + NAV_X, y() + THEMES_ROW_Y, NAV_WIDTH, NAV_HEIGHT);
        this.navigation[View.SETTINGS.ordinal()].setDimensions(x() + NAV_X, y() + 405.0f, NAV_WIDTH, NAV_HEIGHT);
        this.searchField.listen(x() + SEARCH_X, y() + SEARCH_Y, SEARCH_WIDTH, SEARCH_HEIGHT,
                SEARCH_WIDTH - 18.0f, x() + SEARCH_X + 9.0f, context.scaleFactor());
        this.profileSettingsCloseIcon.setDimensions(9, 9);
        this.profileSettingsButton.setDimensions(
                x() + 8.0f, y() + 447.0f, NAV_WIDTH, 38.0f);
        this.settingsColumn.setPosition(settingsPageX(), y() + SETTINGS_ROW_START);
        this.settingsColumn.width = settingsPageWidth();
        super.layout(context);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        handled |= this.profileSettingsButton.handleInput(context, handled);
        if (this.selectedView.moduleView && this.searchField.handleInput(context, handled)) {
            handled = true;
        }
        if (this.selectedView == View.SETTINGS) {
            handled |= this.settingsColumn.handleInput(context, handled);
        }
        for (ClickableBehavior behavior : this.navigation) {
            handled |= behavior.handleInput(context, handled);
        }
        return handled || (this.selectedView == View.SETTINGS
                && !(context.inputEvent() instanceof KeyInput)
                && context.inArea(x() + MenuWindow.SIDEBAR_WIDTH,
                y() + MenuWindow.EXPANDED_HEADER_HEIGHT,
                MenuWindow.CONTENT_WIDTH,
                MenuWindow.MENU_HEIGHT - MenuWindow.EXPANDED_HEADER_HEIGHT));
    }

    private boolean handleProfileSettingsInput(InputEventContext context, boolean consumed) {
        InputEvent event = context.inputEvent();
        if (this.selectedView != View.SETTINGS) {
            return false;
        }
        if (this.capturingMenuKey && event instanceof KeyInput key
                && key.keyAction().press()) {
            if (key.keyCode() != 256) {
                Spectra.INSTANCE.menuWindow().setOpenKey(key.keyCode());
                Spectra.INSTANCE.configManager().menuStateConfig().setMenuOpenKey(key.keyCode());
                saveMenuPreferences();
            }
            this.capturingMenuKey = false;
            return true;
        }
        if (consumed) {
            return false;
        }
        float firstRowY = y() + SETTINGS_ROW_START;
        float sliderX = settingsPageSliderX();
        if (event instanceof MouseButtonInput mouse && mouse.button() == 0) {
            if (mouse.action().press()) {
                if (context.inArea(sliderX, firstRowY + 34.0f,
                        SETTINGS_TRACK_WIDTH, 22.0f)) {
                    this.draggingMenuScale = true;
                    updateMenuScale(context.logicalMousePosition().x());
                    return true;
                }
                if (context.inArea(sliderX,
                        firstRowY + SETTINGS_ROW_HEIGHT + 34.0f,
                        SETTINGS_TRACK_WIDTH, 22.0f)) {
                    this.draggingHudScale = true;
                    updateHudScale(context.logicalMousePosition().x());
                    return true;
                }
                float keyRowY = firstRowY + SETTINGS_ROW_HEIGHT * 2.0f;
                if (context.inArea(settingsPageX() + settingsPageWidth() - 104.0f,
                        keyRowY + 25.0f, 104.0f, 28.0f)) {
                    this.capturingMenuKey = true;
                    return true;
                }
            }
            if (mouse.action().release()) {
                if (this.draggingMenuScale || this.draggingHudScale) {
                    applyPendingMenuScale();
                    this.draggingMenuScale = false;
                    this.draggingHudScale = false;
                    saveMenuPreferences();
                    return true;
                }
            }
        }
        if (event instanceof CursorMoveInput) {
            if (this.draggingMenuScale) {
                updateMenuScale(context.logicalMousePosition().x());
                return true;
            }
            if (this.draggingHudScale) {
                updateHudScale(context.logicalMousePosition().x());
                return true;
            }
        }
        return !(event instanceof KeyInput) && context.inArea(
                x() + MenuWindow.SIDEBAR_WIDTH,
                y() + MenuWindow.EXPANDED_HEADER_HEIGHT,
                MenuWindow.CONTENT_WIDTH,
                MenuWindow.MENU_HEIGHT - MenuWindow.EXPANDED_HEADER_HEIGHT
        );
    }

    private void updateMenuScale(float mouseX) {
        this.pendingMenuScale = sliderValue(mouseX);
        this.lastMenuScaleMoveAt = System.currentTimeMillis();
        this.menuScaleDirty = true;
    }

    private float previewMenuScale() {
        return this.draggingMenuScale && Float.isFinite(this.pendingMenuScale)
                ? this.pendingMenuScale
                : Spectra.INSTANCE.windowController().dpiScaleFactor();
    }

    private void applyPendingMenuScale() {
        if (!this.menuScaleDirty || !Float.isFinite(this.pendingMenuScale)) {
            return;
        }
        float value = this.pendingMenuScale;
        this.menuScaleDirty = false;
        Spectra.INSTANCE.windowController().setManualDpiScaleFactor(value);
        Spectra.INSTANCE.configManager().menuStateConfig().setDpiScale(false, value);
    }

    private void updateHudScale(float mouseX) {
        float value = sliderValue(mouseX);
        WidgetStack.setHudScale(value);
        Spectra.INSTANCE.configManager().menuStateConfig().setHudScale(value);
    }

    private float sliderValue(float mouseX) {
        float progress = Math.max(0.0f, Math.min(1.0f,
                (mouseX - settingsPageSliderX()) / SETTINGS_TRACK_WIDTH));
        return Math.round((0.75f + progress * 1.25f) / 0.05f) * 0.05f;
    }

    private void saveMenuPreferences() {
        try {
            Spectra.INSTANCE.configManager().saveMenuState();
        } catch (IOException exception) {
            Spectra.LOGGER.error("Failed to save interface preferences", exception);
        }
    }

    private float settingsPanelX() {
        return x() + 9.0f;
    }

    private float settingsPanelY() {
        return y() + 250.0f;
    }

    private float settingsPageX() {
        return x() + MenuWindow.SIDEBAR_WIDTH + SETTINGS_PAGE_INSET;
    }

    private float settingsPageWidth() {
        return MenuWindow.CONTENT_WIDTH - SETTINGS_PAGE_INSET * 2.0f;
    }

    private float settingsPageSliderX() {
        return settingsPageX() + settingsPageWidth() - SETTINGS_TRACK_WIDTH;
    }

    public boolean isSettingsView() {
        return this.selectedView == View.SETTINGS;
    }

    @Override
    public void animation(WeightedEngine engine) {
        if (this.draggingMenuScale && this.menuScaleDirty
                && System.currentTimeMillis() - this.lastMenuScaleMoveAt >= 1000L) {
            applyPendingMenuScale();
            saveMenuPreferences();
        }
        this.searchField.animate(engine);
        this.settingsContentAnimation.animate(engine);
        this.profileSettingsAnimation.state(this.profileSettingsOpen).animate(engine);
        this.profileSettingsButton.animate(engine);
        this.profileSettingsCloseButton.animate(engine);
        for (ClickableBehavior behavior : this.navigation) {
            behavior.animate(engine);
        }
        super.animation(engine);
    }

    public boolean expandedState() {
        return false;
    }

    public void exchange() {
        // The Spectra reference uses a permanent sidebar, so there is no expanded header state.
    }

    private enum View {
        DISPLAY("Display", true),
        VISUALS("Visuals", true),
        UTILITIES("Utilities", true),
        CONFIGS("Configs", false),
        THEMES("Themes", false),
        SETTINGS("Settings", false);

        private final String label;
        private final boolean moduleView;

        View(String label, boolean moduleView) {
            this.label = label;
            this.moduleView = moduleView;
        }
    }
}
