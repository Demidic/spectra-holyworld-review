package ru.spectra.client.config;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.model.ConfigErrorNotice;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.KeyInputEvent;
import ru.spectra.client.type.KeyPressState;
import ru.spectra.client.model.MacroEntry;
import ru.spectra.client.type.Mc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroRepository {
    public final List<MacroEntry> macroList = new ArrayList();
    public static final Logger logger = LoggerFactory.getLogger(MacroRepository.class);

    public MacroRepository() {
        Spectra.INSTANCE.eventDispatcher().register(KeyInputEvent.class, class049Var -> {
            if (!ru.spectra.client.net.HolyWorldFeatureControl.allows("macros")) {
                return;
            }
            if (Mc.INSTANCE.isWorldLoaded() && !this.macroList.isEmpty() && class049Var.action() == KeyPressState.PRESS) {
                int iKey = class049Var.key();
                this.macroList.stream().filter(class724Var -> {
                    return class724Var.key() == iKey;
                }).findFirst().ifPresent(class724Var2 -> {
                    try {
                        if (class724Var2.content().startsWith("/")) {
                            Mc.INSTANCE.getNetworkHandler().sendChatCommand(class724Var2.content().substring(1));
                        } else {
                            Mc.INSTANCE.getNetworkHandler().sendChatMessage(class724Var2.content());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to send macro command {}", class724Var2.name(), e);
                        ChatUtil.addChatMessage("Ошибка при отправке команды. См. latest.log");
                    }
                });
            }
        });
    }

    public boolean isEmpty() {
        return this.macroList.isEmpty();
    }

    public void addMacro(String str, String str2, int i) {
        this.macroList.add(new MacroEntry(str, str2, i));
        try {
            Spectra.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after addMacro(name={})", str, e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении макросов"));
        }
    }

    public boolean hasMacro(String str) {
        Iterator<MacroEntry> it = this.macroList.iterator();
        while (it.hasNext()) {
            if (it.next().name().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public void deleteMacro(String str) {
        if (this.macroList.stream().anyMatch(class724Var -> {
            return class724Var.name().equals(str);
        })) {
            this.macroList.removeIf(class724Var2 -> {
                return class724Var2.name().equalsIgnoreCase(str);
            });
            try {
                Spectra.INSTANCE.configManager().saveMacros();
            } catch (IOException e) {
                logger.error("Failed to persist macros after deleteMacro(name={})", str, e);
                ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("удалении макроса"));
            }
        }
    }

    public void clearList() {
        if (this.macroList.isEmpty()) {
            return;
        }
        this.macroList.clear();
        try {
            Spectra.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after clearList()", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("очистке макросов"));
        }
    }

    public List<MacroEntry> getMacroList() {
        return this.macroList;
    }
}
