package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PlayerInitEvent;
import ru.spectra.client.event.PlayerTickEvent;

import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

@Aliases(aliases = {"Death Coords", "Death Coordinates", "Point Death"})
public class DeathCoordinatesModule extends Module {
    public static final String deathCoordsFormat = "%s: " + String.valueOf(Formatting.RED) + "X: " + String.valueOf(Formatting.RESET) + "%d" + String.valueOf(Formatting.RED) + " Y: " + String.valueOf(Formatting.RESET) + "%d" + String.valueOf(Formatting.RED) + " Z: " + String.valueOf(Formatting.RESET) + "%d.";
    public boolean alreadyPosted;

    public DeathCoordinatesModule() {
        super(ModuleTab.PLAYER, "Death Coordinates");
        register(PlayerInitEvent.class, class125Var -> {
            this.alreadyPosted = false;
        });
        register(PlayerTickEvent.class, class130Var -> {
            if (isState() && class130Var.isPre()) {
                Mc class815Var = Mc.INSTANCE;
                ClientPlayerEntity player = class815Var.getPlayer();
                if (!(class815Var.getCurrentScreen() instanceof DeathScreen) || this.alreadyPosted) {
                    return;
                }
                BlockPos blockPos = player.getBlockPos();
                String str = deathCoordsFormat.formatted(Lang.DEATHCOORDINATES_DEATH_TEXT.effective(), Integer.valueOf(blockPos.getX()), Integer.valueOf(blockPos.getY()), Integer.valueOf(blockPos.getZ()));
                Spectra.INSTANCE.notificationRepository().post(NotificationType.INFO, (Text) Text.literal(str), 5L, TimeUnit.SECONDS);
                ChatUtil.addChatMessage(str);
                this.alreadyPosted = true;
            }
        });
    }
}
