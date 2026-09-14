package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.type.TapeMouseHand;
import ru.spectra.client.util.PvPModeDetector;

import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

@Aliases(aliases = {"Tape Mouse", "Auto Click", "Click Assist", "Clicker", "Mouse Automation", "Auto Tap", "Hand Clicker", "Auto Hand Use"})
public class TapeMouseModule extends Module {
    public final ModeSetting<TapeMouseHand> handMode;
    public final NumberSetting delay;
    public final Stopwatch stopwatch;
    public boolean usePressed;

    public TapeMouseModule() {
        super(ModuleTab.MISC, "Tape Mouse");
        this.handMode = new ModeSetting(Lang.TAPEMOUSE_HANDMODE).values(TapeMouseHand.class);
        this.delay = new NumberSetting(Lang.TAPEMOUSE_DELAY).currentValue(1.0f).range(0.0f, 30.0f).step(1.0f).unit(SettingUnit.SECONDS);
        this.stopwatch = new Stopwatch(false);
        this.usePressed = false;
        addSettings(this.handMode, this.delay);
        register(PlayerTickEvent.class, class130Var -> {
            Mc class815Var = Mc.INSTANCE;
            if (isState() && class815Var.isWorldLoaded() && class130Var.isPre()) {
                GameOptions gameOptions = class815Var.getGameOptions();
                if (PvPModeDetector.isPvPMode()) {
                    gameOptions.attackKey.setPressed(false);
                    gameOptions.useKey.setPressed(false);
                    this.usePressed = false;
                    this.stopwatch.reset();
                    setState(false);
                    return;
                }
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                if (this.handMode.isSelected(TapeMouseHand.LEFT)) {
                    if (this.stopwatch.hasElapsed((long) this.delay.currentValue(), TimeUnit.SECONDS)) {
                        minecraftClient.doAttack();
                        this.stopwatch.reset();
                    }
                } else if (this.stopwatch.hasElapsed((long) this.delay.currentValue(), TimeUnit.SECONDS)) {
                    gameOptions.useKey.setPressed(true);
                    this.usePressed = true;
                    this.stopwatch.reset();
                }
                if (this.usePressed) {
                    gameOptions.useKey.setPressed(false);
                    this.usePressed = false;
                }
            }
        });
    }

    @Override
    public void deactivate() {
        Mc class815Var = Mc.INSTANCE;
        if (class815Var.isWorldLoaded() && this.usePressed) {
            class815Var.getGameOptions().useKey.setPressed(false);
            this.usePressed = false;
        }
        super.deactivate();
    }
}
