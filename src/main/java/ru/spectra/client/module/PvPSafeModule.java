package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PacketSendEvent;
import ru.spectra.client.util.PvPModeDetector;
import ru.spectra.client.model.Translation;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;

@Aliases(aliases = {"PvP Safe", "Command Blocker", "Safe Commands", "Anti Hub", "Block Commands", "Command Guard"})
public class PvPSafeModule extends Module {
    public final BooleanSetting blockHub;
    public final BooleanSetting blockAnarchy;
    public final BooleanSetting blockDisconnect;
    static final Pattern hubPattern = Pattern.compile("^(hub|lobby|leave|quit|exit)(\\s.*)?$", 2);
    static final Pattern anarchyPattern = Pattern.compile("^an(archy)?(\\s*\\d+)(\\s.*)?$", 2);
    private String pendingCommand;
    private long pendingCommandAt;

    public PvPSafeModule() {
        super(ModuleTab.MISC, "PvP Safe");
        this.blockHub = new BooleanSetting(Lang.PVPSAFE_BLOCK_HUB, Lang.PVPSAFE_BLOCK_HUB_DESC).setValue(true);
        this.blockAnarchy = new BooleanSetting(Lang.PVPSAFE_BLOCK_ANARCHY, Lang.PVPSAFE_BLOCK_ANARCHY_DESC).setValue(true);
        this.blockDisconnect = new BooleanSetting(Translation.clearText("Block disconnect")).setValue(true);
        addSettings(this.blockHub, this.blockAnarchy, this.blockDisconnect);
        register(PacketSendEvent.class, class037Var -> {
            if (isState()) {
                if ((class037Var.getPacket()) instanceof CommandExecutionC2SPacket packet ) {
                    try {
                        String strTrim = packet.command().trim();
                        if (isBlockedCommand(strTrim) && PvPModeDetector.isPvPMode()) {
                            long now = System.currentTimeMillis();
                            if (strTrim.equalsIgnoreCase(pendingCommand)
                                    && now - pendingCommandAt <= 10_000L) {
                                pendingCommand = null;
                                pendingCommandAt = 0L;
                                return;
                            }
                            class037Var.cancel();
                            pendingCommand = strTrim;
                            pendingCommandAt = now;
                            Spectra.INSTANCE.notificationRepository().post(NotificationType.WARNING, (Text) Text.literal(Lang.PVPSAFE_BLOCKED_NOTIFICATION.effective().replace("{cmd}", "/" + strTrim.split("\\s+")[0])), 4L, TimeUnit.SECONDS);
                        }
                    } catch (Throwable th) {
                        throw new MatchException(th.toString(), th);
                    }
                }
            }
        });
    }

    public boolean isBlockedCommand(String str) {
        if (this.blockHub.isValue() && hubPattern.matcher(str).matches()) {
            return true;
        }
        return this.blockAnarchy.isValue() && anarchyPattern.matcher(str).matches();
    }

    public static boolean shouldConfirmDisconnect() {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null) {
            return false;
        }
        return Spectra.INSTANCE.moduleRepository().find(PvPSafeModule.class)
                .map(module -> module.isState()
                        && module.blockDisconnect.isValue()
                        && PvPModeDetector.isPvPMode())
                .orElse(false);
    }

    @Override
    public void deactivate() {
        pendingCommand = null;
        pendingCommandAt = 0L;
        super.deactivate();
    }
}
