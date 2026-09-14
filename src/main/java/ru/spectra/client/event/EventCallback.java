package ru.spectra.client.event;


public interface EventCallback<T extends Event> {
    void call(T t);
}
