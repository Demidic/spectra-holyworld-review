package ru.spectra.client.event;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.StaffDetector;

public class PlayerListListener implements ClientListener {
    public PlayerListListener() {
        Spectra.INSTANCE.eventDispatcher().register(TabListEntryEvent.class, class038Var -> {
            if (Mc.INSTANCE.isWorldLoaded()) {
                StaffDetector.onPlayerListEvent(class038Var);
            }
        }, EventPriority.LOW);
    }
}
