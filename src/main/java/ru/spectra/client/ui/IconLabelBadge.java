package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.model.Translation;

public class IconLabelBadge extends AbstractWidget {
    public final MsdfFont font = Fonts.INTER_EXTRA_BOLD.get();
    static final int textSize = 9;
    static final float padding = 6.0f;
    public final Translation label;
    public final GlTexture icon;
    public float width;
    public Integer baseColor;
    public Integer textColor;
    private boolean uppercase = true;

    public IconLabelBadge(GlTexture class073Var, Translation class254Var) {
        this.label = class254Var;
        this.icon = class073Var;
    }

    @Override
    public void render(DrawCtx class699Var) {
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.colorStack();
        int iIntValue = this.baseColor != null ? this.baseColor.intValue() : class764VarPalette.accent().argb();
        int iIntValue2 = this.textColor != null ? this.textColor.intValue() : iIntValue;
        float height = this.font.getHeight(9.0f);
        float fHeight = height() / 2.0f;
        class699Var.fillRoundedRect(x(), y(), width(), height(), 5.0f, class115VarColorStack.computeColor(iIntValue, 0.15f));
        class699Var.textureVerticalC(this.icon, x() + padding, y() + fHeight, 10, 10, class115VarColorStack.computeColor(iIntValue2));
        String text = displayText();
        class699Var.text(this.font, text, textSize, x() + padding + 10.0f + 4.0f,
                (y() + fHeight) - (height / 2.0f), class115VarColorStack.computeColor(iIntValue2));
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.width = 26.0f + class698Var.textWidthPhysical(this.font, displayText(), textSize);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return 19.0f;
    }

    public void setBaseColor(Integer num) {
        this.baseColor = num;
    }

    public void setTextColor(Integer num) {
        this.textColor = num;
    }

    public IconLabelBadge uppercase(boolean uppercase) {
        this.uppercase = uppercase;
        return this;
    }

    private String displayText() {
        return this.uppercase ? this.label.effective().toUpperCase() : this.label.effective();
    }
}
