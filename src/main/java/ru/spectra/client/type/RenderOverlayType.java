package ru.spectra.client.type;

public enum RenderOverlayType {
    BOSS_BAR,
    CAMERA_HURT,
    FIRE_OVERLAY,
    GLOWING,
    LAVA_OVERLAY,
    SCOREBOARD,
    TOTEM_POP,
    VIGNETTE,
    WITHER_HEARTS;

    public boolean isBossBar() {
        return this == BOSS_BAR;
    }

    public boolean isCameraHurt() {
        return this == CAMERA_HURT;
    }

    public boolean isFireOverlay() {
        return this == FIRE_OVERLAY;
    }

    public boolean isGlowing() {
        return this == GLOWING;
    }

    public boolean isLavaOverlay() {
        return this == LAVA_OVERLAY;
    }

    public boolean isScoreboard() {
        return this == SCOREBOARD;
    }

    public boolean isTotemPop() {
        return this == TOTEM_POP;
    }

    public boolean isVignette() {
        return this == VIGNETTE;
    }

    public boolean isWitherHearts() {
        return this == WITHER_HEARTS;
    }
}
