package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.net.TooltipService;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

public class IconButtonWidget extends AbstractWidget {
    public final GlTexture icon;
    public final float iconWidth;
    public final float iconHeight;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public Translation tooltipText = null;
    public GlTexture tooltipIcon = null;
    public boolean tooltipShown = false;
    public int color = -1;

    public IconButtonWidget(GlTexture class073Var, Runnable runnable, float f, float f2) {
        this.clickable.clickCallback(runnable);
        this.icon = class073Var;
        this.iconWidth = f;
        this.iconHeight = f2;
    }

    public IconButtonWidget tooltip(Translation class254Var, GlTexture class073Var) {
        this.tooltipText = class254Var;
        this.tooltipIcon = class073Var;
        return this;
    }

    @Override
    public void render(DrawCtx class699Var) {
        DrawEngine class154VarDrawEngine = class699Var.drawEngine();
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
        if (this.color == -1) {
            this.color = class764VarPalette.text().tone(500).argb();
        }
        int iComputeColor = class115VarColorStack.computeColor(this.color);
        class699Var.texture(this.icon, x(), y(), this.iconWidth, this.iconHeight, class115VarColorStack.interpolate(iComputeColor, class115VarColorStack.brighten(iComputeColor, 0.8f), this.clickable.hoverAnimation()));
        TooltipService class736Var = Spectra.INSTANCE.menuWindow().tooltipService();
        if (class736Var != null && class736Var.isOwnedBy(this) && this.tooltipShown) {
            class736Var.updateAnchor(this, bounds(), class699Var);
        }
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = this.clickable.handleInput(class688Var, z);
        boolean zState = this.clickable.hoverAnimation().state();
        TooltipService class736Var = Spectra.INSTANCE.menuWindow().tooltipService();
        if ((class688Var.inputEvent() instanceof CursorMoveInput) && class736Var != null && this.tooltipText != null) {
            if (zState && !this.tooltipShown) {
                this.tooltipShown = true;
                class736Var.show(this, bounds(), this.tooltipText, this.tooltipIcon);
            } else if (!zState && this.tooltipShown) {
                this.tooltipShown = false;
                class736Var.hide(this);
            }
        }
        return zHandleInput;
    }

    public WidgetBounds bounds() {
        return new WidgetBounds(x(), y(), width(), height());
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        setSize(this.iconWidth, this.iconHeight);
        this.clickable.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.clickable.animate(class141Var);
    }

    public void setColor(int i) {
        this.color = i;
    }
}
