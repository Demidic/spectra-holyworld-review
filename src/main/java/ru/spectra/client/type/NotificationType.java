package ru.spectra.client.type;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.GlTexture;

public enum NotificationType {
    INFO("/icons/menu/new/info.png"),
    WARNING("/icons/menu/new/warning.png"),
    ERROR("/icons/menu/new/debug.png"),
    EVENT("/icons/menu/new/falling_star.png"),
    SUCCESS("/icons/menu/new/success.png"),
    MODULE_ENABLED("/icons/menu/new/eyeopen.png"),
    MODULE_DISABLED("/icons/menu/new/eyeclosed.png");

    public final String iconPath;
    public final GlTexture texture;

    NotificationType(String str) {
        this.iconPath = str;
        this.texture = new GlTexture(new ClasspathResource(str));
    }

    public String getIconPath() {
        return this.iconPath;
    }

    public GlTexture getTexture() {
        return this.texture;
    }
}
