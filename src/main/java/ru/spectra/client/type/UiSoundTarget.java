package ru.spectra.client.type;

import ru.spectra.client.Lang;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum UiSoundTarget implements DisplayNamed {
    MODULE_TOGGLE(Lang.CLIENTSOUNDS_TARGET_MODULES),
    SETTING_TOGGLE(Lang.CLIENTSOUNDS_TARGET_SETTINGS),
    SLIDER_MOVE(Lang.CLIENTSOUNDS_TARGET_SLIDERS);

    private final Translation displayName;

    UiSoundTarget(Translation displayName) {
        this.displayName = displayName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
