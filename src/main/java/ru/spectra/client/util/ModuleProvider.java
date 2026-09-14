package ru.spectra.client.util;

import ru.spectra.client.module.*;
import ru.spectra.client.module.Module;
import ru.spectra.client.type.ModuleCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModuleProvider {
    public List<Supplier<Module>> getAll() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(combatModules());
        arrayList.addAll(movementModules());
        arrayList.addAll(renderModules());
        arrayList.addAll(playerModules());
        arrayList.addAll(miscModules());
        return arrayList;
    }

    public List<Supplier<Module>> combatModules() {
        return List.of(
                withCategory(AutoSwapModule::new, ModuleCategory.AUTOMATION)
        );
    }

    public List<Supplier<Module>> movementModules() {
        return List.of(
                withCategory(SprintModule::new, ModuleCategory.AUTOMATION)
        );
    }

    public List<Supplier<Module>> renderModules() {
        return List.of(new Supplier[]{
                WidgetsModule::new,
                withCategory(HudModules.Watermark::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.ActiveEffects::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Keybinds::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.TargetHud::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Coordinates::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Notifications::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Structures::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Inventory::new, ModuleCategory.INTERFACE),
                withCategory(HudModules.Cooldowns::new, ModuleCategory.INTERFACE),
                withCategory(ShulkerPreviewModule::new, ModuleCategory.INTERFACE),
                withCategory(TargetESPModule::new, ModuleCategory.VISUALIZATION),
                withCategory(SelfNameTagModule::new, ModuleCategory.VISUALIZATION),
                withCategory(ItemPhysicModule::new, ModuleCategory.WORLD),
                withCategory(TrailsModule::new, ModuleCategory.VISUALIZATION),
                withCategory(HitEffectModule::new, ModuleCategory.VISUALIZATION),
                withCategory(HitBoxCustomizerModule::new, ModuleCategory.VISUALIZATION),
                withCategory(SkeletonModule::new, ModuleCategory.VISUALIZATION),
                withCategory(ItemRadiusModule::new, ModuleCategory.VISUALIZATION),
                withCategory(KillEffectModule::new, ModuleCategory.VISUALIZATION),
                withCategory(BlockOutlineModule::new, ModuleCategory.WORLD),
                withCategory(ShaderHandModule::new, ModuleCategory.VISUALIZATION),
                withCategory(RemovalsModule::new, ModuleCategory.WORLD),
                withCategory(AspectRatioModule::new, ModuleCategory.INTERFACE),
                withCategory(FullBrightModule::new, ModuleCategory.WORLD),
                withCategory(PopChamsModule::new, ModuleCategory.VISUALIZATION),
                withCategory(JumpCircleModule::new, ModuleCategory.VISUALIZATION),
                withCategory(ChamsModule::new, ModuleCategory.VISUALIZATION),
                withCategory(WorldTweaksModule::new, ModuleCategory.WORLD),
                withCategory(WorldEffectsModule::new, ModuleCategory.WORLD),
                withCategory(ArmTweaksModule::new, ModuleCategory.VISUALIZATION),
                withCategory(CrosshairModule::new, ModuleCategory.INTERFACE),
                withCategory(ZoomModule::new, ModuleCategory.INTERFACE),
                withCategory(ParticlesModule::new, ModuleCategory.VISUALIZATION),
                withCategory(ProjectilePredictionModule::new, ModuleCategory.VISUALIZATION)
        });
    }

    public List<Supplier<Module>> playerModules() {
        return List.of(
                withCategory(ItemScrollerModule::new, ModuleCategory.CONVENIENCE),
                withCategory(DeathCoordinatesModule::new, ModuleCategory.CONVENIENCE),
                withCategory(AutoRespawnModule::new, ModuleCategory.AUTOMATION),
                withCategory(AutoResellModule::new, ModuleCategory.AUTOMATION),
                withCategory(AutoEatModule::new, ModuleCategory.AUTOMATION),
                withCategory(ChangeHandModule::new, ModuleCategory.CONVENIENCE),
                withCategory(FreeLookModule::new, ModuleCategory.CONVENIENCE)
        );
    }

    public List<Supplier<Module>> miscModules() {
        return List.of(
                withCategory(TPAcceptModule::new, ModuleCategory.AUTOMATION),
                withCategory(BetterChatModule::new, ModuleCategory.CONVENIENCE),
                withCategory(NameProtectModule::new, ModuleCategory.UTILITIES),
                withCategory(TapeMouseModule::new, ModuleCategory.UTILITIES),
                withCategory(ElytraHelperModule::new, ModuleCategory.UTILITIES),
                withCategory(FTHelperModule::new, ModuleCategory.UTILITIES),
                withCategory(RWHelperModule::new, ModuleCategory.UTILITIES),
                withCategory(HWHelperModule::new, ModuleCategory.UTILITIES),
                withCategory(SoundsModule::new, ModuleCategory.CONVENIENCE),
                withCategory(RWGriefJoinerModule::new, ModuleCategory.AUTOMATION),
                withCategory(HitSoundsModule::new, ModuleCategory.CONVENIENCE),
                withCategory(ClanInvestModule::new, ModuleCategory.AUTOMATION),
                withCategory(MineHelperModule::new, ModuleCategory.UTILITIES),
                withCategory(PvPSafeModule::new, ModuleCategory.UTILITIES),
                withCategory(OptimizerModule::new, ModuleCategory.UTILITIES)
                ,withCategory(ShiftTapModule::new, ModuleCategory.UTILITIES)
                ,withCategory(LockSlotModule::new, ModuleCategory.CONVENIENCE)
        );
    }

    public Supplier<Module> withCategory(Supplier<Module> supplier, ModuleCategory class672Var) {
        return () -> {
            Module class605Var = (Module) supplier.get();
            class605Var.setCategory(class672Var);
            return class605Var;
        };
    }
}
