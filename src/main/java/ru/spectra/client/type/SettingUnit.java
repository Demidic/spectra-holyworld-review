package ru.spectra.client.type;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum SettingUnit {
    UNITS(Lang.UNIT_UNITS),
    BLOCKS(Lang.UNIT_BLOCKS),
    TICKS(Lang.UNIT_TICKS),
    MILLISECONDS(Lang.UNIT_MILLISECONDS),
    SECONDS(Lang.UNIT_SECONDS),
    HITPOINTS(Lang.UNIT_HITPOINTS),
    PERCENTS(Lang.UNIT_PERCENTS),
    DEGREES(Lang.UNIT_DEGREES),
    PIXELS(Lang.UNIT_PIXELS);

    public final Translation unitName;

    public String format(float f) {
        return this.unitName.effective();
    }

    public Translation getUnitName() {
        return this.unitName;
    }

    SettingUnit(Translation class254Var) {
        this.unitName = class254Var;
    }
}
