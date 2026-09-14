package ru.spectra.client.module;

import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import net.minecraft.entity.LivingEntity;

public final class ShiftTapModule extends Module {
    public final ModeSetting<AttackMode> attackMode =
            new ModeSetting<AttackMode>(Translation.clearText("Attack mode")).values(AttackMode.class);
    public final NumberSetting shiftDuration = new NumberSetting(Translation.clearText("Shift duration"))
            .currentValue(150.0f).range(10.0f, 500.0f).step(10.0f).unit(SettingUnit.MILLISECONDS);
    private long sneakUntil;

    public ShiftTapModule() {
        super(ModuleTab.MISC, "Shift Tap");
        addSettings(attackMode, shiftDuration);
        register(AttackEntityEvent.class, event -> {
            if (!isState() || !(event.attacker() instanceof LivingEntity)
                    || event.attacker() == Mc.INSTANCE.getPlayer()) {
                return;
            }
            if (attackMode.isSelected(AttackMode.AIRBORNE)
                    && Mc.INSTANCE.getPlayer().isOnGround()) {
                return;
            }
            sneakUntil = System.currentTimeMillis() + Math.round(shiftDuration.currentValue());
        });
        register(MovementInputEvent.class, event -> {
            if (isState() && System.currentTimeMillis() < sneakUntil) {
                event.setSneaking(true);
            }
        });
    }

    @Override
    public void deactivate() {
        sneakUntil = 0L;
        super.deactivate();
    }

    public enum AttackMode implements DisplayNamed {
        ALL("All attacks"),
        AIRBORNE("Only airborne");

        private final Translation name;

        AttackMode(String name) {
            this.name = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
