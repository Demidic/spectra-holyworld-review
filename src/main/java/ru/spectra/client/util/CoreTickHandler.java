package ru.spectra.client.util;
import ru.spectra.client.event.ClientListener;
import ru.spectra.client.type.CombatPauseManager;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import ru.spectra.client.event.PlayerTickEvent;

public class CoreTickHandler implements ClientListener {
    public CoreTickHandler() {
        Spectra.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, class130Var -> {
            if (Mc.INSTANCE.isWorldLoaded() && class130Var.isPre()) {
                StaffDetector.update();
                GrimDelayHandler.tick();
                PvPModeDetector.tick();
                ServerUtil.tick();
                CombatPauseManager.INSTANCE.update();
            }
        }, EventPriority.LOW);
    }
}
