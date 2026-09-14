package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum WatermarkPosition implements DisplayNamed {
    TOP_LEFT(Translation.clearText("Top Left")),
    TOP_CENTER(Translation.clearText("Top Center"));

    private final Translation displayName;

    WatermarkPosition(Translation displayName) {
        this.displayName = displayName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
