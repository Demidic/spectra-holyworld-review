package ru.spectra.client.model;

public class PinnedServerEntry {
    private String name;
    private String address;
    private int accentColor;

    public PinnedServerEntry() {
        this("", "", 0);
    }

    public PinnedServerEntry(String name, String address, int accentColor) {
        this.name = name == null ? "" : name;
        this.address = address == null ? "" : address;
        this.accentColor = accentColor;
    }

    public int accentColor() {
        return this.accentColor;
    }

    public String address() {
        return this.address;
    }

    public String name() {
        return this.name;
    }
}
