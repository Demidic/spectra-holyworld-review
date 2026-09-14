package ru.spectra.client.ui;

import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.util.WeightedEngine;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ThemeTabLayout extends AbstractTabLayout {
    private static final float HEADER_HEIGHT = 46.0f;
    private static final float FRAMES_Y = 72.0f;
    private static final float SIDE_PADDING = 12.0f;
    private static final float COLUMN_GAP = 20.0f;
    private static final float ROW_GAP = 8.0f;

    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    private final GlTexture brushIcon =
            new GlTexture(new ClasspathResource("/icons/menu/new/brush.png"));
    private final IconLabelBadge titleBadge =
            new IconLabelBadge(this.brushIcon, Translation.clearText("Themes")).uppercase(false);
    private final ru.spectra.client.model.WidgetBounds headerBounds =
            new ru.spectra.client.model.WidgetBounds(0.0f, 0.0f, 0.0f, HEADER_HEIGHT);
    private ScrollbarWidget scrollbar;

    @Override
    public void initialize(MenuTabElement tab) {
        super.initialize(tab);
        ScrollArea area = tab.scrollingAreaComponent();
        Objects.requireNonNull(area);
        Supplier<Float> scroll = area::scrollY;
        Supplier<Float> content = this::getContentHeight;
        Supplier<Float> viewport = () ->
                MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT
                        - this.headerBounds.height() - 16.0f;
        this.scrollbar = new ScrollbarWidget(
                scroll,
                content,
                viewport,
                area::scrollTo,
                10.0f,
                3.0f,
                24.0f
        );
    }

    @Override
    public void render(DrawCtx ctx) {
        ColorStack colors = ctx.colorStack();
        colors.push();
        colors.alphaAnimation(this.tab.currentTabAnimation());
        this.tab.scrollingAreaComponent().beginArea(
                ctx,
                this.tab.framesOriginX(),
                this.tab.framesOriginY() + FRAMES_Y,
                width(),
                height() - FRAMES_Y
        );
        for (int index = this.tab.frames().size() - 1; index >= 0; index--) {
            AbstractFrame frame = this.tab.frames().get(index);
            if (frame.visible() && isFrameVisible(frame, 30.0f, originY() + FRAMES_Y)) {
                frame.render(ctx);
            }
        }
        this.tab.scrollingAreaComponent().endArea(ctx, getContentHeight());
        if (needsScrollbar()) {
            this.scrollbar.render(ctx);
        }
        this.titleBadge.render(ctx);
        ctx.text(
                this.font,
                "Available themes",
                16,
                this.headerBounds.x() + 10.0f,
                this.headerBounds.y() + this.titleBadge.height() + 8.0f,
                colors.computeColor(ctx.theme().palette().text().tone(300).argb())
        );
        colors.pop();
    }

    @Override
    public void layout(LayoutScaleContext context) {
        this.tab.frames().forEach(frame -> frame.layout(context));
        this.headerBounds.withPosition(originX() + SIDE_PADDING, originY() + 16.0f)
                .withSize(width() - SIDE_PADDING * 2.0f - 16.0f, HEADER_HEIGHT);
        this.titleBadge.layout(context);
        this.titleBadge.setPosition(this.headerBounds.x() + 10.0f, this.headerBounds.y());
        layoutScrollbar(context);
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.scrollbar.animation(engine);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        if (needsScrollbar()) {
            handled |= this.scrollbar.handleInput(context, handled);
        }
        InputEventContext offset =
                context.withMouseOffset(0.0f, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame frame : this.tab.frames()) {
            if (frame.visible() && isFrameVisible(frame, 30.0f, originY() + FRAMES_Y)) {
                handled |= frame.handleInput(offset, handled);
            }
        }
        return handled | this.tab.scrollingAreaComponent().handleInput(context, handled);
    }

    @Override
    public float getContentHeight() {
        float maximum = 0.0f;
        for (AbstractFrame frame : this.tab.frames()) {
            if (frame.visible()) {
                float bottom = frame.y() - this.tab.framesOriginY() - FRAMES_Y
                        + frame.height() + frame.contentHeight();
                maximum = Math.max(maximum, bottom);
            }
        }
        return maximum + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> frames = this.tab.frames().stream()
                .filter(AbstractFrame::visible)
                .toList();
        float cardWidth = frames.stream()
                .map(AbstractFrame::width)
                .max(Float::compare)
                .orElse(0.0f);
        int columns = cardWidth > 0.0f
                ? Math.max(1, (int) Math.floor(
                        (width() - SIDE_PADDING * 2.0f + COLUMN_GAP)
                                / (cardWidth + COLUMN_GAP)
                ))
                : 1;
        float[] columnHeights = new float[columns];
        for (AbstractFrame frame : frames) {
            int column = indexOfMin(columnHeights);
            float frameX = originX() + SIDE_PADDING + column * (cardWidth + COLUMN_GAP);
            float frameY = originY() + FRAMES_Y + columnHeights[column];
            if (frame instanceof ThemeCard2 card) {
                card.targetPosition(
                        frameX,
                        frameY,
                        !this.tab.forceImmediateFramePositioning()
                );
            } else {
                frame.setPosition(frameX, frameY);
            }
            columnHeights[column] += frame.height() + frame.contentHeight() + ROW_GAP;
        }
    }

    private void layoutScrollbar(LayoutScaleContext context) {
        float availableHeight = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(
                originX() + width() - SIDE_PADDING - this.scrollbar.width(),
                this.headerBounds.y() + this.headerBounds.height()
        );
        this.scrollbar.setSize(this.scrollbar.width(), availableHeight);
        this.scrollbar.layout(context);
    }

    private boolean needsScrollbar() {
        return getContentHeight() > MenuWindow.MENU_HEIGHT
                - MenuWindow.COLLAPSED_HEADER_HEIGHT
                - this.headerBounds.height() - 15.5f;
    }

    @Override
    public float width() {
        return MenuWindow.CONTENT_WIDTH;
    }
}
