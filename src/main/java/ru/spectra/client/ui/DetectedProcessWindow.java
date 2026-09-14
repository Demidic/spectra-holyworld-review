package ru.spectra.client.ui;
import ru.spectra.client.type.BlacklistedProcess;
import ru.spectra.client.model.ProcessWindowInfo;

public final class DetectedProcessWindow {
    public final BlacklistedProcess target;
    public final ProcessWindowInfo window;

    public DetectedProcessWindow(BlacklistedProcess class522Var, ProcessWindowInfo class519Var) {
        this.target = class522Var;
        this.window = class519Var;
    }
}
