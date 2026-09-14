package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum AuctionElytraType implements DisplayNamed {
    UNBREAKING5(Lang.AUCTION_HELPER_ELYTRA_UNBREAKING5),
    MENDING(Lang.AUCTION_HELPER_ELYTRA_MENDING);

    final Translation displayName;

    AuctionElytraType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
