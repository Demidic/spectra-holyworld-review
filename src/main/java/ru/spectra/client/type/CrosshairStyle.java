package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum CrosshairStyle implements DisplayNamed {
    DEFAULT(Lang.CROSSHAIR_TYPE_DEFAULT),
    CIRCLE(Lang.CROSSHAIR_TYPE_CIRCLE);

    final Translation displayName;

    CrosshairStyle(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
