package ru.spectra.client.event;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.GrimDelayHandler;
import ru.spectra.client.type.Mc;

public class GrimInputListener implements ClientListener {
    public GrimInputListener() {
        Spectra.INSTANCE.eventDispatcher().register(MovementInputEvent.class, class040Var -> {
            if (Mc.INSTANCE.isWorldLoaded()) {
                GrimDelayHandler.input(class040Var);
            }
        }, EventPriority.HIGHEST);
    }
}
