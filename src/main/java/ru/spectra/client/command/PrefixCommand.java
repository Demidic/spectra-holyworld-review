package ru.spectra.client.command;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.model.CommandContext;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.model.TranslatedException;
import ru.spectra.client.model.Translation;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PrefixCommand implements ClientCommand {
    @Override
    public String getName() {
        return "prefix";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_PREFIX_DESC;
    }

    @Override
    public String getUsage() {
        return Spectra.INSTANCE.commandDispatcher().getPrefix() + "prefix <символ>";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public void execute(CommandContext class392Var) throws TranslatedException {
        String[] strArrArgs = class392Var.args();
        if (strArrArgs.length == 0) {
            ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_PREFIX_CURRENT.effective().replace("{prefix}", String.valueOf(Formatting.RED) + Spectra.INSTANCE.commandDispatcher().getPrefix() + String.valueOf(Formatting.GRAY))).formatted(Formatting.GRAY));
            return;
        }
        String str = strArrArgs[0];
        if (str.isBlank() || str.length() > 1) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_PREFIX_INVALID.effective()));
        }
        String prefix = Spectra.INSTANCE.commandDispatcher().getPrefix();
        Spectra.INSTANCE.commandDispatcher().setPrefix(str);
        ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_PREFIX_SET.effective().replace("{old}", String.valueOf(Formatting.RED) + prefix + String.valueOf(Formatting.GRAY)).replace("{new}", String.valueOf(Formatting.RED) + str + String.valueOf(Formatting.GRAY))).formatted(Formatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        return List.of();
    }
}
