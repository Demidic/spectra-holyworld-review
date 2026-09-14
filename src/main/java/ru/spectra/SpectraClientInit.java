package ru.spectra;

import net.fabricmc.api.ClientModInitializer;
import ru.spectra.client.SpectraBootstrap;
import ru.spectra.client.net.HolyWorldFeatureControl;
import ru.spectra.ui.TitleScreenIntegration;

/** PRIVATE moderator review build. Production protection is not distributed here. */
public class SpectraClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TitleScreenIntegration.register();
        SpectraBootstrap.init();
        HolyWorldFeatureControl.register();
    }
}
