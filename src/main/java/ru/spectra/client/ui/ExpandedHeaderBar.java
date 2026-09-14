package ru.spectra.client.ui;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.type.DropdownOption;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.SpectraLogoRenderer;
import ru.spectra.client.type.GuiScale;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Language;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.model.ScrollInput;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.WindowController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ExpandedHeaderBar extends WidgetContainer {
    public final BindableToggleWidget hideButton;

    public final ToggleTextButton chatButton;

    public final DropdownWidget<Language> languageDropdown;

    public final DropdownWidget<GuiScale> scaleDropdown;

    public final HeaderBindingEditor bindingEditor;
    public final Runnable toggleCallback;

    public final ExpandedTabPanel tabPanel = new ExpandedTabPanel();

    public final CategoryChipBar categoryChipBar = new CategoryChipBar();

    public final UserProfileBadge profileBadge = new UserProfileBadge();
    public final GlTexture tabsIcon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/misc.png"));
    public final GlTexture categoriesIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final MsdfFont labelFont = Fonts.INTER_BOLD.get();
    public final BindableToggleWidget searchButton = new BindableToggleWidget(() -> {
        Spectra.INSTANCE.menuWindow().searchContainer().invert();
    }, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/search.png")), 6.0f, 8.0f, 5.5f, 13).setBindingEditable(true).setEditableBinding(List.of(341, 70));

    public ExpandedHeaderBar(Runnable runnable) {
        this.toggleCallback = runnable;
        this.hideButton = new BindableToggleWidget(runnable, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/unfold.png")), 6.0f, 8.0f, 5.5f, 13).setBindingEditable(true).setImmutableBinding(List.of(256)).setEditableBinding(List.of(258));
        BindableToggleWidget class737Var = this.hideButton;
        Objects.requireNonNull(class737Var);
        Supplier supplier = class737Var::editableBinding;
        BindableToggleWidget class737Var2 = this.hideButton;
        Objects.requireNonNull(class737Var2);
        this.bindingEditor = new HeaderBindingEditor(supplier, class737Var2::setEditableBinding).maxBindingKeys(1);
        this.chatButton = new ToggleTextButton(() -> {
            Spectra.INSTANCE.menuWindow().chat().invertOpenState();
        }, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/chat.png")), 6.0f, 8.0f, 5.5f, 13);
        this.languageDropdown = new DropdownWidget<>(Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/language.png")), 6.0f, 8.0f, 5.5f, 13);
        this.languageDropdown.setOptions(Arrays.stream(Language.values()).map(class313Var -> {
            return new DropdownOption<Language>(class313Var, Translation.clearText(class313Var.canonical()));
        }).toList(), Spectra.INSTANCE.languages().current());
        this.languageDropdown.onSelect(class313Var2 -> {
            Spectra.INSTANCE.menuWindow().contentArea().requestLanguage(class313Var2);
        });
        this.scaleDropdown = new DropdownWidget<>(Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/display.png")), 6.0f, 8.0f, 5.5f, 13);
        this.scaleDropdown.setOptions(GuiScale.options(), currentGuiScale());
        this.scaleDropdown.onSelect(class785Var -> {
            if (class785Var.auto()) {
                Spectra.INSTANCE.windowController().enableAutoDpiScale();
                try {
                    Spectra.INSTANCE.configManager().menuStateConfig().setDpiScale(true, Spectra.INSTANCE.windowController().dpiScaleFactor());
                    Spectra.INSTANCE.configManager().saveMenuState();
                    return;
                } catch (IOException e) {
                    Spectra.LOGGER.error("Failed to save menu scale state", e);
                    return;
                }
            }
            Spectra.INSTANCE.windowController().setManualDpiScaleFactor(class785Var.scaleFactor());
            try {
                Spectra.INSTANCE.configManager().menuStateConfig().setDpiScale(false, class785Var.scaleFactor());
                Spectra.INSTANCE.configManager().saveMenuState();
            } catch (IOException e2) {
                Spectra.LOGGER.error("Failed to save menu scale state", e2);
            }
        });
        addChild(this.tabPanel);
        addChild(this.categoryChipBar);
        addChild(this.bindingEditor);
        addChild(this.profileBadge);
        addChild(this.scaleDropdown);
        addChild(this.hideButton);
        addChild(this.languageDropdown);
        addChild(this.chatButton);
        addChild(this.searchButton);
    }

    @Override
    public void render(DrawCtx class699Var) {
        this.languageDropdown.label(Translation.clearText(Spectra.INSTANCE.languages().current().canonical()), 12);
        this.chatButton.label(Lang.CHAT, 12);
        this.searchButton.label(Lang.SEARCH.effective(), 12);
        this.hideButton.label(Lang.HIDE.effective(), 12);
        this.hideButton.setActive(true);
        this.searchButton.setActive(Spectra.INSTANCE.menuWindow().searchContainer().opened);
        SpectraLogoRenderer.drawCentered(class699Var, x() + 18.0f,
                y() + MenuWindow.COLLAPSED_HEADER_HEIGHT * 0.5f,
                22.0f, class699Var.colorStack().white());
        super.render(class699Var);
        DrawEngine class154VarDrawEngine = class699Var.drawEngine();
        String strEffective = Lang.ALL_TABS.effective();
        float fY = y() + MenuWindow.COLLAPSED_HEADER_HEIGHT + 9.0f;
        float f = fY + ((13.0f - 10.0f) / 2.0f);
        class699Var.text(this.labelFont, strEffective.toUpperCase(), 10, x() + 15.0f, f, class154VarDrawEngine.colorStack().computeColor(class699Var.theme().palette().text().tone(300).argb()));
        class699Var.texture(this.tabsIcon, x() + 15.0f + class699Var.textWidthPhysical(this.labelFont, strEffective, 10) + 13.0f + 5.0f, fY, 13.0f, 13.0f, class154VarDrawEngine.colorStack().white());
        float fWidth = this.tabPanel.width() > 0.0f ? this.tabPanel.width() : 270.0f;
        if (this.categoryChipBar.hasElements()) {
            String strEffective2 = Lang.VISIBLE_CATEGORIES.effective();
            float fTextWidthPhysical = class699Var.textWidthPhysical(this.labelFont, strEffective2.toUpperCase(), 10);
            class699Var.text(this.labelFont, strEffective2.toUpperCase(), 10, x() + fWidth + 20.0f, f, class154VarDrawEngine.colorStack().computeColor(class699Var.theme().palette().text().tone(300).argb()));
            class699Var.texture(this.categoriesIcon, x() + fWidth + 20.0f + fTextWidthPhysical + 5.0f, fY, 13.0f, 13.0f, class154VarDrawEngine.colorStack().white());
        }
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        InputEvent class691VarInputEvent = class688Var.inputEvent();
        if (super.handleInput(class688Var, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        if (class691VarInputEvent instanceof CursorMoveInput) {
            return class688Var.inArea(x(), y(), width(), MenuWindow.MENU_HEIGHT);
        }
        if (class691VarInputEvent instanceof MouseButtonInput) {
            MouseButtonInput class693Var = (MouseButtonInput) class691VarInputEvent;
            if (class693Var.button() == 0 && class693Var.action().press()) {
                if (class688Var.inArea(x(), y(), width(), MenuWindow.EXPANDED_HEADER_HEIGHT)) {
                    Spectra.INSTANCE.menuWindow().draggableBehavior().handleInput(class688Var, false);
                    return true;
                }
                if (this.toggleCallback == null) {
                    return false;
                }
                this.toggleCallback.run();
                return true;
            }
        }
        if (!(class691VarInputEvent instanceof ScrollInput)) {
            return false;
        }
        return class688Var.inArea(x(), y(), width(), MenuWindow.MENU_HEIGHT);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.languageDropdown.select(Spectra.INSTANCE.languages().current());
        super.animation(class141Var);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.EXPANDED_HEADER_HEIGHT);
        float fX = ((x() + width()) - 16.0f) - this.profileBadge.width();
        this.profileBadge.setPosition(fX, y() + 19.0f);
        float fWidth = fX - (this.chatButton.width() + 16.0f);
        this.chatButton.setPosition(fWidth, y() + 19.0f);
        float fWidth2 = fWidth - (this.scaleDropdown.width() + 6.0f);
        this.scaleDropdown.setPosition(fWidth2, y() + 19.0f);
        float fWidth3 = fWidth2 - (this.languageDropdown.width() + 6.0f);
        this.languageDropdown.setPosition(fWidth3, y() + 19.0f);
        float fWidth4 = fWidth3 - (this.searchButton.width() + 6.0f);
        this.searchButton.setPosition(fWidth4, y() + 19.0f);
        this.hideButton.setPosition(fWidth4 - (this.hideButton.width() + 6.0f), y() + 19.0f);
        this.chatButton.setActive(Spectra.INSTANCE.menuWindow().chat().isOpened());
        this.bindingEditor.setPosition(((x() + width()) - 15.0f) - this.bindingEditor.preferredWidth(class698Var), ((y() + MenuWindow.EXPANDED_HEADER_HEIGHT) - 17.0f) - this.bindingEditor.preferredHeight());
        this.bindingEditor.layout(class698Var);
        float fX2 = x() + (this.tabPanel.width() > 0.0f ? this.tabPanel.width() : 270.0f) + 20.0f;
        float fY = y() + MenuWindow.COLLAPSED_HEADER_HEIGHT + 33.0f;
        float f = (MenuWindow.MENU_WIDTH - fX2) - 16.0f;
        this.categoryChipBar.setPosition(fX2, fY);
        this.categoryChipBar.setSize(f, 60.0f);
        super.layout(class698Var);
    }

    public GuiScale currentGuiScale() {
        WindowController class686VarWindowController = Spectra.INSTANCE.windowController();
        return class686VarWindowController.autoDpiScale() ? GuiScale.AUTO : GuiScale.nearestScale(class686VarWindowController.dpiScaleFactor());
    }

    public void resetHotkeysState() {
        this.hideButton.resetPressedKeys();
        this.searchButton.resetPressedKeys();
    }

    public List<Integer> exchangeEditableBinding() {
        return this.hideButton.editableBinding();
    }
}
