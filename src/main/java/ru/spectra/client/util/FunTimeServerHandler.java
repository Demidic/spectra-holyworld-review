package ru.spectra.client.util;
import ru.spectra.client.net.FunTimeProcess;
import ru.spectra.client.net.ReconnectTask;

import java.util.List;

public class FunTimeServerHandler implements ReconnectHandler {
    public static final String funtimeId = "funtime";

    @Override
    public List<String> getServerIds() {
        return List.of(funtimeId, "spookytime");
    }

    @Override
    public ReconnectTask createProcess() {
        return new FunTimeProcess();
    }
}
