package ru.spectra.client.config;
import ru.spectra.client.util.ClientHandler;
import ru.spectra.client.util.DropAllHandler;

import java.util.ArrayList;
import java.util.List;

public class HandlerRepository {
    public final List<ClientHandler> listeners = new ArrayList();
    public DropAllHandler containerHandler;

    public HandlerRepository() {
        setup();
    }

    public void setup() {
        DropAllHandler class090Var = new DropAllHandler();
        this.containerHandler = class090Var;
        registerHandlers(class090Var);
    }

    public void registerHandlers(ClientHandler... class240VarArr) {
        this.listeners.addAll(List.of(class240VarArr));
    }

    public List<ClientHandler> getListeners() {
        return this.listeners;
    }

    public DropAllHandler getContainerHandler() {
        return this.containerHandler;
    }
}
