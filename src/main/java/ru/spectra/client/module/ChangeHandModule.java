package ru.spectra.client.module;

import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.KeybindSetting;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.Arm;

@Aliases(aliases = {"Change Hand", "Switch Hand", "Main Hand", "Leading Hand"})
public final class ChangeHandModule extends Module {
    public final KeybindSetting changeKey = new KeybindSetting(
            Translation.clearText("Change hand key"),
            Translation.clearText("Switches the leading hand between right and left")
    );

    public ChangeHandModule() {
        super(ModuleTab.PLAYER, "Change Hand");
        addSettings(this.changeKey);
        this.changeKey.consumer(ignored -> changeHand());
    }

    private void changeHand() {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        GameOptions options = Mc.INSTANCE.getGameOptions();
        SimpleOption<Arm> mainArm = options.getMainArm();
        mainArm.setValue(mainArm.getValue() == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT);
        options.write();
        options.sendClientSettings();
    }
}
