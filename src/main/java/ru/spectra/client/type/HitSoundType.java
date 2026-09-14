package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum HitSoundType implements DisplayNamed {
    TYPE_1(Lang.HITSOUNDS_TYPE_1),
    TYPE_2(Lang.HITSOUNDS_TYPE_2),
    TYPE_3(Lang.HITSOUNDS_TYPE_3),
    TYPE_4(Lang.HITSOUNDS_TYPE_4),
    MOANS(Lang.HITSOUNDS_TYPE_MOANS);

    final Translation displayName;

    HitSoundType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
