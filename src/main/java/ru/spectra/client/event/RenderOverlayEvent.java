package ru.spectra.client.event;
import ru.spectra.client.type.RenderOverlayType;

public class RenderOverlayEvent extends CancellableEvent {
    public final RenderOverlayType type;

    public RenderOverlayType getType() {
        return this.type;
    }

    public RenderOverlayEvent(RenderOverlayType class253Var) {
        this.type = class253Var;
    }
}
