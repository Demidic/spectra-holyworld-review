package ru.spectra.client.ui;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.type.Language;
import ru.spectra.client.event.LanguageChangeEvent;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.OverlayCommandQueue;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;

import java.io.IOException;

public class MenuContentArea extends WidgetContainer {
    public final ToggleAnimator languageAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean pendingLanguageChange = false;
    public Language pendingLanguage = null;

    public void requestNextLanguage() {
        Language class313VarCurrent = Spectra.INSTANCE.languages().current();
        Language[] class313VarArrValues = Language.values();
        requestLanguage(class313VarArrValues[(class313VarCurrent.ordinal() + 1) % class313VarArrValues.length]);
    }

    public void requestLanguage(Language class313Var) {
        if (class313Var == null || class313Var == Spectra.INSTANCE.languages().current()) {
            return;
        }
        this.pendingLanguage = class313Var;
        this.pendingLanguageChange = true;
        this.languageAnimation.state(true);
    }

    @Override
    public void render(DrawCtx class699Var) {
        MenuTabElement current = Spectra.INSTANCE.tabsController().current();
        current.drawFrames(class699Var);
        super.render(class699Var);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.languageAnimation.animate(class141Var);
        MenuTabElement currentTab = Spectra.INSTANCE.tabsController().current();
        if (currentTab != null) {
            currentTab.animation(class141Var);
        }
        if (this.pendingLanguageChange && this.languageAnimation.isOne()) {
            this.pendingLanguageChange = false;
            Spectra.INSTANCE.languages().language(this.pendingLanguage);
            ClientLocalization.invalidateMinecraftTranslations();
            Spectra.INSTANCE.eventDispatcher().dispatch(new LanguageChangeEvent());
            try {
                Spectra.INSTANCE.configManager().menuStateConfig().setLanguage(this.pendingLanguage);
                Spectra.INSTANCE.configManager().saveMenuState();
            } catch (IOException e) {
                Spectra.LOGGER.error("Failed to save menu language state", e);
            }
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
        if (!this.pendingLanguageChange && this.languageAnimation.isOne()) {
            this.languageAnimation.state(false);
        }
        super.animation(class141Var);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        setSize(MenuWindow.CONTENT_WIDTH, MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT);
        Spectra.INSTANCE.tabsController().current().layout(class698Var, x(), y());
        super.layout(class698Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = z | super.handleInput(class688Var, z);
        return zHandleInput | Spectra.INSTANCE.tabsController().current().handleInputFrames(class688Var, zHandleInput);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue class677Var) {
        Spectra.INSTANCE.tabsController().current().collectBlurElements(class677Var);
        super.collectBlurElements(class677Var);
    }
}
