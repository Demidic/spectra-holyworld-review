package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum WorldEffectType implements DisplayNamed {
    TNT_EXPLOSION("TNT explosion", 1_400L),
    MACE_SMASH("Mace smash", 950L),
    ARROW_BLOOD("Arrow blood", 6_500L);

    private final Translation displayName;
    private final long lifetimeMs;

    WorldEffectType(String displayName, long lifetimeMs) {
        this.displayName = Translation.clearText(displayName);
        this.lifetimeMs = lifetimeMs;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public long lifetimeMs() {
        return this.lifetimeMs;
    }
}
