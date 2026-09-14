package ru.spectra.client.util;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.model.SlotSearchResult2;

import java.util.function.Predicate;

public class InventoryTask {
    private final Predicate predicate;
    private final SlotSearchResult2 result;
    private final boolean needStop;
    private final boolean restoreSlot;
    private final boolean rotateBeforeUse;
    private Rotation rotation;

    private InventoryTask(Predicate predicate, SlotSearchResult2 result, boolean needStop,
                          boolean restoreSlot, boolean rotateBeforeUse) {
        this.predicate = predicate;
        this.result = result;
        this.needStop = needStop;
        this.restoreSlot = restoreSlot;
        this.rotateBeforeUse = rotateBeforeUse;
        this.rotation = Rotation.playerRotation();
    }

    public static InventoryTask create(Predicate predicate, SlotSearchResult2 class329Var, boolean z, boolean z2, boolean z3) {
        return new InventoryTask(predicate, class329Var, z, z2, z3);
    }

    public boolean needStop() {
        return this.needStop;
    }

    public SlotSearchResult2 result() {
        return this.result;
    }

    public Rotation rotation() {
        return this.rotation;
    }

    public InventoryTask withRotation(Rotation class007Var) {
        this.rotation = class007Var == null ? Rotation.playerRotation() : class007Var;
        return this;
    }
}
