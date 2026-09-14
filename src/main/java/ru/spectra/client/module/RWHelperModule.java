package ru.spectra.client.module;
import ru.spectra.client.util.ActionScheduler;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.PacketSendEvent;
import ru.spectra.client.type.RwHelperAction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;

@Aliases(aliases = {"ReallyWorld Helper", "RW Helper"})
public class RWHelperModule extends Module {
    public final MultiSelectSetting<RwHelperAction> actions;
    public final Mc mc;
    public final Pattern bannedWordsPattern;
    public final ActionScheduler closeScheduler;

    public RWHelperModule() {
        super(ModuleTab.MISC, "RW Helper");
        this.actions = new MultiSelectSetting(Lang.RWHELPER_ACTIONS, Lang.RWHELPER_ACTIONS_DESC).values(RwHelperAction.class);
        this.mc = Mc.INSTANCE;
        this.closeScheduler = new ActionScheduler();
        this.bannedWordsPattern = Pattern.compile((String) new HashSet<String>(Arrays.asList("акриен(а|у|ом|е|чик)?", "рич(а|у|ом|ей|е)?", "ньюкод(ом|а|у|ами|ик|е)?", "экспенсив(ом|а|у|ами|е)?", "импакт(ом|а|у|ами|ик|е)?", "экселлент(ом|а|у|ами|ик|е)?", "экселент(ом|а|у|ами|ик)?", "катлаван(ом|а|у|ами|чик)?", "катлован(ом|а|у|ами|чик)?", "целестиал(ом|а|у|ами|е)?", "целк(ой|а|у|ами|очка|и|е)?", "матикс(ом|а|у|ами|е)?", "инерти(я|ей|ю|ями|е)?", "эксп(а|ой|ою|у|уличка|е)?", "флюгер(ом|а|у|ами)?", "рикер(а|у|ом|очек)?", "фанпе(й|ю|я|ем|е|йчик)?", "вексайд(ом|а|у|ами|ик|е)?", "нурсултан(а|у|е|ом|чик)?", "нурик(а|у|ом|е)?", "нурлан(а|у|ом|чик|е)?", "векс(ом|у|а|ами|ик|е)?", "релейк(ом|у|а|ами|е)?", "арбуз(ом|а|у|ами|ик|е|иком)?", "вилд(ом|у|а|ами|ик|е)?", "фантайм(е|а|у)?", "холик(е|а|у)?", "холиворлд(а|у|е)?", "рокстар(ом|а|у|ами|чик|е)?", "рогалик(а|у|ом|е)?", "тандерхак(ом|у|и|ами|а|е)?", "ликвидбаунс(а|у|ами|е)?", "spectra", "celestial", "newcode", "arbuz", "akrien", "nursultan", "relake", "wild", "wurst", "catlovan", "excellent", "rockstar", "catlavan", "impact", "matix", "inertia", "wex", "wexside", "nurik", "nurlan", "rich", "funpay", "fluger", "riker", "funtime", "holyworld", "wwe", "hvh", "rogalik", "thunderhack", "liquidbounce")).stream().map(str -> {
            if (str.contains(" ")) {
                return "(?i)" + str.replace(" ", "\\s+");
            }
            return str.contains("(") ? "(?i)(?<![\\p{L}])" + str + "(?![\\p{L}])" : "(?i)(?<![\\p{L}])" + Pattern.quote(str) + "(а|у|ом|ями|е|ей|ю|иком|очка|йчик|ою|чик|очек|ик|уличка|ой|и)?(?![\\p{L}])";
        }).collect(Collectors.joining("|")), 64);
        addSettings(this.actions);
        register(PacketSendEvent.class, class037Var -> {
            if (isState() && this.mc.isWorldLoaded() && this.actions.isSelected(RwHelperAction.FILTER_BANNED_WORDS)) {
                if ((class037Var.getPacket()) instanceof ChatMessageC2SPacket packet ) {
                    String strChatMessage = packet.chatMessage();
                    if (strChatMessage.startsWith("/") || strChatMessage.startsWith(".") || !this.bannedWordsPattern.matcher(strChatMessage).find()) {
                        return;
                    }
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.WARNING, (Text) Text.literal(String.valueOf(Formatting.GRAY) + Lang.RWHELPER_BANNED_MESSAGE.effective()), 3L, TimeUnit.SECONDS);
                    class037Var.cancel();
                }
            }
        });
        register(PacketReceiveEvent.class, class051Var -> {
            if (isState() && this.mc.isWorldLoaded()) {
                ClientPlayerEntity player = this.mc.getPlayer();
                if (this.actions.isSelected(RwHelperAction.CLOSE_SERVER_MENU)
                        && class051Var.getPacket() instanceof OpenScreenS2CPacket packet
                        && player != null && StringHelper.stripTextFormat(packet.getName().getString()).contains("ꈁꀀꈂꌁꈂꀁꈃꄀ") && player.age < 100) {
                    ActionScheduler class265Var = this.closeScheduler;
                    Objects.requireNonNull(player);
                    class265Var.addTickStep(1, player::closeHandledScreen);
                    class051Var.cancel();
                }
            }
        });
        register(ClientTickEvent.class, event -> {
            if (isState() && this.mc.isWorldLoaded()) {
                this.closeScheduler.update().cleanupIfFinished();
            } else {
                this.closeScheduler.cleanup();
            }
        });
    }

    @Override
    public void deactivate() {
        this.closeScheduler.cleanup();
        super.deactivate();
    }
}
