package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum TargetEspMode implements DisplayNamed {
    SOULS(Translation.clearText("Souls")),
    CROSSES(Translation.clearText("Crosses")),
    CRYSTALS(Translation.clearText("Crystals"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    TargetEspMode(Translation class254Var) {
        this.displayName = class254Var;
    }
}
