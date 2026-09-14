package ru.spectra.client.render;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.math.Easings;

public class ColorSwatch2 {
    final int color;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator selectAnim = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public int color() {
        return this.color;
    }

    public ClickableBehavior clickable() {
        return this.clickable;
    }

    public ToggleAnimator selectAnim() {
        return this.selectAnim;
    }

    public ColorSwatch2(int i) {
        this.color = i;
    }
}
