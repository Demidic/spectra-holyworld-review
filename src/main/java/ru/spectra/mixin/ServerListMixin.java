package ru.spectra.mixin;

import ru.spectra.client.model.PinnedServerEntry;
import ru.spectra.client.Spectra;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerList.class})
public abstract class ServerListMixin {

    @Unique
    private static final Set<String> SPECTRA_RETIRED_BUNDLED_SERVERS =
            Set.of("exp.metahvh.space:25565");

    @Shadow
    @Final
    private List<ServerInfo> servers;

    @Shadow
    @Nullable
    public abstract ServerInfo get(String str);

    @Shadow
    public abstract void add(ServerInfo serverInfo, boolean z);

    @Shadow
    public abstract void saveFile();

    @Inject(method = {"loadFile"}, at = {@At("TAIL")})
    public void loadFile(CallbackInfo callbackInfo) {
        boolean removedRetiredServer = this.servers.removeIf(serverInfo ->
                SPECTRA_RETIRED_BUNDLED_SERVERS.contains(
                        normalizeAddressLocal(serverInfo.address).toLowerCase(java.util.Locale.ROOT)));
        ensurePinnedOnTop(true);
        if (removedRetiredServer) {
            saveFile();
        }
    }

    @Inject(method = {"add"}, at = {@At("TAIL")})
    public void add(ServerInfo serverInfo, boolean z, CallbackInfo callbackInfo) {
        ensurePinnedOnTop(false);
    }

    @Inject(method = {"swapEntries"}, at = {@At("TAIL")})
    public void swapEntries(int i, int i2, CallbackInfo callbackInfo) {
        ensurePinnedOnTop(false);
    }

    @Inject(method = {"tryUnhide"}, at = {@At("TAIL")})
    public void tryUnhide(String str, CallbackInfoReturnable<ServerInfo> callbackInfoReturnable) {
        ensurePinnedOnTop(false);
    }

    @Unique
    private void ensurePinnedOnTop(boolean z) {
        List<PinnedServerEntry> listFavorites = Spectra.INSTANCE.pinnedServersController().favorites();
        boolean z2 = false;
        for (PinnedServerEntry class029Var : listFavorites) {
            String strNormalizeAddressLocal = normalizeAddressLocal(class029Var.address());
            if (indexOfAddress(strNormalizeAddressLocal) == -1) {
                add(new ServerInfo(class029Var.name(), strNormalizeAddressLocal, ServerInfo.ServerType.OTHER), false);
                z2 = true;
            }
        }
        ArrayList arrayList = new ArrayList(this.servers.size());
        Iterator<PinnedServerEntry> it = listFavorites.iterator();
        while (it.hasNext()) {
            String strNormalizeAddressLocal2 = normalizeAddressLocal(it.next().address());
            for (ServerInfo serverInfo : this.servers) {
                if (isSameAddressLocal(serverInfo.address, strNormalizeAddressLocal2)) {
                    arrayList.add(serverInfo);
                }
            }
        }
        for (ServerInfo serverInfo2 : this.servers) {
            boolean z3 = false;
            String strNormalizeAddressLocal3 = normalizeAddressLocal(serverInfo2.address);
            Iterator<PinnedServerEntry> it2 = listFavorites.iterator();
            while (it2.hasNext()) {
                if (isSameAddressLocal(strNormalizeAddressLocal3, it2.next().address())) {
                    z3 = true;
                    break;
                }
            }
            if (!z3) {
                arrayList.add(serverInfo2);
            }
        }
        this.servers.clear();
        this.servers.addAll(arrayList);
        if (z && z2) {
            saveFile();
        }
    }

    @Unique
    private int indexOfAddress(String str) {
        for (int i = 0; i < this.servers.size(); i++) {
            if (isSameAddressLocal(this.servers.get(i).address, str)) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private static boolean isSameAddressLocal(String str, String str2) {
        return normalizeAddressLocal(str).equals(normalizeAddressLocal(str2));
    }

    @Unique
    private static String normalizeAddressLocal(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return !strTrim.contains(":") ? strTrim + ":25565" : strTrim;
    }
}
