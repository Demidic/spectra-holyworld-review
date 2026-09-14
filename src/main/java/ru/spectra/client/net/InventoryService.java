package ru.spectra.client.net;
import ru.spectra.client.type.CombatPauseManager;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.InventoryItemFinder;
import ru.spectra.client.util.InventoryTask;
import ru.spectra.client.util.ItemInteractionHelper;
import ru.spectra.client.type.Mc;
import ru.spectra.client.module.Module;
import ru.spectra.client.util.PlayerActionUtil;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.RotationManager;
import ru.spectra.client.util.SilentSlotManager;
import ru.spectra.client.model.SlotSearchResult2;
import ru.spectra.client.util.SwapUtil;

import net.minecraft.util.Hand;

public class InventoryService {
    public final ItemInteractionHelper itemInteractor = new ItemInteractionHelper();

    public final SilentSlotManager hotbarSlotSwapper = new SilentSlotManager();

    public final InventoryItemFinder searcher = new InventoryItemFinder();

    public InventoryService() {
        Spectra.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, class130Var -> {
            if (Mc.INSTANCE.isWorldLoaded() && class130Var.isPre() && !CombatPauseManager.INSTANCE.shouldPauseSwaps()) {
                this.hotbarSlotSwapper.update();
            }
        });
    }

    public void addTask(InventoryTask class012Var, Module class605Var) {
        if (Mc.INSTANCE.getPlayer() == null) {
            return;
        }
        SlotSearchResult2 class329VarResult = class012Var.result();
        if (class329VarResult.found()) {
            SwapUtil.swapAndExecute(class329VarResult.slotReference().increasedSlot(), class012Var.rotation(), class012Var.needStop(), () -> {
                PlayerActionUtil.INSTANCE.interactItem(Hand.MAIN_HAND, RotationManager.INSTANCE.getCurrentRotation(), false);
            });
        }
    }

    public ItemInteractionHelper itemInteractor() {
        return this.itemInteractor;
    }

    public InventoryItemFinder searcher() {
        return this.searcher;
    }

    public SilentSlotManager hotbarSlotSwapper() {
        return this.hotbarSlotSwapper;
    }
}
