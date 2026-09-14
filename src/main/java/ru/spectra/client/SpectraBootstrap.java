package ru.spectra.client;

import net.minecraft.client.MinecraftClient;
import ru.spectra.client.net.UserSession;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.resource.ClasspathResource;

/** Review-only local profile: no license lease, protected loader or production credential. */
public final class SpectraBootstrap {
    private SpectraBootstrap() {
    }

    public static void init() {
        String username = MinecraftClient.getInstance().getSession().getUsername();
        new Spectra(new UserSession("holyworld-review", username, "", "reviewer",
                "review-build", "", new GlTexture(new ClasspathResource("assets/spectra/textures/avatar.png"))));
    }
}
