package ru.spectra.client.model;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.math.Easings;
import ru.spectra.client.render.ToggleAnimator;

public class MultiSelectOption2<T> {
    public final T option;
    public final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator toggleAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);

    public MultiSelectOption2(T t) {
        this.option = t;
    }

    public T option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator toggleAnimation() {
        return this.toggleAnimation;
    }
}
