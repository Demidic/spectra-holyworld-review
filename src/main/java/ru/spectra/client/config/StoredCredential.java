package ru.spectra.client.config;

import com.google.gson.annotations.SerializedName;

/** One DPAPI-protected Minecraft server credential. */
public final class StoredCredential {
    @SerializedName("server")
    public String serverAddress;

    @SerializedName("username")
    public String username;

    @SerializedName("protectedPassword")
    public String protectedPassword;

    public StoredCredential() {
        // Gson constructor.
    }

    public StoredCredential(
            String serverAddress,
            String username,
            String protectedPassword
    ) {
        this.serverAddress = serverAddress;
        this.username = username;
        this.protectedPassword = protectedPassword;
    }
}
