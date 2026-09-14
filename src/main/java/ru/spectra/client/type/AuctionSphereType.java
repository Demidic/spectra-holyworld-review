package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.Lang;
import ru.spectra.client.model.Translation;

public enum AuctionSphereType implements DisplayNamed {
    CHIMERA(Lang.AUCTION_HELPER_SPHERES_CHIMERA),
    ANDROMEDA(Lang.AUCTION_HELPER_SPHERES_ANDROMEDA),
    PANDORA(Lang.AUCTION_HELPER_SPHERES_PANDORA),
    TITAN(Lang.AUCTION_HELPER_SPHERES_TITAN);

    final Translation displayName;

    AuctionSphereType(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
