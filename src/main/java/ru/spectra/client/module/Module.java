package ru.spectra.client.module;
import ru.spectra.client.type.BindMode;
import ru.spectra.client.event.Event;
import ru.spectra.client.event.EventCallback;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.ModuleCategory;
import ru.spectra.client.event.ModuleStateEvent;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.SettingHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Module extends SettingHolder {
    public static final int KEY_UNBOUND = -1;
    public final ModuleTab moduleTab;

    public ModuleCategory category;
    public final String visibleName;
    public final String name;
    public BindMode type;
    public final List<Integer> boundKeys;
    public boolean state;
    private ServerAccessPolicy serverAccessPolicy = ServerAccessPolicy.allowAll();

    public Module(ModuleTab class847Var, String str) {
        this(class847Var, null, str);
    }

    public Module(ModuleTab class847Var, ModuleCategory class672Var, String str) {
        this.type = BindMode.TOGGLE;
        this.boundKeys = new ArrayList();
        this.moduleTab = class847Var;
        this.visibleName = str;
        this.name = str;
        if (class672Var != null) {
            setCategory(class672Var);
        }
    }

    public void switchState() {
        setState(!this.state);
    }

    public void setState(boolean z) {
        if (z && !isAvailable()) {
            return;
        }
        if (z != this.state) {
            this.state = z;
            applyState();
        }
    }

    public Module setStateSilent(boolean z) {
        if (z && !isAvailable()) {
            z = false;
        }
        if (z != this.state) {
            this.state = z;
        }
        return this;
    }

    public void setCategory(ModuleCategory class672Var) {
        if (class672Var != null && !class672Var.supports(this.moduleTab)) {
            throw new IllegalArgumentException("Category %s is not supported for tab %s".formatted(class672Var.name(), this.moduleTab.name()));
        }
        this.category = class672Var;
    }

    public void setBind(int i, BindMode class660Var) {
        setBind(i == -1 ? List.of() : List.of(Integer.valueOf(i)), class660Var);
    }

    public void setBind(List<Integer> list, BindMode class660Var) {
        setKey(list);
        this.type = class660Var;
    }

    public void setKey(int i) {
        setKey(i == -1 ? List.of() : List.of(Integer.valueOf(i)));
    }

    public void setKey(List<Integer> list) {
        this.boundKeys.clear();
        if (list == null) {
            return;
        }
        Stream<Integer> streamLimit = list.stream().filter(num -> {
            return (num.intValue() == -1 || num.intValue() == -1) ? false : true;
        }).limit(2L);
        List<Integer> list2 = this.boundKeys;
        Objects.requireNonNull(list2);
        streamLimit.forEach((v1) -> {
            list2.add(v1);
        });
    }

    public List<Integer> getKeyBind() {
        return Collections.unmodifiableList(this.boundKeys);
    }

    public boolean hasKeyBind() {
        return !this.boundKeys.isEmpty();
    }

    public int getKey() {
        if (this.boundKeys.isEmpty()) {
            return -1;
        }
        return ((Integer) this.boundKeys.getFirst()).intValue();
    }

    public void applyState() {
        if (this.state) {
            activate();
        } else {
            deactivate();
        }
    }

    public void activate() {
        Spectra.INSTANCE.eventDispatcher().dispatch(new ModuleStateEvent(this, true));
    }

    public void deactivate() {
        Spectra.INSTANCE.eventDispatcher().dispatch(new ModuleStateEvent(this, false));
    }

    public final <T extends Event> void register(Class<T> cls, EventCallback<T> class058Var) {
        Spectra.INSTANCE.eventDispatcher().register(cls, class058Var);
    }

    public final <T extends Event> void register(Class<T> cls, EventCallback<T> class058Var, EventPriority class396Var) {
        Spectra.INSTANCE.eventDispatcher().register(cls, class058Var, class396Var);
    }

    public ModuleTab getModuleTab() {
        return this.moduleTab;
    }

    public ModuleCategory getCategory() {
        return this.category;
    }

    public String getVisibleName() {
        return this.visibleName;
    }

    public String getName() {
        return this.name;
    }

    public String getFeatureId() {
        return ru.spectra.client.net.HolyWorldFeaturePolicy.featureId(this.name);
    }

    public BindMode getType() {
        return this.type;
    }

    public boolean isState() {
        return this.state && isAvailable();
    }

    public boolean isAvailable() {
        return this.serverAccessPolicy.allowsCurrentContext()
                && ru.spectra.client.net.HolyWorldFeatureControl.allows(getFeatureId());
    }

    public boolean isVisibleInMenu() {
        return isAvailable();
    }

    public Module serverAccessPolicy(ServerAccessPolicy policy) {
        this.serverAccessPolicy = Objects.requireNonNull(policy, "policy");
        return this;
    }

    public ServerAccessPolicy serverAccessPolicy() {
        return this.serverAccessPolicy;
    }

    public boolean enforceServerAccessPolicy() {
        if (this.state && !isAvailable()) {
            this.state = false;
            deactivate();
            return true;
        }
        return false;
    }

    public void setType(BindMode class660Var) {
        this.type = class660Var;
    }
}
