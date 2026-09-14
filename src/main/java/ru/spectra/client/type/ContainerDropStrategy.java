package ru.spectra.client.type;
import ru.spectra.client.util.ContainerItemDropper;
import ru.spectra.client.util.ItemDropper;
import ru.spectra.client.util.PlayerInventoryDropper;

import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

public class ContainerDropStrategy implements ItemDropper {
    @Override
    public void dropItems(ScreenHandler screenHandler, int i, Item item) {
        if (screenHandler instanceof GenericContainerScreenHandler) {
            if (i < ((GenericContainerScreenHandler) screenHandler).getRows() * 9) {
                new ContainerItemDropper().dropItems(screenHandler, i, item);
            } else {
                new PlayerInventoryDropper().dropItems(screenHandler, i, item);
            }
        }
    }
}
