package ru.spectra.client.net;
import ru.spectra.client.render.GlTexture;


public final class UserSession {
    public final String uid;
    public final String username;
    public final String hwid;
    public final String role;
    public final String expire;
    public final String avatarUrl;
    public final GlTexture texture;
    private final long playedSecondsAtLaunch;
    private final long sessionStartedAtNanos;

    public UserSession(String str, String str2, String str3, String str4, String str5, String str6, GlTexture class073Var) {
        this(str, str2, str3, str4, str5, str6, class073Var, 0L);
    }

    public UserSession(String str, String str2, String str3, String str4, String str5,
                       String str6, GlTexture class073Var, long playedSecondsAtLaunch) {
        this(str, str2, str3, str4, str5, str6, class073Var,
                playedSecondsAtLaunch, System.nanoTime());
    }

    private UserSession(String str, String str2, String str3, String str4, String str5,
                        String str6, GlTexture class073Var, long playedSecondsAtLaunch,
                        long sessionStartedAtNanos) {
        this.uid = str;
        this.username = str2;
        this.hwid = str3;
        this.role = str4;
        this.expire = str5;
        this.avatarUrl = str6;
        this.texture = class073Var;
        this.playedSecondsAtLaunch = Math.max(0L, playedSecondsAtLaunch);
        this.sessionStartedAtNanos = sessionStartedAtNanos;
    }

    @Override
    public String toString() {
        return String.format(
                "Username %s, Uid %s, Role %s, Hwid <redacted>",
                this.username,
                this.uid,
                this.role
        );
    }

    public UserSession withExpire(String str) {
        return java.util.Objects.equals(this.expire, str) ? this : new UserSession(
                this.uid, this.username, this.hwid, this.role, str, this.avatarUrl,
                this.texture, this.playedSecondsAtLaunch, this.sessionStartedAtNanos);
    }

    public UserSession withTexture(GlTexture class073Var) {
        return java.util.Objects.equals(this.texture, class073Var) ? this : new UserSession(
                this.uid, this.username, this.hwid, this.role, this.expire,
                this.avatarUrl, class073Var, this.playedSecondsAtLaunch,
                this.sessionStartedAtNanos);
    }

        @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.uid, this.username, this.hwid, this.role, this.expire, this.avatarUrl, this.texture);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserSession)) return false;
        UserSession o = (UserSession) obj;
        return java.util.Objects.equals(this.uid, o.uid) && java.util.Objects.equals(this.username, o.username) && java.util.Objects.equals(this.hwid, o.hwid) && java.util.Objects.equals(this.role, o.role) && java.util.Objects.equals(this.expire, o.expire) && java.util.Objects.equals(this.avatarUrl, o.avatarUrl) && java.util.Objects.equals(this.texture, o.texture);
    }
public String uid() {
        return this.uid;
    }

    public String username() {
        return this.username;
    }

    public String hwid() {
        return this.hwid;
    }

    public String role() {
        return this.role;
    }

    public String expire() {
        return this.expire;
    }

    public String avatarUrl() {
        return this.avatarUrl;
    }

    public GlTexture texture() {
        return this.texture;
    }

    public long playedSeconds() {
        long elapsed = Math.max(0L, System.nanoTime() - this.sessionStartedAtNanos)
                / 1_000_000_000L;
        return this.playedSecondsAtLaunch + elapsed;
    }
}
