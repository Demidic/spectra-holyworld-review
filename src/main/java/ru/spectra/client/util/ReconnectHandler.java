package ru.spectra.client.util;
import ru.spectra.client.net.ReconnectTask;

import java.util.List;

public interface ReconnectHandler {
    List<String> getServerIds();

    ReconnectTask createProcess();
}
