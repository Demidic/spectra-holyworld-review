package ru.spectra.client.type;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.math.Easings;
import ru.spectra.client.render.ToggleAnimator;

public class ModeOption<T> {
    public final T option;
    public final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public T option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public ModeOption(T t) {
        this.option = t;
    }
}
