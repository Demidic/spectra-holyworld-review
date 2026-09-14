package ru.spectra.client.type;

public enum ChatLimitType {
    HISTORY,
    LIMIT;

    public boolean isHistory() {
        return this == HISTORY;
    }

    public boolean isLimit() {
        return this == LIMIT;
    }
}
