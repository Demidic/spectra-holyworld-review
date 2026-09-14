package ru.spectra.client.util;
import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.event.ClientListener;
import ru.spectra.client.type.CombatPauseManager;
import ru.spectra.client.Spectra;

public class CombatDetector implements ClientListener {
    public CombatDetector() {
        Spectra.INSTANCE.eventDispatcher().register(AttackEntityEvent.class, class144Var -> {
            CombatPauseManager.INSTANCE.inCombatForAtLeast(40);
        });
    }
}
