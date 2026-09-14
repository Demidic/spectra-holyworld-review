package ru.spectra.client.ui;

import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.util.WeightedEngine;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ConfigHeaderButton extends AbstractWidget {
    private final Translation label;
    private final MsdfFont font;
    private final GlTexture icon;
    private final Runnable action;
    private final boolean accentButton;
    private final BooleanSupplier active;
    private final Supplier<String> labelSupplier;
    private final ClickableBehavior click = new ClickableBehavior();

    public ConfigHeaderButton(
            Translation label,
            MsdfFont font,
            GlTexture icon,
            Runnable action,
            boolean accentButton,
            BooleanSupplier active
    ) {
        this(label, font, icon, action, accentButton, active, null);
    }

    public ConfigHeaderButton(
            Translation label,
            MsdfFont font,
            GlTexture icon,
            Runnable action,
            boolean accentButton,
            BooleanSupplier active,
            Supplier<String> labelSupplier
    ) {
        this.label = label;
        this.font = font;
        this.icon = icon;
        this.action = action;
        this.accentButton = accentButton;
        this.active = active == null ? () -> false : active;
        this.labelSupplier = labelSupplier;
        this.click.clickCallback(() -> {
            if (this.action != null) {
                this.action.run();
            }
        });
    }

    @Override
    public void render(DrawCtx ctx) {
        this.click.setDimensions(x(), y(), width(), height());
        ColorStack colors = ctx.colorStack();
        ThemePalette palette = ctx.theme().palette();
        boolean enabled = this.active.getAsBoolean();
        float hover = this.click.hoverAnimation().smoothAnimation();
        int accent = colors.computeColor(palette.accentBright().argb());
        int outline;
        int background;
        int foreground;
        if (this.accentButton) {
            outline = accent;
            background = colors.brightenedInterpolatedColor(accent, 0.08f, this.click.hoverAnimation());
            foreground = colors.white();
        } else {
            outline = colors.computeColor(palette.surfaceOutline().tone(600).argb());
            background = colors.interpolate(
                    colors.computeColor(palette.surfaceBackground().tone(700).argb(), 0),
                    colors.computeColor(palette.surfaceBackground().tone(700).argb(), 140),
                    this.click.hoverAnimation()
            );
            foreground = colors.computeColor(palette.text().tone(100).argb());
        }
        ctx.fillOutlinedRoundedRect(x(), y(), width(), height(), 6.0f,
                this.accentButton ? 1.0f : 2.5f, outline, background);
        ctx.texture(this.icon, x() + 8.0f, y() + (height() - 12.0f) / 2.0f,
                12.0f, 12.0f, foreground);
        ctx.text(this.font, labelText(), 12,
                x() + 25.0f,
                y() + (height() - this.font.getHeight(12.0f)) / 2.0f,
                foreground);
    }

    @Override
    public void layout(LayoutScaleContext context) {
        this.icon.setDimensions(12, 12);
        setSize(33.0f + context.textWidthPhysical(this.font, labelText(), 12), 27.0f);
        this.click.setDimensions(x(), y(), width(), height());
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        return this.click.handleInput(context, consumed);
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.click.animate(engine);
    }

    private String labelText() {
        return this.labelSupplier == null ? this.label.effective() : this.labelSupplier.get();
    }
}
