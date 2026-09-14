package ru.spectra.client.ui;

import ru.spectra.client.event.InputEvent;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.model.WidgetBounds;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;

import java.util.Objects;

/** Modal host shared by account details and feature-specific editors. */
public final class PopupWindow extends Widget implements WidgetParent {
    private static final float OUTER_PADDING = 14.0f;
    private static final float RADIUS = 10.0f;
    private static final float CLOSE_SIZE = 24.0f;

    private final ToggleAnimator openAnimator =
            new ToggleAnimator(180, Easings.EASE_IN_OUT_CUBIC);
    private final WidgetBounds popupBounds =
            new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    private final WidgetBounds contentBounds =
            new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    private final ClickableBehavior closeButton = new ClickableBehavior()
            .clickCallback(this::close);
    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();

    private PopupContent content;
    private boolean opened;
    private boolean anchored;
    private float anchorX;
    private float anchorY;

    public void open(PopupContent nextContent) {
        this.anchored = false;
        openContent(nextContent);
    }

    public void openAnchored(PopupContent nextContent, float anchorX, float anchorY) {
        this.anchored = true;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        openContent(nextContent);
    }

    private void openContent(PopupContent nextContent) {
        Objects.requireNonNull(nextContent, "popup content");
        if (this.content != null && this.content != nextContent) {
            this.content.onClose();
        }
        this.content = nextContent;
        this.opened = true;
        this.openAnimator.state(true);
        nextContent.onOpen();
    }

    public void close() {
        if (!this.opened) {
            return;
        }
        this.opened = false;
        this.openAnimator.state(false);
        if (this.content != null) {
            this.content.onClose();
        }
    }

    public boolean isOpen() {
        return this.opened || !this.openAnimator.isZero();
    }

    @Override
    public void layout(LayoutScaleContext context) {
        if (this.content == null) {
            return;
        }
        float popupWidth = Math.min(width() - OUTER_PADDING * 2.0f,
                Math.max(220.0f, this.content.preferredWidth()));
        float popupHeight = Math.min(height() - OUTER_PADDING * 2.0f,
                Math.max(150.0f, this.content.preferredHeight()));
        float popupX = this.anchored
                ? this.anchorX
                : x() + MenuWindow.SIDEBAR_WIDTH + 22.0f;
        float popupY = this.anchored
                ? this.anchorY - popupHeight / 2.0f
                : y() + (height() - popupHeight) / 2.0f;
        // Keep an anchored flyout beside its source row while ensuring every
        // account field stays reachable on small windows and non-default DPI.
        float viewportPadding = 8.0f;
        popupX = Math.max(viewportPadding,
                Math.min(popupX, context.logicalWidth() - popupWidth - viewportPadding));
        popupY = Math.max(viewportPadding,
                Math.min(popupY, context.logicalHeight() - popupHeight - viewportPadding));
        this.popupBounds.withPosition(popupX, popupY)
                .withSize(popupWidth, popupHeight);
        this.contentBounds.withPosition(popupX + OUTER_PADDING, popupY + OUTER_PADDING)
                .withSize(popupWidth - OUTER_PADDING * 2.0f,
                        popupHeight - OUTER_PADDING * 2.0f);
        this.closeButton.setDimensions(
                popupX + popupWidth - CLOSE_SIZE - 8.0f,
                popupY + 8.0f,
                CLOSE_SIZE, CLOSE_SIZE
        );
        this.content.layout(context, this.contentBounds);
    }

    @Override
    public void render(DrawCtx context) {
        if (this.content == null || this.openAnimator.isZero()) {
            return;
        }
        ColorStack colors = context.colorStack();
        colors.push();
        try {
            colors.alpha(this.openAnimator.smoothAnimation());
            MenuWindow.renderSurface(
                    context,
                    this.popupBounds.x(), this.popupBounds.y(),
                    this.popupBounds.width(), this.popupBounds.height(),
                    RADIUS
            );

            if (this.content.showCloseButton()) {
                float closeProgress = this.closeButton.hoverAnimation().smoothAnimation();
                if (closeProgress > 0.01f) {
                    context.fillRoundedRect(
                            this.closeButton.ownerX(), this.closeButton.ownerY(),
                            this.closeButton.ownerW(), this.closeButton.ownerH(),
                            6.0f, colors.computeColor(0xFFFFFFFF, 0.07f * closeProgress)
                    );
                }
                String close = "×";
                float closeWidth = context.textWidthPhysical(this.font, close, 17);
                context.text(this.font, close, 17,
                        this.closeButton.ownerX() + (CLOSE_SIZE - closeWidth) / 2.0f,
                        this.closeButton.ownerY()
                                + (CLOSE_SIZE - this.font.getHeight(17.0f)) / 2.0f,
                        colors.computeColor(0xFFD9D9DF));
            }

            this.content.render(context, this.contentBounds);
        } finally {
            colors.pop();
        }
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (this.content == null || this.openAnimator.isZero()) {
            return false;
        }
        if (!consumed && this.content.handleInput(context, false, this.contentBounds)) {
            return true;
        }
        if (this.content.showCloseButton()
                && this.closeButton.handleInput(context, consumed)) {
            return true;
        }
        if (consumed) {
            return false;
        }
        InputEvent event = context.inputEvent();
        if (event instanceof KeyInput key && key.keyAction().press()
                && key.keyCode() == 256) {
            close();
            return true;
        }
        if (event instanceof MouseButtonInput mouse && mouse.button() == 0
                && mouse.action().press()
                && !context.inArea(this.popupBounds.x(), this.popupBounds.y(),
                this.popupBounds.width(), this.popupBounds.height())) {
            close();
            return true;
        }
        return event instanceof CursorMoveInput
                || !(event instanceof KeyInput);
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.openAnimator.animate(engine);
        this.closeButton.animate(engine);
        if (this.content != null) {
            this.content.animation(engine);
            if (!this.opened && this.openAnimator.isZero()) {
                this.content = null;
            }
        }
    }
}
