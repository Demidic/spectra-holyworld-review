package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.Translation;
import ru.spectra.client.model.WidgetBounds;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.Theme;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.type.ConfigOrigin;
import ru.spectra.client.type.ThemeMode;
import ru.spectra.client.util.WeightedEngine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ThemeCard2 extends AbstractFrame {
    private static final float CARD_WIDTH = 281.0f;
    private static final float CARD_HEIGHT = 85.0f;

    private final GlTexture themeIcon = texture("/icons/menu/new/tabs/themes.png", 14, 14);
    private final GlTexture calendarIcon = texture("/icons/menu/new/calendar.png", 10, 10);
    private final GlTexture loadIcon = texture("/icons/menu/new/display.png", 11, 11);
    private final MsdfFont boldFont = Fonts.INTER_BOLD.get();
    private final MsdfFont semiboldFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont menuIcon = Fonts.MENU_ICON.get();
    private final AnimatedFloat xAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat yAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator selectedAnimator = new ToggleAnimator(150, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator hoverAnimator = new ToggleAnimator(140, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator visibilityAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    private final WidgetBounds iconBounds = new WidgetBounds(0.0f, 0.0f, 36.0f, 36.0f);
    private final ClickableBehavior loadButton = new ClickableBehavior().clickCallback(this::selectExclusive);
    private final ThemeCard themeContext;
    private final IconLabelBadge modeBadge;
    private final List<Integer> paletteColors;

    private boolean pendingHide;
    private boolean selected;
    private boolean favorite;
    private boolean positionInitialized;
    private boolean menuDragging;

    public ThemeCard2(ThemeCard themeContext) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.themeContext = themeContext;
        this.modeBadge = new IconLabelBadge(
                this.themeIcon,
                Translation.clearText(
                        themeContext.type() == ThemeMode.LIGHT ? "Light theme" : "Dark theme"
                )
        ).uppercase(false);
        this.paletteColors = extractPaletteColors(themeContext.palette());
        this.visibilityAnimator.force(true);
    }

    private static GlTexture texture(String path, int width, int height) {
        return new GlTexture(new ClasspathResource(path)).setDimensions(width, height);
    }

    @Override
    public void render(DrawCtx ctx) {
        this.loadButton.setDimensions(x() + width() - 84.0f, y() + 16.0f, 74.0f, 26.0f);
        this.modeBadge.setPosition(
                x() + 10.0f,
                y() + height() - 10.0f - this.modeBadge.height()
        );
        ColorStack colors = ctx.colorStack();
        ThemePalette palette = ctx.theme().palette();
        colors.push();
        colors.alpha(this.visibilityAnimator.smoothAnimation());

        float surface = this.selected
                ? Math.max(0.45f, this.selectedAnimator.smoothAnimation())
                : Math.max(this.selectedAnimator.smoothAnimation(), this.hoverAnimator.smoothAnimation());
        if (surface > 0.01f) {
            colors.push();
            colors.alpha(surface);
            ctx.fillOutlinedRoundedRect(
                    x(), y(), width(), height(),
                    10.0f, 2.5f,
                    colors.computeColor(palette.surfaceOutline().tone(600).argb()),
                    colors.computeColor(palette.surfaceBackground().tone(800).argb())
            );
            colors.pop();
        }

        float iconX = x() + 10.0f;
        float iconY = y() + 10.0f;
        this.iconBounds.withPosition(iconX, iconY);
        ctx.fillRoundedRect(iconX, iconY, 36.0f, 36.0f, 10.0f,
                colors.computeColor(palette.surfaceBackground().tone(600).argb()));
        ctx.texture(this.themeIcon, iconX + 11.0f, iconY + 11.0f,
                14.0f, 14.0f, colors.computeColor(palette.accentBright().argb()));

        float textX = iconX + 44.0f;
        float rightEdge = this.loadButton.ownerX() - 8.0f;
        ctx.text(this.boldFont, fit(ctx, this.themeContext.name(), rightEdge - textX, 13),
                13, textX, y() + 11.0f,
                colors.computeColor(palette.text().tone(200).argb()));
        renderMetadata(ctx, colors, textX, y() + 34.0f, rightEdge);
        renderLoadButton(ctx, colors);

        this.modeBadge.setBaseColor(
                this.themeContext.type() == ThemeMode.LIGHT
                        ? colors.computeColor(0xDAD8E2, 24)
                        : colors.computeColor(0x171720, 180)
        );
        this.modeBadge.setTextColor(colors.computeColor(palette.text().tone(400).argb()));
        this.modeBadge.render(ctx);
        renderPalette(ctx, colors);
        colors.pop();
    }

    private void renderMetadata(
            DrawCtx ctx,
            ColorStack colors,
            float startX,
            float y,
            float rightEdge
    ) {
        int muted = colors.computeColor(ctx.theme().palette().text().tone(700).argb());
        ctx.texture(this.calendarIcon, startX, y, 10.0f, 10.0f, muted);
        float dateX = startX + 14.0f;
        String date = this.themeContext.date();
        ctx.text(this.semiboldFont, date, 10, dateX, y - 1.0f, muted);
        float bulletX = dateX + ctx.textWidthPhysical(this.semiboldFont, date, 10) + 7.0f;
        ctx.text(this.semiboldFont, "\u2022", 10, bulletX, y - 1.0f, muted);
        String authorGlyph = "4";
        float authorIconX = bulletX + 10.0f;
        ctx.text(this.menuIcon, authorGlyph, 9, authorIconX, y, muted);
        float authorTextX = authorIconX + this.menuIcon.getWidth(authorGlyph, 9.0f) + 5.0f;
        float available = Math.max(0.0f, rightEdge - authorTextX);
        if (available > 5.0f) {
            ctx.text(this.semiboldFont,
                    fit(ctx, this.themeContext.author().name(), available, 10),
                    10, authorTextX, y - 1.0f, muted);
        }
    }

    private void renderLoadButton(DrawCtx ctx, ColorStack colors) {
        int outline = colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb());
        int background = colors.interpolate(
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(700).argb(), 0),
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(700).argb(), 140),
                this.loadButton.hoverAnimation()
        );
        int foreground = colors.computeColor(ctx.theme().palette().text().tone(200).argb());
        ctx.fillOutlinedRoundedRect(
                this.loadButton.ownerX(), this.loadButton.ownerY(),
                this.loadButton.ownerW(), this.loadButton.ownerH(),
                6.0f, 2.5f, outline, background
        );
        ctx.texture(this.loadIcon,
                this.loadButton.ownerX() + 8.0f,
                this.loadButton.ownerY() + 7.0f,
                11.0f, 11.0f, foreground);
        ctx.text(this.semiboldFont, "Load", 11,
                this.loadButton.ownerX() + 25.0f,
                this.loadButton.ownerY() + 6.0f,
                foreground);
    }

    private void renderPalette(DrawCtx ctx, ColorStack colors) {
        float circleX = x() + width() - 10.0f;
        float circleY = this.modeBadge.y() + this.modeBadge.height() / 2.0f;
        for (int index = this.paletteColors.size() - 1; index >= 0; index--) {
            ctx.circle(circleX, circleY, 5.0f,
                    colors.computeColor(this.paletteColors.get(index)));
            circleX -= 16.0f;
        }
    }

    @Override
    public void layout(LayoutScaleContext context) {
        this.loadButton.setDimensions(x() + width() - 84.0f, y() + 16.0f, 74.0f, 26.0f);
        this.modeBadge.layout(context);
        this.modeBadge.setPosition(x() + 10.0f, y() + height() - 10.0f - this.modeBadge.height());
        super.layout(context);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (context.inputEvent() instanceof CursorMoveInput) {
            this.hoverAnimator.state(
                    !consumed && context.inArea(x(), y(), width(), height())
            );
        }
        return this.loadButton.handleInput(context, consumed);
    }

    @Override
    public void animation(WeightedEngine engine) {
        if (this.positionInitialized) {
            this.xAnimation.animate(engine);
            this.yAnimation.animate(engine);
            setPosition(this.xAnimation.animatedValue(), this.yAnimation.animatedValue());
        }
        this.selectedAnimator.state(this.selected).animate(engine);
        this.hoverAnimator.animate(engine);
        this.visibilityAnimator.animate(engine);
        this.loadButton.animate(engine);
        if (this.pendingHide && this.visibilityAnimator.isZero()) {
            this.pendingHide = false;
            visible(false);
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
    }

    public void selectExclusive() {
        MenuTabElement current = Spectra.INSTANCE.tabsController().current();
        for (AbstractFrame frame : current.frames()) {
            if (frame instanceof ThemeCard2 card) {
                card.selected(false);
            }
        }
        Spectra.INSTANCE.theme = Theme.of(
                this.themeContext.cloudId(),
                this.themeContext.name(),
                this.themeContext.author().name(),
                this.themeContext.kind(),
                this.themeContext.type(),
                this.themeContext.palette(),
                Instant.now(),
                Instant.now()
        );
        selected(true);
        current.markFramesDirty();
    }

    public void updateFilterVisibility(boolean visible) {
        if (!visible) {
            if (visible()) {
                this.visibilityAnimator.state(false);
                this.pendingHide = true;
            }
            return;
        }
        this.pendingHide = false;
        if (!visible()) {
            visible(true);
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.visibilityAnimator.state(true);
    }

    public void targetPosition(float x, float y, boolean animate) {
        if (!this.positionInitialized || !animate || this.menuDragging) {
            this.positionInitialized = true;
            snapTo(x, y);
        } else {
            this.xAnimation.destination(x);
            this.yAnimation.destination(y);
        }
    }

    private void snapTo(float x, float y) {
        setPosition(x, y);
        this.xAnimation.set(x);
        this.yAnimation.set(y);
    }

    @Override
    public void onMenuDrag(boolean dragging) {
        this.menuDragging = dragging;
        if (dragging && this.positionInitialized) {
            this.xAnimation.set(x());
            this.yAnimation.set(y());
            this.xAnimation.destination(x());
            this.yAnimation.destination(y());
        }
    }

    private String fit(DrawCtx ctx, String value, float width, int size) {
        if (width <= 0.0f) {
            return "";
        }
        if (ctx.textWidthPhysical(this.semiboldFont, value, size) <= width) {
            return value;
        }
        String suffix = "...";
        float available = width - ctx.textWidthPhysical(this.semiboldFont, suffix, size);
        int end = value.length();
        while (end > 0
                && ctx.textWidthPhysical(this.semiboldFont, value.substring(0, end), size) > available) {
            end--;
        }
        return (end == 0 ? "" : value.substring(0, end).stripTrailing()) + suffix;
    }

    private List<Integer> extractPaletteColors(ThemePalette palette) {
        return new ArrayList<>(List.of(
                palette.accent().argb(),
                palette.accentBright().argb(),
                palette.favorite().argb(),
                palette.surfaceBackground().tone(400).argb(),
                palette.surfaceBackground().tone(700).argb(),
                palette.text().tone(200).argb(),
                palette.text().tone(500).argb()
        ));
    }

    @Override
    public float width() {
        return CARD_WIDTH;
    }

    @Override
    public float height() {
        return CARD_HEIGHT;
    }

    @Override
    public float contentHeight() {
        return 0.0f;
    }

    public ThemeCard themeContext() {
        return this.themeContext;
    }

    public ThemeCard2 selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public boolean selected() {
        return this.selected;
    }

    public boolean favorite() {
        return this.favorite;
    }
}
