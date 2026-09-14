package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum WorldWeather implements DisplayNamed {
    CLEAR("Clear"),
    RAIN("Rain"),
    THUNDER("Thunder");

    private final Translation displayName;

    WorldWeather(String displayName) {
        this.displayName = Translation.clearText(displayName);
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
