package ru.spectra.mixin;

import ru.spectra.client.accessor.ChatLineIdAccessor;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ChatHudLine.class})
public abstract class ChatHudLineMixin implements ChatLineIdAccessor {

    @Unique
    private String spectra_ru$id = null;

    @Override
    @Unique
    public void spectra_ru$setId(String str) {
        this.spectra_ru$id = str;
    }

    @Override
    @Unique
    public String spectra_ru$getId() {
        return this.spectra_ru$id;
    }
}
