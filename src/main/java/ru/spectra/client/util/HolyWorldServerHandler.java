package ru.spectra.client.util;
import ru.spectra.client.net.HolyWorldProcess;
import ru.spectra.client.net.ReconnectTask;

import java.util.List;

public class HolyWorldServerHandler implements ReconnectHandler {
    @Override
    public List<String> getServerIds() {
        return List.of("holyworld");
    }

    @Override
    public ReconnectTask createProcess() {
        return new HolyWorldProcess();
    }
}
