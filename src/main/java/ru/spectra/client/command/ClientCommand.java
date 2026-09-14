package ru.spectra.client.command;
import ru.spectra.client.model.CommandContext;
import ru.spectra.client.model.Translation;

import java.util.List;

public interface ClientCommand {
    String getName();

    default String getFeatureId() {
        return switch (getName().toLowerCase(java.util.Locale.ROOT)) {
            case "macro" -> "macros";
            case "waypoint" -> "waypoints";
            case "friend" -> "friends";
            case "rct" -> "reconnect";
            case "bind" -> "binds";
            case "prefix" -> "commandprefix";
            default -> "command" + ru.spectra.client.net.HolyWorldFeaturePolicy.featureId(getName());
        };
    }

    default boolean isAvailable() {
        return ru.spectra.client.net.HolyWorldFeatureControl.allows(getFeatureId());
    }

    Translation getDescription();

    String getUsage();

    List<String> getAliases();

    void execute(CommandContext class392Var);

    List<String> getSuggestions(String[] strArr, int i);
}
