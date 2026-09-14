package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum BetterChatOption implements DisplayNamed {
    ANTI_SPAM(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_ANTI_SPAM),
    ANTI_CLEAR(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_ANTI_CLEAR),
    INFINITY(Lang.BETTERCHAT_CHAT_IMPROVEMENTS_INFINITY),
    SHOW_MESSAGE_TIME(Translation.clearText("Show message time")),
    AH_HELPER(Translation.clearText("AH helper")),
    FIX_COMMAND_LAYOUT(Translation.clearText("Fix command keyboard layout"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }

    BetterChatOption(Translation class254Var) {
        this.displayName = class254Var;
    }
}
