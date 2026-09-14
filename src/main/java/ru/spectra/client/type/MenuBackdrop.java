package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.ClientLocalization;

public enum MenuBackdrop implements DisplayNamed {
    DIM("Dimming", "Затемнение"),
    BLUR("Background blur", "Размытие фона");

    private final String english;
    private final String russian;

    MenuBackdrop(String english, String russian) {
        this.english = english;
        this.russian = russian;
    }

    @Override
    public Translation getDisplayName() {
        return Translation.clearText(ClientLocalization.text(this.english, this.russian));
    }
}

