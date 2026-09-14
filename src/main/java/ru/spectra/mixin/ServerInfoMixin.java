package ru.spectra.mixin;

import ru.spectra.client.net.PinnableServer;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ServerInfo.class})
public class ServerInfoMixin implements PinnableServer {

    @Unique
    private boolean spectra$pinned;

    @Override
    public boolean spectra$isPinned() {
        return this.spectra$pinned;
    }

    @Override
    public void spectra$setPinned(boolean z) {
        this.spectra$pinned = z;
    }
}
