package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum AuctionTalismanType implements DisplayNamed {
    ECHIDNA(Lang.AUCTION_HELPER_TALISMANS_ECHIDNA),
    DAEDALUS(Lang.AUCTION_HELPER_TALISMANS_DAEDALUS),
    DESTROYER(Lang.AUCTION_HELPER_TALISMANS_DESTROYER),
    PUNISHER(Lang.AUCTION_HELPER_TALISMANS_PUNISHER);

    final Translation displayName;

    AuctionTalismanType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
