package ru.spectra.client.ui;

import ru.spectra.client.event.InputEvent;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.util.WeightedEngine;

import java.util.function.Consumer;

public class ConfigCreateCard extends AbstractFrame {
    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    private final TextInputField input = new TextInputField(this.font, 12).maxLength(50);
    private final GlTexture confirmIcon =
            new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png")).setDimensions(11, 11);
    private final GlTexture cancelIcon =
            new GlTexture(new ClasspathResource("/icons/menu/new/cross.png")).setDimensions(10, 10);
    private final ClickableBehavior confirm = new ClickableBehavior();
    private final ClickableBehavior cancel = new ClickableBehavior();
    private final ToggleAnimator appear = new ToggleAnimator(170, Easings.EASE_OUT_CUBIC);
    private final Consumer<String> submit;
    private final Runnable cancelAction;
    private boolean loading;
    private String error = "";

    public ConfigCreateCard(Consumer<String> submit, Runnable cancelAction) {
        super(new FrameElementColumn(0.0f, 0.0f));
        this.submit = submit;
        this.cancelAction = cancelAction;
        this.confirm.clickCallback(this::submit);
        this.cancel.clickCallback(this::cancel);
        this.input.changeText(value -> this.error = "");
        this.input.focusAtEnd();
        this.appear.state(true);
    }

    @Override
    public void render(DrawCtx ctx) {
        ColorStack colors = ctx.colorStack();
        colors.push();
        colors.alpha(this.appear.smoothAnimation());
        ctx.fillOutlinedRoundedRect(x(), y(), width(), height(), 10.0f, 2.5f,
                colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb()),
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(800).argb()));

        float inputX = x() + 12.0f;
        float inputY = y() + 11.0f;
        float inputWidth = width() - 78.0f;
        float inputHeight = 27.0f;
        int inputBorder = this.input.focused()
                ? colors.computeColor(ctx.theme().palette().accentBright().argb(), 165)
                : colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb());
        ctx.fillOutlinedRoundedRect(inputX, inputY, inputWidth, inputHeight,
                6.0f, 2.5f, inputBorder,
                colors.computeColor(ctx.theme().palette().surfaceBackground().tone(700).argb(), 80));
        renderInput(ctx, colors, inputX, inputY, inputWidth, inputHeight);

        renderIconButton(ctx, colors, this.confirm, this.confirmIcon, true);
        renderIconButton(ctx, colors, this.cancel, this.cancelIcon, false);
        if (!this.error.isBlank()) {
            ctx.text(this.font, this.error, 9, inputX, y() + 43.0f,
                    colors.computeColor(0xFF7B85));
        } else {
            ctx.text(this.font, this.loading ? "Creating..." : "Enter a name and press Enter", 9,
                    inputX, y() + 43.0f, colors.computeColor(0xAAAAB4, 130));
        }
        colors.pop();
    }

    private void renderInput(
            DrawCtx ctx,
            ColorStack colors,
            float inputX,
            float inputY,
            float inputWidth,
            float inputHeight
    ) {
        String text = this.input.text();
        float originX = inputX + 8.0f - this.input.viewportOffset();
        float textY = inputY + (inputHeight - this.font.getHeight(12.0f)) / 2.0f;
        ctx.drawEngine().beginStencil();
        ctx.fillRect(inputX + 6.0f, inputY, inputWidth - 12.0f, inputHeight, colors.white());
        ctx.drawEngine().prepareStencil(1);
        if (text.isBlank()) {
            ctx.text(this.font, "Configuration name", 12, originX, textY,
                    colors.computeColor(0xAAAAB4, 95));
        } else {
            ctx.text(this.font, text, 12, originX, textY,
                    colors.computeColor(0xF4F4F8));
        }
        if (this.input.isCursorVisible()) {
            float cursor = originX + ctx.textWidthPhysical(
                    this.font,
                    text.substring(0, this.input.cursorIndex()),
                    12
            );
            ctx.fillRect(cursor, textY, 1.0f, this.font.getHeight(12.0f), colors.white());
        }
        ctx.drawEngine().endStencil();
        if (this.input.focused()) {
            ctx.window().interceptKeyboard(true);
        }
    }

    private void renderIconButton(
            DrawCtx ctx,
            ColorStack colors,
            ClickableBehavior button,
            GlTexture icon,
            boolean accent
    ) {
        float hover = button.hoverAnimation().smoothAnimation();
        int border = accent
                ? colors.computeColor(ctx.theme().palette().accentBright().argb(), 170)
                : colors.computeColor(ctx.theme().palette().surfaceOutline().tone(600).argb());
        int background = accent
                ? colors.computeColor(ctx.theme().palette().accent().argb(), Math.round(130.0f + hover * 35.0f))
                : colors.computeColor(0xFFFFFF, Math.round(4.0f + hover * 6.0f));
        ctx.fillOutlinedRoundedRect(button.ownerX(), button.ownerY(), button.ownerW(), button.ownerH(),
                6.0f, 2.5f, border, background);
        ctx.texture(icon,
                button.ownerX() + (button.ownerW() - icon.width()) / 2.0f,
                button.ownerY() + (button.ownerH() - icon.height()) / 2.0f,
                icon.width(), icon.height(), colors.white());
    }

    @Override
    public void layout(LayoutScaleContext context) {
        float inputX = x() + 12.0f;
        float inputY = y() + 11.0f;
        float inputWidth = width() - 78.0f;
        this.input.listen(inputX, inputY, inputWidth, 27.0f,
                inputWidth - 16.0f, inputX + 8.0f, context.scaleFactor());
        this.confirm.setDimensions(x() + width() - 58.0f, inputY, 23.0f, 27.0f);
        this.cancel.setDimensions(x() + width() - 30.0f, inputY, 20.0f, 27.0f);
        super.layout(context);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (consumed) {
            return false;
        }
        InputEvent event = context.inputEvent();
        if (event instanceof KeyInput key && key.keyAction().press()) {
            if (key.keyCode() == 257 && this.input.focused()) {
                submit();
                return true;
            }
            if (key.keyCode() == 256) {
                cancel();
                return true;
            }
        }
        boolean handled = this.confirm.handleInput(context, false);
        handled |= this.cancel.handleInput(context, handled);
        handled |= this.input.handleInput(context, handled);
        return handled;
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.appear.animate(engine);
        this.confirm.animate(engine);
        this.cancel.animate(engine);
        this.input.animate(engine);
    }

    private void submit() {
        if (this.loading) {
            return;
        }
        String name = this.input.text().trim();
        if (name.isEmpty()) {
            this.error = "Name cannot be empty";
            this.input.focusAtEnd();
            return;
        }
        this.submit.accept(name);
    }

    private void cancel() {
        if (!this.loading) {
            this.cancelAction.run();
        }
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void setError(String error) {
        this.error = error == null ? "Failed to create configuration" : error;
        this.loading = false;
        this.input.focusAtEnd();
    }

    public void focus() {
        this.input.focusAtEnd();
    }

    @Override
    public float width() {
        return 281.0f;
    }

    @Override
    public float height() {
        return 62.0f;
    }

    @Override
    public float contentHeight() {
        return 0.0f;
    }
}
