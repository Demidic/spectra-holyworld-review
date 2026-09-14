package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum TapeMouseHand implements DisplayNamed {
    RIGHT(Lang.TAPEMOUSE_HANDMODE_RIGHT),
    LEFT(Lang.TAPEMOUSE_HANDMODE_LEFT);

    final Translation displayName;

    TapeMouseHand(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
