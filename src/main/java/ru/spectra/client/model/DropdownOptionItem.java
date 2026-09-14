package ru.spectra.client.model;
import ru.spectra.client.ui.ClickableBehavior;
import ru.spectra.client.type.DropdownOption;
import ru.spectra.client.math.Easings;
import ru.spectra.client.render.ToggleAnimator;

public class DropdownOptionItem<T> {
    final DropdownOption<T> option;
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public DropdownOption<T> option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public DropdownOptionItem(DropdownOption<T> class739Var) {
        this.option = class739Var;
    }
}
