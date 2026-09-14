package ru.spectra.client.ui;

/**
 * Owns editor-only draggable representations of vanilla HUD overlays.
 */
public final class HudEditorOverlays {
    private static HudEditorOverlayWidget bossBar;
    private static HudEditorOverlayWidget scoreboard;
    private static HudEditorOverlayWidget playerList;

    private HudEditorOverlays() {
    }

    public static void initialize(WidgetStack stack) {
        if (bossBar != null) {
            return;
        }
        bossBar = new HudEditorOverlayWidget(HudEditorOverlayWidget.Kind.BOSS_BAR);
        scoreboard = new HudEditorOverlayWidget(HudEditorOverlayWidget.Kind.SCOREBOARD);
        playerList = new HudEditorOverlayWidget(HudEditorOverlayWidget.Kind.PLAYER_LIST);
        stack.newWidget(bossBar);
        stack.newWidget(scoreboard);
        stack.newWidget(playerList);
    }

    public static HudEditorOverlayWidget bossBar() {
        return bossBar;
    }

    public static HudEditorOverlayWidget scoreboard() {
        return scoreboard;
    }

    public static HudEditorOverlayWidget playerList() {
        return playerList;
    }

}
