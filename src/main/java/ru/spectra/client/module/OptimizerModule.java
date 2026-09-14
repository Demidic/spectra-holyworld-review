package ru.spectra.client.module;

import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.event.ModuleStateEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.EntityCullingBridge;

@Aliases(aliases = {
        "Optimization", "Culling", "Occlusion Culling",
        "Оптимизация", "Отсечение"
})
public final class OptimizerModule extends Module {
    public final BooleanSetting worldCulling = new BooleanSetting(
            Translation.clearText("Off-screen blocks"),
            Translation.clearText("Caches terrain during camera turns; uses native rendering while moving or with shader packs")
    ).setValue(false);
    public final BooleanSetting entityOcclusion = new BooleanSetting(
            Translation.clearText("Entity occlusion"),
            Translation.clearText("Skips entities that are fully hidden behind world geometry")
    ).setValue(true);
    public final BooleanSetting blockEntityOcclusion = new BooleanSetting(
            Translation.clearText("Block entity occlusion"),
            Translation.clearText("Skips hidden chests, signs and other block entities")
    ).setValue(true);
    public final BooleanSetting tickCulling = new BooleanSetting(
            Translation.clearText("Risky tick culling"),
            Translation.clearText("Stops updates for hidden entities; may conflict with gameplay mods")
    ).setValue(false);
    public final NumberSetting tracingDistance = new NumberSetting(
            Translation.clearText("Occlusion distance"),
            Translation.clearText("Maximum distance used for entity visibility checks")
    ).range(32.0f, 256.0f).currentValue(128.0f).step(16.0f);

    private final EntityCullingBridge bridge = new EntityCullingBridge();
    private int lastSignature = Integer.MIN_VALUE;

    public OptimizerModule() {
        super(ModuleTab.MISC, "Optimizer");
        addSettings(
                this.worldCulling,
                this.entityOcclusion,
                this.blockEntityOcclusion,
                this.tickCulling,
                this.tracingDistance
        );
        setStateSilent(true);
        register(ClientTickEvent.class, ignored -> sync(false));
        register(ModuleStateEvent.class, event -> {
            if (event.module() == this) {
                sync(true);
            }
        });
    }

    private void sync(boolean force) {
        int distance = Math.round(this.tracingDistance.currentValue());
        int signature = java.util.Objects.hash(
                isState(),
                this.entityOcclusion.isValue(),
                this.blockEntityOcclusion.isValue(),
                this.tickCulling.isValue(),
                distance
        );
        if (!force && signature == this.lastSignature) {
            return;
        }
        if (this.bridge.apply(
                isState(),
                this.entityOcclusion.isValue(),
                this.blockEntityOcclusion.isValue(),
                this.tickCulling.isValue(),
                distance
        )) {
            this.lastSignature = signature;
        }
    }
}


