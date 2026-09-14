package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum ShaderEffectMode implements DisplayNamed {
    CLASSIC("Classic"),
    PLASMA("Plasma"),
    NEBULA("Nebula"),
    COBWEB("Cobweb"),
    WAVE("Wave"),
    CLOUD("Cloud"),
    PULSE("Pulse");

    private final Translation displayName;

    ShaderEffectMode(String name) {
        this.displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
