package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum HandShaderMode implements DisplayNamed {
    CLOUDS("Clouds"),
    NEBULA("Nebula"),
    AURORA("Aurora");

    private final Translation displayName;

    HandShaderMode(String name) {
        displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
