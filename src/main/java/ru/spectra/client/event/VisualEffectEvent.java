package ru.spectra.client.event;
import ru.spectra.client.type.VisualEffectType;

public class VisualEffectEvent extends CancellableEvent {
    public final VisualEffectType type;

    public VisualEffectType getType() {
        return this.type;
    }

    public VisualEffectEvent(VisualEffectType class259Var) {
        this.type = class259Var;
    }
}
