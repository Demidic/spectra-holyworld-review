package ru.spectra.client.module;

import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoResellModule extends Module {
    public final ModeSetting<Mode> mode =
            new ModeSetting<Mode>(Translation.clearText("Mode")).values(Mode.class);
    public final BooleanSetting randomDelay =
            new BooleanSetting(Translation.clearText("Random delay")).setValue(true);
    private int stage;
    private long cycleAt;
    private long actionAt;

    public AutoResellModule() {
        super(ModuleTab.PLAYER, "Auto Resell");
        addSettings(mode, randomDelay);
        register(PlayerTickEvent.class, event -> {
            if (event.isPre() && isState() && Mc.INSTANCE.isWorldLoaded()) {
                tick();
            }
        });
        resetCycle();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (mode.isSelected(Mode.COMMAND)) {
            if (now >= cycleAt) {
                Mc.INSTANCE.getNetworkHandler().sendChatCommand("ah resell");
                resetCycle();
            }
            return;
        }
        switch (stage) {
            case 0 -> {
                if (now >= cycleAt) {
                    Mc.INSTANCE.getNetworkHandler().sendChatCommand("ah");
                    stage = 1;
                    actionAt = now + 500L;
                }
            }
            case 1 -> {
                if (now >= actionAt && clickItem(Items.ENDER_CHEST)) {
                    stage = 2;
                    actionAt = now + 500L;
                } else if (now > actionAt + 4_500L) {
                    resetCycle();
                }
            }
            case 2 -> {
                if (now >= actionAt && clickItem(Items.CLOCK)) {
                    stage = 3;
                    actionAt = now + 300L;
                } else if (now > actionAt + 4_500L) {
                    resetCycle();
                }
            }
            case 3 -> {
                if (now >= actionAt) {
                    Mc.INSTANCE.getPlayer().closeHandledScreen();
                    resetCycle();
                }
            }
            default -> resetCycle();
        }
    }

    private boolean clickItem(Item item) {
        if (!(Mc.INSTANCE.getCurrentScreen() instanceof HandledScreen<?> screen)) {
            return false;
        }
        for (var slot : screen.getScreenHandler().slots) {
            if (slot.hasStack() && slot.getStack().isOf(item)) {
                Mc.INSTANCE.getInteractionManager().clickSlot(
                        screen.getScreenHandler().syncId, slot.id, 0,
                        SlotActionType.PICKUP, Mc.INSTANCE.getPlayer());
                return true;
            }
        }
        return false;
    }

    private void resetCycle() {
        stage = 0;
        long delay = randomDelay.isValue()
                ? ThreadLocalRandom.current().nextLong(61_001L, 62_001L)
                : 61_000L;
        cycleAt = System.currentTimeMillis() + delay;
    }

    @Override
    public void activate() {
        resetCycle();
        super.activate();
    }

    @Override
    public void deactivate() {
        resetCycle();
        super.deactivate();
    }

    public enum Mode implements DisplayNamed {
        STANDARD("Standard"),
        COMMAND("Command");

        private final Translation name;

        Mode(String name) {
            this.name = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
