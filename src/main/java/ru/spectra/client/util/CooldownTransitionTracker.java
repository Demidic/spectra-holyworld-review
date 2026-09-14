package ru.spectra.client.util;

import java.util.HashMap;
import java.util.Map;

/** A completed cooldown is a true -> false edge, never merely an idle item. */
public final class CooldownTransitionTracker<T> {
    private final Map<T, Boolean> previous = new HashMap<>();

    public boolean observe(T key, boolean cooling) {
        return Boolean.TRUE.equals(this.previous.put(key, cooling)) && !cooling;
    }

    public void clear() {
        this.previous.clear();
    }
}
