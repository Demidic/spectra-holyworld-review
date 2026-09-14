package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.ClientLocalization;

public enum SurfaceStyle implements DisplayNamed {
    NORMAL("Normal", "Обычный"),
    BLURRED("Blurred", "С размытием");

    private final String english;
    private final String russian;

    SurfaceStyle(String english, String russian) {
        this.english = english;
        this.russian = russian;
    }

    @Override
    public Translation getDisplayName() {
        return Translation.clearText(ClientLocalization.text(this.english, this.russian));
    }
}
