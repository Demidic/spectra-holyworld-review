package ru.spectra.client.config;
import ru.spectra.client.event.ClientListener;
import ru.spectra.client.util.CombatDetector;
import ru.spectra.client.util.CoreTickHandler;
import ru.spectra.client.util.FragmentShaderBuilder;
import ru.spectra.client.event.GrimInputListener;
import ru.spectra.client.util.KeybindHandler;
import ru.spectra.client.util.MatrixCaptureHandler;
import ru.spectra.client.util.ModuleAccessController;
import ru.spectra.client.event.PlayerListListener;
import ru.spectra.client.util.SlotSyncHandler;

import java.util.ArrayList;
import java.util.List;

public class ListenerRepository {
    public final List<ClientListener> listeners = new ArrayList();

    public ListenerRepository() {
        setup();
    }

    public void setup() {
        registerListeners(new SlotSyncHandler(), new KeybindHandler(), new CombatDetector(),
                new FragmentShaderBuilder(), new GrimInputListener(), new PlayerListListener(),
                new MatrixCaptureHandler(), new CoreTickHandler(), new ModuleAccessController());
    }

    public void registerListeners(ClientListener... class291VarArr) {
        this.listeners.addAll(List.of(class291VarArr));
    }

    public List<ClientListener> getListeners() {
        return this.listeners;
    }
}
