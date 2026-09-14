package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum AuctionArmorType implements DisplayNamed {
    WITHOUT_THORNS(Lang.AUCTION_HELPER_ARMOR_WITHOUT_THORNS),
    PROTECTION5(Lang.AUCTION_HELPER_ARMOR_PROTECTION5),
    UNBREAKING5(Lang.AUCTION_HELPER_ARMOR_UNBREAKING5),
    MENDING(Lang.AUCTION_HELPER_ARMOR_MENDING);

    final Translation displayName;

    AuctionArmorType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
