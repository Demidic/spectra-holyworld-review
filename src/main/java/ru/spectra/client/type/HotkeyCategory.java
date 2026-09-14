package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.model.Translation;

public enum HotkeyCategory implements DisplayNamed {
    COMBAT(Lang.COMBAT, ModuleTab.COMBAT),
    MOVEMENT(Lang.MOVEMENT, ModuleTab.MOVEMENT),
    PLAYER(Lang.PLAYER, ModuleTab.PLAYER),
    RENDER(Lang.RENDER, ModuleTab.RENDER),
    MISC(Lang.MISC, ModuleTab.MISC);

    public final Translation displayName;
    public final ModuleTab tab;

    public static HotkeyCategory fromTab(ModuleTab class847Var) {
        for (HotkeyCategory class641Var : values()) {
            if (class641Var.tab == class847Var) {
                return class641Var;
            }
        }
        return null;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public ModuleTab getTab() {
        return this.tab;
    }

    HotkeyCategory(Translation class254Var, ModuleTab class847Var) {
        this.displayName = class254Var;
        this.tab = class847Var;
    }
}
