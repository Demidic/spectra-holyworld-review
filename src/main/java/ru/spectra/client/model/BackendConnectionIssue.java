package ru.spectra.client.model;

/** UI diagnostics only. Not a production protection or licensing component. */
public enum BackendConnectionIssue {
    NONE, NETWORK, TIMEOUT, SERVICE, RATE_LIMIT, SESSION_EXPIRED, ACCESS_DENIED,
    TLS, INVALID_RESPONSE;

    public boolean retryable() {
        return this == NETWORK || this == TIMEOUT || this == SERVICE || this == RATE_LIMIT;
    }
}
