package ru.spectra.client.util;
import ru.spectra.client.net.ReallyWorldProcess;
import ru.spectra.client.net.ReconnectTask;

import java.util.List;

public class ReallyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("reallyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new ReallyWorldProcess();
    }
}
