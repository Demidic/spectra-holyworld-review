package ru.spectra.client.util;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.ClientListener;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.module.Module;
import ru.spectra.client.ui.MenuTabElement;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ModuleAccessController implements ClientListener {
    private final Map<Module, Boolean> lastAvailability = new IdentityHashMap<>();

    public ModuleAccessController() {
        Spectra.INSTANCE.eventDispatcher().register(ClientTickEvent.class, event -> update());
    }

    private void update() {
        if (Spectra.INSTANCE.moduleRepository() == null) {
            return;
        }
        boolean visibilityChanged = false;
        for (Module module : Spectra.INSTANCE.moduleRepository().getModules()) {
            boolean available = module.isAvailable();
            Boolean previous = this.lastAvailability.put(module, available);
            visibilityChanged |= previous == null || previous.booleanValue() != available;
            module.enforceServerAccessPolicy();
        }
        if (visibilityChanged && Spectra.INSTANCE.menuWindow() != null) {
            refresh(Spectra.INSTANCE.tabsController().render());
            refresh(Spectra.INSTANCE.tabsController().misc());
        }
    }

    private static void refresh(MenuTabElement tab) {
        if (tab != null) {
            tab.refreshFrameVisibility();
        }
    }
}
