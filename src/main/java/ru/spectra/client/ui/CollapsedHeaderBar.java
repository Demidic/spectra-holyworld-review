package ru.spectra.client.ui;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.SpectraLogoRenderer;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.WeightedEngine;

public class CollapsedHeaderBar extends WidgetContainer {
    public final Widget unfoldButton;
    public final GlTexture chatIcon = new GlTexture(new ClasspathResource("/icons/menu/new/chat.png"));
    public final GlTexture searchIcon = new GlTexture(new ClasspathResource("/icons/menu/new/search.png"));
    public final GlTexture languageIcon = new GlTexture(new ClasspathResource("/icons/menu/new/language.png"));
    public final GlTexture unfoldIcon = new GlTexture(new ClasspathResource("/icons/menu/new/unfold.png"));
    public final Widget languageButton = new IconButtonWidget(this.languageIcon, () -> {
        Spectra.INSTANCE.menuWindow().contentArea().requestNextLanguage();
    }, 13.0f, 13.0f);
    public final Widget chatButton = new IconButtonWidget(this.chatIcon, () -> {
        Spectra.INSTANCE.menuWindow().chat().invertOpenState();
    }, 13.0f, 13.0f);
    public final Widget searchButton = new IconButtonWidget(this.searchIcon, () -> {
        Spectra.INSTANCE.menuWindow().searchContainer().invert();
    }, 13.0f, 13.0f);

    public CollapsedHeaderBar(Runnable runnable) {
        this.unfoldButton = new IconButtonWidget(this.unfoldIcon, runnable, 13.0f, 13.0f);
        addChild(new CollapsedTabBar());
        addChild(this.unfoldButton);
        addChild(this.languageButton);
        addChild(this.chatButton);
        addChild(this.searchButton);
    }

    @Override
    public void render(DrawCtx class699Var) {
        ColorStack class115VarColorStack = class699Var.colorStack();
        class699Var.roundedTexture(Spectra.INSTANCE.userSession().texture(), ((x() + width()) - 16.0f) - 22.0f, (y() + (height() / 2.0f)) - 11.0f, 22.0f, 22.0f, 6.0f, class115VarColorStack.white());
        SpectraLogoRenderer.drawCentered(class699Var, x() + 18.0f,
                y() + height() * 0.5f, 22.0f, class115VarColorStack.white());
        super.render(class699Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        return super.handleInput(class688Var, z);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.COLLAPSED_HEADER_HEIGHT);
        float fX = ((((x() + width()) - 16.0f) - 22.0f) - 16.0f) - this.chatButton.width();
        this.chatButton.setPosition(fX, (y() + (height() / 2.0f)) - (this.chatButton.height() / 2.0f));
        float fWidth = fX - (this.chatButton.width() + 14.0f);
        this.languageButton.setPosition(fWidth, (y() + (height() / 2.0f)) - (this.languageButton.height() / 2.0f));
        float fWidth2 = fWidth - (this.languageButton.width() + 14.0f);
        this.searchButton.setPosition(fWidth2, (y() + (height() / 2.0f)) - (this.searchButton.height() / 2.0f));
        this.unfoldButton.setPosition(fWidth2 - (this.searchButton.width() + 14.0f), (y() + (height() / 2.0f)) - (this.unfoldButton.height() / 2.0f));
        super.layout(class698Var);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        super.animation(class141Var);
    }
}
