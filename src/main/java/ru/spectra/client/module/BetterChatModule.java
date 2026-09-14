package ru.spectra.client.module;
import ru.spectra.client.Spectra;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.type.BetterChatOption;
import ru.spectra.client.event.ChatLimitEvent;
import ru.spectra.client.type.ChatLimitType;
import ru.spectra.client.type.ChatMessageType;
import ru.spectra.client.event.ChatReceiveEvent;
import ru.spectra.client.model.ChatStackEntry;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.math.TimestampedCounter;
import ru.spectra.client.model.Translation;
import ru.spectra.client.event.PacketSendEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

@Aliases(aliases = {"Better Chat", "Chat History", "Anti Spam", "Infinite Chat"})
public class BetterChatModule extends Module {
    public static final String externalSuffix = "-external";
    public final MultiSelectSetting<BetterChatOption> chatImprovements;
    public final Map<String, TimestampedCounter> messageCounters;
    public final long spamWindowMs = 10000;
    private boolean resendingAhCommand;

    public BetterChatModule() {
        super(ModuleTab.MISC, "Better Chat");
        this.chatImprovements = new MultiSelectSetting<BetterChatOption>(Lang.BETTERCHAT_CHAT_IMPROVEMENTS)
                .values(BetterChatOption.class)
                .select(
                        BetterChatOption.SHOW_MESSAGE_TIME,
                        BetterChatOption.AH_HELPER,
                        BetterChatOption.FIX_COMMAND_LAYOUT
                );
        this.messageCounters = new HashMap();
        addSettings(this.chatImprovements);
        register(ChatReceiveEvent.class, this::onChatReceive);
        register(ChatLimitEvent.class, this::onChatLimit);
        register(PacketSendEvent.class, this::onPacketSend);
    }

    public void onChatReceive(ChatReceiveEvent class066Var) {
        Mc class815Var = Mc.INSTANCE;
        boolean antiSpam = this.chatImprovements.isSelected(BetterChatOption.ANTI_SPAM);
        if (isState() && class815Var.isWorldLoaded()
                && class066Var.type() != ChatMessageType.DISGUISED_CHAT_MESSAGE
                && (antiSpam || this.chatImprovements.isSelected(BetterChatOption.SHOW_MESSAGE_TIME))) {
            InGameHud inGameHud = class815Var.getInGameHud();
            class066Var.cancel();
            String str = TextVisitFactory.removeFormattingCodes(class066Var.textData()) + "-external";
            ChatHud chatHud = inGameHud.getChatHud();
            long jCurrentTimeMillis = System.currentTimeMillis();
            int i = 1;
            if (antiSpam) {
                pruneExpiredCounters(jCurrentTimeMillis);
                incrementCounter(str, jCurrentTimeMillis);
                i = this.messageCounters.get(str).count;
            }
            MutableText mutableTextLiteral = Text.literal("");
            if (this.chatImprovements.isSelected(BetterChatOption.SHOW_MESSAGE_TIME)) {
                mutableTextLiteral.append(Text.literal(
                        "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] "
                ).formatted(Formatting.DARK_GRAY));
            }
            mutableTextLiteral.append(class066Var.applyChatDecoration().apply(class066Var.textData()));
            if (antiSpam && i > 1) {
                mutableTextLiteral.append(Text.literal(" ").formatted(Formatting.GRAY).append("[x" + i + "]"));
            }
            ChatStackEntry class085Var = new ChatStackEntry(false, str, antiSpam, i);
            if (class085Var.remove() && StringUtils.isNotEmpty(class085Var.id())) {
                ChatUtil.removeMessage(chatHud, class085Var.id());
            }
            ChatUtil.addMessage(chatHud, mutableTextLiteral, class085Var.id());
        }
    }

    private void onPacketSend(PacketSendEvent event) {
        if (!isState() || !this.chatImprovements.isSelected(BetterChatOption.AH_HELPER) || resendingAhCommand
                || !Mc.INSTANCE.isWorldLoaded()
                || !(event.getPacket() instanceof CommandExecutionC2SPacket packet)) {
            return;
        }
        String command = packet.command().trim();
        if (!command.equalsIgnoreCase("ah search")) {
            return;
        }
        var held = Mc.INSTANCE.getPlayer().getMainHandStack();
        if (held.isEmpty()) {
            Spectra.INSTANCE.notificationRepository().post(
                    ru.spectra.client.type.NotificationType.WARNING,
                    Text.literal(ClientLocalization.text(
                            "Hold an item to search the auction",
                            "Возьмите предмет в руку для поиска на аукционе")),
                    3L, java.util.concurrent.TimeUnit.SECONDS);
            return;
        }
        event.cancel();
        String itemName = StringHelper.stripTextFormat(held.getName().getString());
        resendingAhCommand = true;
        try {
            Mc.INSTANCE.getNetworkHandler().sendChatCommand("ah search " + itemName);
        } finally {
            resendingAhCommand = false;
        }
    }

    public void pruneExpiredCounters(long j) {
        this.messageCounters.entrySet().removeIf(entry -> {
            return j - ((TimestampedCounter) entry.getValue()).timestamp > 10000;
        });
    }

    public void incrementCounter(String str, long j) {
        this.messageCounters.compute(str, (str2, class482Var) -> {
            if (class482Var == null) {
                return new TimestampedCounter(1, j);
            }
            class482Var.count++;
            class482Var.timestamp = j;
            return class482Var;
        });
    }

    public void onChatLimit(ChatLimitEvent class056Var) {
        if (isState() && Mc.INSTANCE.isWorldLoaded()) {
            ChatLimitType type = class056Var.getType();
            if ((type.isHistory() && this.chatImprovements.isSelected(BetterChatOption.ANTI_CLEAR)) || (type.isLimit() && this.chatImprovements.isSelected(BetterChatOption.INFINITY))) {
                class056Var.cancel();
            }
        }
    }

    /**
     * Converts only the command name from the Russian keyboard layout to the
     * matching QWERTY keys. Arguments are intentionally preserved.
     */
    public String fixCommandLayout(String command) {
        if (!isState()
                || !this.chatImprovements.isSelected(BetterChatOption.FIX_COMMAND_LAYOUT)
                || command == null
                || command.isEmpty()) {
            return command;
        }
        int argumentStart = command.indexOf(' ');
        String name = argumentStart < 0 ? command : command.substring(0, argumentStart);
        String arguments = argumentStart < 0 ? "" : command.substring(argumentStart);
        StringBuilder translated = new StringBuilder(name.length());
        boolean changed = false;
        for (int i = 0; i < name.length(); i++) {
            char source = name.charAt(i);
            char replacement = translateRussianKey(source);
            translated.append(replacement);
            changed |= replacement != source;
        }
        return changed ? translated.append(arguments).toString() : command;
    }

    private static char translateRussianKey(char value) {
        String russian = "ёйцукенгшщзхъфывапролджэячсмитьбю"
                + "ЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
        String english = "`qwertyuiop[]asdfghjkl;'zxcvbnm,."
                + "~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>";
        int index = russian.indexOf(value);
        return index < 0 ? value : english.charAt(index);
    }
}
