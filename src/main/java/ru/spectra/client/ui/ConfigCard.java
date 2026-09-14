package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.util.WeightedEngine;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConfigCard extends AbstractFrame {
    private static final float CARD_WIDTH = 281.0f;
    private static final float CARD_HEIGHT = 62.0f;
    private static final float POPUP_WIDTH = 164.0f;
    private static final float POPUP_HEIGHT = 118.0f;

    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont menuIcon = Fonts.MENU_ICON.get();
    private final GlTexture folderIcon = texture("/icons/menu/new/folder_fill.png");
    private final GlTexture calendarIcon = texture("/icons/menu/new/calendar.png");
    private final GlTexture loadIcon = texture("/icons/menu/new/display.png");
    private final GlTexture saveIcon = texture("/icons/menu/new/save.png");
    private final GlTexture deleteIcon = texture("/icons/menu/new/trash.png");
    private final GlTexture renameIcon = texture("/icons/menu/new/pencil.png");
    private final GlTexture resetIcon = texture("/icons/menu/new/arrow_rotation.png");
    private final AnimatedFloat animX = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat animY = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator hoverAnimator = new ToggleAnimator(160, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator selectedAnimator = new ToggleAnimator(160, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator popupAnimator = new ToggleAnimator(170, Easings.EASE_IN_OUT_CUBIC);
    private final ClickableBehavior loadButton = new ClickableBehavior();
    private final ClickableBehavior settingsButton = new ClickableBehavior();
    private final ClickableBehavior[] actionButtons = new ClickableBehavior[Action.values().length];
    private final BiConsumer<CloudConfigCard, Action> actionCallback;
    private final Consumer<CloudConfigCard> afterLoad;

    private final CloudConfigCard configContext;
    private final boolean favorite;
    private boolean selected;
    private boolean positioned;
    private boolean dragging;
    private boolean popupOpen;

    public ConfigCard(
            CloudConfigCard configContext,
            Consumer<CloudConfigCard> afterLoad,
            BiConsumer<CloudConfigCard, Action> actionCallback
    ) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.configContext = configContext;
        this.afterLoad = afterLoad;
        this.actionCallback = actionCallback;
        this.favorite = Spectra.INSTANCE.configManager()
                .menuStateConfig().isConfigFavorite(configContext.cloudId());
        this.loadButton.clickCallback(this::loadConfig);
        this.settingsButton.clickCallback(() -> this.popupOpen = !this.popupOpen);
        for (int i = 0; i < this.actionButtons.length; i++) {
            Action action = Action.values()[i];
            this.actionButtons[i] = new ClickableBehavior().clickCallback(() -> runAction(action));
        }
    }

    private static GlTexture texture(String path) {
        return new GlTexture(new ClasspathResource(path)).setDimensions(11, 11);
    }

    @Override
    public void render(DrawCtx ctx) {
        layoutPrimaryButtons();
        ColorStack colors = ctx.colorStack();
        float hover = this.hoverAnimator.smoothAnimation();
        float selectedProgress = this.selected
                ? Math.max(0.35f, this.selectedAnimator.smoothAnimation())
                : this.selectedAnimator.smoothAnimation();
        if (selectedProgress > 0.01f) {
            colors.push();
            colors.alpha(selectedProgress);
            int outline = colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb());
            int background = colors.computeColor(ctx.theme().palette().surfaceBackground().tone(800).argb());
            ctx.fillOutlinedRoundedRect(x(), y(), width(), height(),
                    10.0f, 2.5f, outline, background);
            colors.pop();
        }

        float folderX = x() + 10.0f;
        float folderY = y() + 13.0f;
        int folderBackground = colors.computeColor(
                ctx.theme().palette().surfaceBackground().tone(600).argb()
        );
        ctx.fillRoundedRect(folderX, folderY, 36.0f, 36.0f, 10.0f, folderBackground);
        ctx.texture(this.folderIcon, folderX + 11.0f, folderY + 11.0f,
                14.0f, 14.0f,
                colors.computeColor(ctx.theme().palette().accentBright().argb()));

        float textX = folderX + 44.0f;
        float controlsLeft = hover > 0.02f ? this.loadButton.ownerX() - 8.0f : x() + width() - 11.0f;
        String name = fit(ctx, this.configContext.name(), controlsLeft - textX, 14);
        ctx.text(this.font, name, 14, textX, y() + 12.0f,
                colors.computeColor(0xF5F5F8));
        renderMetadata(ctx, colors, textX, y() + 36.0f, controlsLeft);

        if (hover > 0.01f) {
            colors.push();
            colors.alpha(hover);
            renderLoadButton(ctx, colors);
            renderSettingsButton(ctx, colors);
            colors.pop();
        }
    }

    private void renderMetadata(
            DrawCtx ctx,
            ColorStack colors,
            float startX,
            float metadataY,
            float rightEdge
    ) {
        int metadataColor = colors.computeColor(0xAAAAB4, 150);
        this.calendarIcon.setDimensions(10, 10);
        ctx.texture(this.calendarIcon, startX, metadataY, 10.0f, 10.0f, metadataColor);
        float dateX = startX + 14.0f;
        String date = this.configContext.date();
        ctx.text(this.font, date, 10, dateX, metadataY - 1.0f, metadataColor);
        float dateWidth = ctx.textWidthPhysical(this.font, date, 10);
        float bulletX = dateX + dateWidth + 7.0f;
        ctx.text(this.font, "\u2022", 10, bulletX, metadataY - 1.0f, metadataColor);

        String authorGlyph = "4";
        float authorIconX = bulletX + 10.0f;
        ctx.text(this.menuIcon, authorGlyph, 9, authorIconX, metadataY, metadataColor);
        float authorTextX = authorIconX + this.menuIcon.getWidth(authorGlyph, 9.0f) + 5.0f;
        float available = Math.max(0.0f, rightEdge - authorTextX);
        if (available > 5.0f) {
            ctx.text(this.font, fit(ctx, this.configContext.author().name(), available, 10),
                    10, authorTextX, metadataY - 1.0f, metadataColor);
        }
    }

    private void renderLoadButton(DrawCtx ctx, ColorStack colors) {
        float hover = this.loadButton.hoverAnimation().smoothAnimation();
        int outline = colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb());
        int background = colors.interpolate(
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(700).argb(), 0),
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(700).argb(), 140),
                this.loadButton.hoverAnimation()
        );
        int foreground = colors.computeColor(0xE7E7ED);
        ctx.fillOutlinedRoundedRect(
                this.loadButton.ownerX(), this.loadButton.ownerY(),
                this.loadButton.ownerW(), this.loadButton.ownerH(),
                6.0f, 2.5f, outline, background
        );
        ctx.texture(this.loadIcon,
                this.loadButton.ownerX() + 8.0f,
                this.loadButton.ownerY() + 7.0f,
                11.0f, 11.0f, foreground);
        ctx.text(this.font, "Load", 11,
                this.loadButton.ownerX() + 25.0f,
                this.loadButton.ownerY() + 6.0f,
                foreground);
    }

    private void renderSettingsButton(DrawCtx ctx, ColorStack colors) {
        float hover = this.settingsButton.hoverAnimation().smoothAnimation();
        int foreground = colors.interpolate(
                colors.computeColor(0xB9B9C2),
                colors.computeColor(0xFFFFFF),
                this.settingsButton.hoverAnimation()
        );
        String glyph = "\u041F";
        ctx.text(this.menuIcon, glyph, 12,
                this.settingsButton.ownerX()
                        + (this.settingsButton.ownerW() - this.menuIcon.getWidth(glyph, 12.0f)) / 2.0f,
                this.settingsButton.ownerY() + 6.0f,
                foreground);
    }

    @Override
    public void renderOverlays(DrawCtx ctx) {
        float animation = this.popupAnimator.smoothAnimation();
        if (animation <= 0.01f) {
            return;
        }
        layoutPopupButtons();
        ColorStack colors = ctx.colorStack();
        colors.push();
        colors.alpha(animation);
        float popupX = popupX();
        float popupY = popupY();
        ctx.fillOutlinedRoundedRect(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT,
                8.0f, 2.5f,
                colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb()),
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(900).argb()));
        for (int i = 0; i < Action.values().length; i++) {
            renderActionRow(ctx, colors, Action.values()[i], i);
        }
        colors.pop();
    }

    private void renderActionRow(DrawCtx ctx, ColorStack colors, Action action, int index) {
        ClickableBehavior button = this.actionButtons[index];
        float hover = button.hoverAnimation().smoothAnimation();
        if (hover > 0.01f) {
            ctx.fillRoundedRect(
                    button.ownerX(), button.ownerY(), button.ownerW(), button.ownerH(),
                    5.0f, colors.computeColor(0xFFFFFF, Math.round(5.0f + hover * 8.0f))
            );
        }
        int foreground = action == Action.DELETE
                ? colors.computeColor(0xFF8790)
                : colors.computeColor(0xDCDCE3);
        GlTexture icon = switch (action) {
            case SAVE -> this.saveIcon;
            case DELETE -> this.deleteIcon;
            case RENAME -> this.renameIcon;
            case RESET -> this.resetIcon;
        };
        ctx.texture(icon, button.ownerX() + 7.0f, button.ownerY() + 7.0f,
                11.0f, 11.0f, foreground);
        ctx.text(this.font, action.label, 11,
                button.ownerX() + 25.0f, button.ownerY() + 6.0f, foreground);
    }

    @Override
    public void layout(LayoutScaleContext context) {
        layoutPrimaryButtons();
        layoutPopupButtons();
        super.layout(context);
    }

    private void layoutPrimaryButtons() {
        this.loadButton.setDimensions(x() + width() - 108.0f, y() + 18.0f, 74.0f, 26.0f);
        this.settingsButton.setDimensions(x() + width() - 29.0f, y() + 18.0f, 20.0f, 26.0f);
    }

    private void layoutPopupButtons() {
        float popupX = popupX();
        float popupY = popupY();
        for (int i = 0; i < this.actionButtons.length; i++) {
            this.actionButtons[i].setDimensions(
                    popupX + 6.0f,
                    popupY + 6.0f + i * 27.0f,
                    POPUP_WIDTH - 12.0f,
                    25.0f
            );
        }
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (context.inputEvent() instanceof CursorMoveInput) {
            boolean hovered = !consumed && (context.inArea(x(), y(), width(), height())
                    || (this.popupOpen && context.inArea(popupX(), popupY(), POPUP_WIDTH, POPUP_HEIGHT)));
            this.hoverAnimator.state(hovered);
        }
        if (this.popupOpen && !consumed && context.inputEvent() instanceof MouseButtonInput mouse
                && mouse.action().press()
                && !context.inArea(popupX(), popupY(), POPUP_WIDTH, POPUP_HEIGHT)
                && !context.inArea(
                        this.settingsButton.ownerX(), this.settingsButton.ownerY(),
                        this.settingsButton.ownerW(), this.settingsButton.ownerH()
                )) {
            this.popupOpen = false;
            return true;
        }
        boolean handled = consumed;
        if (this.popupOpen) {
            for (int i = this.actionButtons.length - 1; i >= 0; i--) {
                handled |= this.actionButtons[i].handleInput(context, handled);
            }
        }
        if (!handled && !this.hoverAnimator.isZero()) {
            handled |= this.settingsButton.handleInput(context, false);
            handled |= this.loadButton.handleInput(context, handled);
        }
        return handled;
    }

    @Override
    public void animation(WeightedEngine engine) {
        if (this.positioned) {
            this.animX.animate(engine);
            this.animY.animate(engine);
            setPosition(this.animX.animatedValue(), this.animY.animatedValue());
        }
        this.selectedAnimator.state(this.selected).animate(engine);
        this.hoverAnimator.animate(engine);
        this.popupAnimator.state(this.popupOpen).animate(engine);
        this.loadButton.animate(engine);
        this.settingsButton.animate(engine);
        for (ClickableBehavior actionButton : this.actionButtons) {
            actionButton.animate(engine);
        }
    }

    private void loadConfig() {
        Spectra.INSTANCE.cloudConfigService()
                .downloadConfig(this.configContext.cloudId())
                .thenAccept(config -> {
                    Spectra.INSTANCE.cloudConfigService().applyConfig(config);
                    Spectra.INSTANCE.configManager().saveLastConfigID();
                    this.afterLoad.accept(this.configContext);
                })
                .exceptionally(error -> {
                    Spectra.LOGGER.error("Load failed", error);
                    return null;
                });
    }

    private void runAction(Action action) {
        this.popupOpen = false;
        this.actionCallback.accept(this.configContext, action);
    }

    private String fit(DrawCtx ctx, String value, float width, int size) {
        if (ctx.textWidthPhysical(this.font, value, size) <= width) {
            return value;
        }
        String suffix = "...";
        float available = width - ctx.textWidthPhysical(this.font, suffix, size);
        int end = value.length();
        while (end > 0
                && ctx.textWidthPhysical(this.font, value.substring(0, end), size) > available) {
            end--;
        }
        return (end == 0 ? "" : value.substring(0, end).stripTrailing()) + suffix;
    }

    private float popupX() {
        return x() + width() - POPUP_WIDTH;
    }

    private float popupY() {
        return y() + height() + 4.0f;
    }

    @Override
    public void onMenuDrag(boolean dragging) {
        this.dragging = dragging;
        if (dragging && this.positioned) {
            this.animX.set(x());
            this.animY.set(y());
            this.animX.destination(x());
            this.animY.destination(y());
        }
    }

    public void targetPosition(float x, float y, boolean animate) {
        if (this.positioned && animate && !this.dragging) {
            this.animX.destination(x);
            this.animY.destination(y);
        } else {
            this.positioned = true;
            snapTo(x, y);
        }
    }

    private void snapTo(float x, float y) {
        setPosition(x, y);
        this.animX.set(x);
        this.animY.set(y);
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

    public CloudConfigCard configContext() {
        return this.configContext;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public boolean selected() {
        return this.selected;
    }

    public ConfigCard selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public enum Action {
        SAVE("Save"),
        DELETE("Delete"),
        RENAME("Rename"),
        RESET("Reset to defaults");

        private final String label;

        Action(String label) {
            this.label = label;
        }
    }
}
