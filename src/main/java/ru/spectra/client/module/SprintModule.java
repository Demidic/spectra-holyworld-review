package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.event.MovementUpdateEvent;
import ru.spectra.client.event.MovementUpdateSource;

import net.minecraft.entity.effect.StatusEffects;

@Aliases(aliases = {"Auto Sprint", "Sprint", "Auto Run", "Always Sprint", "Automatic Sprint", "Toggle Sprint"})
public class SprintModule extends Module {
    private final Mc mc;

    public SprintModule() {
        super(ModuleTab.MOVEMENT, "Auto Sprint");
        this.mc = Mc.INSTANCE;
        register(MovementUpdateEvent.class, class308Var -> {
            if (isState() && this.mc.isWorldLoaded() && class308Var.getDirectionalInput().isMoving()) {
                boolean movementTick = class308Var.getSource() == MovementUpdateSource.MOVEMENT_TICK;
                if (hasEnoughFood() && !hasBlindness()
                        && (class308Var.getSource() == MovementUpdateSource.INPUT || movementTick)) {
                    class308Var.setSprint(true);
                }
            }
        }, EventPriority.LOW);
    }

    public boolean hasEnoughFood() {
        return this.mc.getPlayer().getHungerManager().getFoodLevel() > 6;
    }

    public boolean hasBlindness() {
        return this.mc.getPlayer().hasStatusEffect(StatusEffects.BLINDNESS);
    }
}
