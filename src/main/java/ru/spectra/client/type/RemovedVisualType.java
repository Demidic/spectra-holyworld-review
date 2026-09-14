package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum RemovedVisualType implements DisplayNamed {
    CAMERA_HURT(Lang.REMOVALS_CAMERA_HURT),
    FIRE_OVERLAY(Lang.REMOVALS_FIRE_OVERLAY),
    LAVA_OVERLAY(Lang.REMOVALS_LAVA_OVERLAY),
    SCOREBOARD(Lang.REMOVALS_SCOREBOARD),
    BOSS_BAR(Lang.REMOVALS_BOSS_BAR),
    TOTEM_POP(Lang.REMOVALS_TOTEM_POP),
    VIGNETTE(Lang.REMOVALS_VIGNETTE),
    GLOWING(Lang.REMOVALS_GLOWING),
    WITHER_HEARTS(Lang.REMOVALS_WITHER_HEARTS);

    final Translation displayName;

    RemovedVisualType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
