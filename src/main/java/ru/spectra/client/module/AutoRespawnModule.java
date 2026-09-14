package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.DeathTickEvent;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.type.SettingUnit;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;

@Aliases(aliases = {"Auto Respawn", "Automatic Respawn", "Fast Respawn", "Quick Respawn", "Auto Revive"})
public class AutoRespawnModule extends Module {
    public final NumberSetting respawnDelay;

    public AutoRespawnModule() {
        super(ModuleTab.PLAYER, "AutoRespawn");
        this.respawnDelay = new NumberSetting(Lang.AUTORESPAWN_DELAY).currentValue(20.0f).range(0.0f, 70.0f).step(1.0f).unit(SettingUnit.TICKS);
        addSettings(this.respawnDelay);
        register(DeathTickEvent.class, class276Var -> {
            Mc class815Var;
            ClientPlayerEntity player;
            if (!isState() || class276Var.ticksSinceDeath() <= Math.round(this.respawnDelay.currentValue()) || (player = (class815Var = Mc.INSTANCE).getPlayer()) == null) {
                return;
            }
            player.requestRespawn();
            class815Var.getMinecraft().setScreen((Screen) null);
        });
    }
}
