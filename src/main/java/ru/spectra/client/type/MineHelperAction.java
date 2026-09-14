package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum MineHelperAction implements DisplayNamed {
    NEXT_MINE(Lang.MINE_HELPER_HELP_TYPE_NEXT_MINE),
    CLEAN_INVENTORY(Lang.MINE_HELPER_HELP_TYPE_CLEAN_INVENTORY),
    SAVE_PICKAXE(Lang.MINE_HELPER_HELP_TYPE_SAVE_PICKAXE);

    public final Translation displayName;

    MineHelperAction(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
