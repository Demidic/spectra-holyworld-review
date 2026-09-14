package ru.spectra.client.model;
import ru.spectra.client.module.Module;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.type.ModuleCategory;

import java.util.ArrayList;
import java.util.List;

public class HotkeyEntry {
    public final String name;
    public final String moduleName;
    public final ModuleCategory category;
    public final List<Integer> keys;
    public final ToggleAnimator anim;

    public HotkeyEntry(String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = ToggleAnimator.times(2, 80);
        this.moduleName = null;
        this.category = null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public HotkeyEntry(Module class605Var, String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = ToggleAnimator.times(2, 80);
        this.moduleName = class605Var != null ? class605Var.getName() : null;
        this.category = class605Var != null ? class605Var.getCategory() : null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public boolean hasModule() {
        return this.moduleName != null;
    }

    public String name() {
        return this.name;
    }

    public String moduleName() {
        return this.moduleName;
    }

    public List<Integer> keys() {
        return this.keys;
    }

    public ToggleAnimator anim() {
        return this.anim;
    }
}
