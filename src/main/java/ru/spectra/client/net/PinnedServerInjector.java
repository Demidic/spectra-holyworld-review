package ru.spectra.client.net;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import ru.spectra.mixin.accessors.ServerListAccessor;

public final class PinnedServerInjector {
    public static final List<PinnedServerEntry2> pinnedServers = List.of();

    public PinnedServerInjector() {
    }

    public static void inject(ServerList serverList) {
        List<ServerInfo> listSpectra$getServers = ((ServerListAccessor) serverList).spectra$getServers();
        for (int size = pinnedServers.size() - 1; size >= 0; size--) {
            PinnedServerEntry2 class711Var = pinnedServers.get(size);
            boolean z = false;
            Iterator<ServerInfo> it = listSpectra$getServers.iterator();
            while (it.hasNext()) {
                ServerInfo serverInfoEntry = it.next();
                PinnableServer class712Var = (PinnableServer) (Object) serverInfoEntry;
                if (serverInfoEntry.address.equalsIgnoreCase(class711Var.address())) {
                    class712Var.spectra$setPinned(true);
                    z = true;
                    break;
                }
            }
            if (!z) {
                ServerInfo serverInfo = new ServerInfo(class711Var.name(), class711Var.address(), ServerInfo.ServerType.OTHER);
                ((PinnableServer) (Object) serverInfo).spectra$setPinned(true);
                listSpectra$getServers.add(0, serverInfo);
            }
        }
    }
}
