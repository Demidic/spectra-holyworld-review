package ru.spectra.client.util;
import ru.spectra.client.event.CancellableEvent;
import ru.spectra.client.event.Event;
import ru.spectra.client.event.EventCallback;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.RegisteredListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventDispatcher {
    private static final long ERROR_LOG_INTERVAL_MS = 60_000L;

    public final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new HashMap();
    private final Map<FailureKey, FailureState> failures = new ConcurrentHashMap<>();

    public <T extends Event> void dispatch(T r8) {
        List<RegisteredListener<?>> list = this.listeners.get(r8.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        int n = list.size();
        for (int i = 0; i < n; i++) {
            RegisteredListener<?> class225Var = list.get(i);
            EventCallback class058Var = class225Var.callback();
            try {
                class058Var.call(r8);
            } catch (Throwable th) {
                reportFailure(class058Var, class225Var, r8, th);
            }
            if (r8 instanceof CancellableEvent && ((CancellableEvent) r8).isStopProgression()) {
                break;
            }
        }
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> class058Var) {
        register(cls, class058Var, EventPriority.NORMAL);
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> class058Var, EventPriority class396Var) {
        List<RegisteredListener<?>> listComputeIfAbsent = this.listeners.computeIfAbsent(cls, cls2 -> {
            return new ArrayList();
        });
        listComputeIfAbsent.add(new RegisteredListener<>(class058Var, class396Var));
        listComputeIfAbsent.sort(Comparator.comparing(class225Var -> {
            return class225Var.priority();
        }));
    }

    public <T extends Event> void unregister(Class<T> cls, EventCallback<T> class058Var) {
        List<RegisteredListener<?>> list = this.listeners.get(cls);
        if (list != null) {
            list.removeIf(class225Var -> {
                return class225Var.callback() == class058Var;
            });
        }
    }

    private void reportFailure(EventCallback<?> callback, RegisteredListener<?> listener, Event event, Throwable throwable) {
        FailureState state = this.failures.computeIfAbsent(
                new FailureKey(callback, event.getClass()),
                ignored -> new FailureState()
        );
        long now = System.currentTimeMillis();
        int suppressed;
        synchronized (state) {
            if (state.lastLoggedAt != 0L && now - state.lastLoggedAt < ERROR_LOG_INTERVAL_MS) {
                state.suppressed++;
                return;
            }
            suppressed = state.suppressed;
            state.suppressed = 0;
            state.lastLoggedAt = now;
        }
        if (suppressed == 0) {
            Spectra.LOGGER.error("EventDispatcher: exception in callback {} (priority={}) for event {}",
                    callback.getClass().getName(), listener.priority(), event.getClass().getName(), throwable);
        } else {
            Spectra.LOGGER.error("EventDispatcher: exception in callback {} (priority={}) for event {} " +
                            "({} identical failures suppressed)",
                    callback.getClass().getName(), listener.priority(), event.getClass().getName(), suppressed, throwable);
        }
    }

    private record FailureKey(EventCallback<?> callback, Class<?> eventType) {
    }

    private static final class FailureState {
        private long lastLoggedAt;
        private int suppressed;
    }
}
