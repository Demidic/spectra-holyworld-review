package ru.spectra.client.util;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.module.Module;
import ru.spectra.client.model.TranslatedException;
import ru.spectra.client.model.Translation;

import java.util.List;
import java.util.stream.Collectors;

public class ModuleArgumentParser implements ArgumentParser<Module> {
    @Override
    public Module parse(String str) {
        return Spectra.INSTANCE.moduleRepository().getModules().stream().filter(class605Var -> {
            return class605Var.isVisibleInMenu()
                    && class605Var.getName().replaceAll("\\s", "").equalsIgnoreCase(str);
        }).findFirst().orElseThrow(() -> {
            return new TranslatedException(Translation.clearText(Lang.TYPE_MODULE_NOT_FOUND.effective().replace("{input}", str)));
        });
    }

    @Override
    public List<String> getSuggestions(String str) {
        String lowerCase = str.toLowerCase();
        return (List) Spectra.INSTANCE.moduleRepository().getModules().stream()
                .filter(Module::isVisibleInMenu).map(class605Var -> {
            return class605Var.getName().replaceAll("\\s", "");
        }).filter(str2 -> {
            return str2.replaceAll("\\s", "").toLowerCase().startsWith(lowerCase);
        }).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "module";
    }
}
