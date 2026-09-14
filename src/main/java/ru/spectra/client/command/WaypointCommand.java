package ru.spectra.client.command;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.CommandContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.ProjectionUtil;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.model.TranslatedException;
import ru.spectra.client.model.Translation;
import ru.spectra.client.config.Waypoint;
import ru.spectra.client.config.WaysRepository;
import ru.spectra.client.ui.WorldMarkerRenderer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector3i;

public class WaypointCommand implements ClientCommand {
    public WaypointCommand() {
        Spectra.INSTANCE.eventDispatcher().register(Render2DEvent.class, class311Var -> {
            if (!isAvailable()) {
                return;
            }
            WaysRepository class675VarWaysRepository = Spectra.INSTANCE.waysRepository();
            if (class675VarWaysRepository.isEmpty() || class311Var.isPost()) {
                return;
            }
            Mc class815Var = Mc.INSTANCE;
            MatrixStack matrixStack = class311Var.matrixStack();
            DrawEngine class154VarDrawEngine = Spectra.INSTANCE.drawEngine();
            ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
            ThemePalette class764VarPalette = Spectra.INSTANCE.theme().palette();
            class154VarDrawEngine.begin();
            for (Waypoint class674Var : class675VarWaysRepository.getWaypoints()) {
                Vector3i vector3iVector = class674Var.vector();
                Vec3d vec3d = new Vec3d(((double) vector3iVector.x()) + 0.5d, ((double) vector3iVector.y()) + 0.5d, ((double) vector3iVector.z()) + 0.5d);
                Optional<Vector2f> optionalWorldToScreen = ProjectionUtil.worldToScreen(vec3d);
                if (optionalWorldToScreen.isPresent()) {
                    WorldMarkerRenderer.renderWaypoint(
                            class154VarDrawEngine, matrixStack, optionalWorldToScreen.get(),
                            class674Var.name(), vec3d
                    );
                }
            }
            class154VarDrawEngine.end();
        });
    }

    @Override
    public String getName() {
        return "waypoint";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_WAY_DESC;
    }

    @Override
    public String getUsage() {
        return ".waypoint <add | remove | list | clear> [args]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("way", "waypoint");
    }

    @Override
    public void execute(CommandContext class392Var) throws TranslatedException {
        String[] strArrArgs = class392Var.args();
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        switch (strArrArgs[0].toLowerCase()) {
            case "add":
                addWaypoint(strArrArgs);
                return;
            case "remove":
                removeWaypoint(strArrArgs);
                return;
            case "list":
                listWaypoints();
                return;
            case "clear":
                clearWaypoints();
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", strArrArgs[0])));
        }
    }

    public void addWaypoint(String[] strArr) throws TranslatedException {
        WaysRepository class675VarWaysRepository = Spectra.INSTANCE.waysRepository();
        if (strArr.length != 5) {
            throw new TranslatedException(Lang.COMMAND_INVALID_ARG_COUNT);
        }
        String str = strArr[1];
        try {
            int i = Integer.parseInt(strArr[2]);
            int i2 = Integer.parseInt(strArr[3]);
            int i3 = Integer.parseInt(strArr[4]);
            class675VarWaysRepository.getWaypoints().removeIf(class674Var -> {
                return class674Var.name().equalsIgnoreCase(str) || class674Var.vector().equals(new Vector3i(i, i2, i3));
            });
            class675VarWaysRepository.add(str, new Vector3i(i, i2, i3));
            ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_WAYPOINT_ADDED.effective().replace("{name}", str)).formatted(Formatting.GRAY));
        } catch (NumberFormatException e) {
            throw new TranslatedException(Lang.COMMAND_WAY_INVALID_COORDS);
        }
    }

    public void removeWaypoint(String[] strArr) throws TranslatedException {
        WaysRepository class675VarWaysRepository = Spectra.INSTANCE.waysRepository();
        if (strArr.length != 2) {
            throw new TranslatedException(Lang.COMMAND_INVALID_ARG_COUNT);
        }
        String str = strArr[1];
        if (!class675VarWaysRepository.remove(str)) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_WAYPOINT_NOT_FOUND.effective().replace("{name}", str)));
        }
        ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_WAYPOINT_REMOVED.effective().replace("{name}", str)).formatted(Formatting.DARK_GRAY));
    }

    public void listWaypoints() {
        WaysRepository class675VarWaysRepository = Spectra.INSTANCE.waysRepository();
        if (class675VarWaysRepository.isEmpty()) {
            ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_WAYPOINTS_NOT_FOUND.effective()).formatted(Formatting.DARK_GRAY));
            return;
        }
        ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_WAYPOINTS_LIST.effective()).formatted(Formatting.DARK_GRAY));
        for (Waypoint class674Var : class675VarWaysRepository.getWaypoints()) {
            ChatUtil.addChatMessage((Text) Text.literal("- ").formatted(Formatting.DARK_GRAY).append(Text.literal(class674Var.name()).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ".waypoint remove " + class674Var.name())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(Lang.COMMAND_WAYPOINT_COORDS.effective()).append(Text.literal("[" + class674Var.vector().x + ", " + class674Var.vector().y + ", " + class674Var.vector().z + "]").formatted(Formatting.RED)))))));
        }
    }

    public void clearWaypoints() {
        Spectra.INSTANCE.waysRepository().clearList();
        ChatUtil.addChatMessage((Text) Text.literal(Lang.COMMAND_WAYPOINTS_CLEARED.effective()).formatted(Formatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        if (i == 0) {
            return List.of("add", "remove", "list", "clear");
        }
        String lowerCase = strArr[0].toLowerCase();
        String str = i < strArr.length ? strArr[i] : "";
        if (i == 1 && lowerCase.equals("remove")) {
            return (List) Spectra.INSTANCE.waysRepository().getWaypoints().stream().map((v0) -> {
                return v0.name();
            }).filter(str2 -> {
                return str2.toLowerCase().startsWith(str.toLowerCase());
            }).collect(Collectors.toList());
        }
        if (!lowerCase.equals("add")) {
            return List.of();
        }
        switch (i) {
            case 1:
                return List.of("<name>");
            case 2:
                return List.of("<x>");
            case 3:
                return List.of("<y>");
            case 4:
                return List.of("<z>");
            default:
                return List.of();
        }
    }
}
