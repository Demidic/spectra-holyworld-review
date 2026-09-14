package ru.spectra.client.module;

import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.ModuleTab;

@Aliases(aliases = {"Shulker Preview", "Shulker Viewer", "Shulker Tooltip"})
public final class ShulkerPreviewModule extends Module {
    public ShulkerPreviewModule() {
        super(ModuleTab.RENDER, "Shulker Preview");
    }
}
