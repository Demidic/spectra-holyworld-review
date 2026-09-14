package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum SkyShaderMode implements DisplayNamed {
    NORTHERN_LIGHTS("Northern Lights"),
    COSMIC_NIGHT("Cosmic Night"),
    STORM_VEIL("Storm Veil"),
    ASTRAL_RIFT("Astral Rift");

    private final Translation displayName;

    SkyShaderMode(String name) {
        displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
