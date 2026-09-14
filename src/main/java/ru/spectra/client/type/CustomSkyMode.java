package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum CustomSkyMode implements DisplayNamed {
    CLIENT("Client"),
    STATIC("Static"),
    CUSTOM("Custom"),
    SHADERS("Shaders");

    private final Translation displayName;

    CustomSkyMode(String name) {
        displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
