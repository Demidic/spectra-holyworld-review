package ru.spectra.client.model;


/**
 * Thread-safe state supplied by the update/backend services to the fixed
 * system-status overlay. This is deliberately separate from HUD widgets and
 * their persisted configuration.
 */
public final class SystemStatusState {
    static final long UPDATE_INSTALLED_DURATION_MILLIS = 30_000L;
    private volatile boolean updateAvailable;
    private volatile BackendConnectionIssue connectionIssue = BackendConnectionIssue.NONE;
    private volatile long updateInstalledUntil;
    private volatile String releaseNotesUrl = "";

    public boolean updateAvailable() {
        return this.updateAvailable;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public boolean serversUnavailable() {
        return this.connectionIssue != BackendConnectionIssue.NONE;
    }

    public void setServersUnavailable(boolean serversUnavailable) {
        setConnectionIssue(serversUnavailable ? BackendConnectionIssue.NETWORK : BackendConnectionIssue.NONE);
    }

    public BackendConnectionIssue connectionIssue() { return this.connectionIssue; }

    public void setConnectionIssue(BackendConnectionIssue issue) {
        this.connectionIssue = java.util.Objects.requireNonNull(issue);
    }

    public void showUpdateInstalled(String releaseNotesUrl) {
        this.releaseNotesUrl = releaseNotesUrl == null
                ? "" : releaseNotesUrl;
        this.updateInstalledUntil = System.currentTimeMillis()
                + UPDATE_INSTALLED_DURATION_MILLIS;
    }

    public boolean updateInstalled() {
        return System.currentTimeMillis() < this.updateInstalledUntil;
    }

    public String releaseNotesUrl() {
        return this.releaseNotesUrl;
    }
}
