package ru.spectra.client.command;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.model.CommandContext;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.FunTimeServerHandler;
import ru.spectra.client.event.HandledScreenRenderEvent;
import ru.spectra.client.util.HolyWorldServerHandler;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.ReallyWorldServerHandler;
import ru.spectra.client.net.ReconnectException;
import ru.spectra.client.util.ReconnectHandler;
import ru.spectra.client.net.ReconnectTask;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.model.TranslatedException;
import ru.spectra.client.model.Translation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ReconnectCommand implements ClientCommand {
    public ReconnectTask activeTask;
    public final List<ReconnectHandler> handlers = new ArrayList();

    public final Mc mc = Mc.INSTANCE;

    public ReconnectCommand() {
        this.handlers.add(new FunTimeServerHandler());
        this.handlers.add(new HolyWorldServerHandler());
        this.handlers.add(new ReallyWorldServerHandler());
        Spectra.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, class130Var -> {
            if (!isAvailable()) {
                this.activeTask = null;
                return;
            }
            if (this.activeTask != null) {
                try {
                    this.activeTask.tick();
                } catch (ReconnectException e) {
                    ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_ERROR_PREFIX.effective().replace("{error}", e.getMessage())).formatted(Formatting.RED));
                }
                if (this.activeTask.isComplete()) {
                    this.activeTask = null;
                }
            }
        });
        Spectra.INSTANCE.eventDispatcher().register(HandledScreenRenderEvent.class, class015Var -> {
            if (!isAvailable()) {
                this.activeTask = null;
                return;
            }
            if (this.activeTask != null) {
                try {
                    this.activeTask.handledScreenTick();
                } catch (ReconnectException e) {
                    ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_ERROR_PREFIX.effective().replace("{error}", e.getMessage())).formatted(Formatting.RED));
                }
            }
        });
    }

    @Override
    public void execute(CommandContext class392Var) throws TranslatedException {
        if (!this.mc.isWorldLoaded()) {
            throw new TranslatedException(Lang.COMMAND_WORLD_NOT_LOADED);
        }
        if (this.mc.isSingleplayer()) {
            throw new TranslatedException(Lang.COMMAND_SINGLEPLAYER_ONLY);
        }
        for (ReconnectHandler class036Var : this.handlers) {
            Iterator<String> it = class036Var.getServerIds().iterator();
            while (it.hasNext()) {
                if (ServerUtil.isConnectedToServer(it.next())) {
                    this.activeTask = class036Var.createProcess();
                    return;
                }
            }
        }
        throw new TranslatedException(Lang.COMMAND_UNSUPPORTED_SERVER);
    }

    @Override
    public String getName() {
        return "rct";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_RCT_DESC;
    }

    @Override
    public String getUsage() {
        return ".rct";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reconnect", "reconect");
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        return List.of();
    }
}
