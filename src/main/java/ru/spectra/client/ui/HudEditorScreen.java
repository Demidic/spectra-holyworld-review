package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Full-screen HUD editing surface. The world stays live, while Minecraft's
 * screen background supplies the game blur and the widget stack is rendered
 * afterwards so HUD previews remain sharp.
 */
public final class HudEditorScreen extends Screen {
    private static boolean menuPreviewEnabled;
    private final Screen parent;
    private final boolean restoreMenu;
    private final long openedAtNanos = System.nanoTime();
    private boolean playerListPreviewVisible;
    private boolean playerListKeyDown;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Spectra HUD Editor"));
        this.parent = parent;
        MenuWindow menu = Spectra.INSTANCE == null ? null : Spectra.INSTANCE.menuWindow();
        if (Spectra.INSTANCE != null) {
            Spectra.INSTANCE.widgetStack().resetEditorMenus();
        }
        this.restoreMenu = menu != null && menu.menuOpen;
        if (this.restoreMenu) {
            menu.suspendForHudEditor();
        }
    }

    public static boolean isAdvancedOpen() {
        return Mc.INSTANCE.getCurrentScreen() instanceof HudEditorScreen;
    }

    public static boolean isEditing() {
        Screen screen = Mc.INSTANCE.getCurrentScreen();
        return screen instanceof ChatScreen
                || screen instanceof HudEditorScreen
                || isMenuPreviewOpen();
    }

    public static boolean isMenuPreviewEnabled() {
        return menuPreviewEnabled;
    }

    public static boolean isMenuPreviewOpen() {
        return menuPreviewEnabled
                && !Mc.INSTANCE.isWorldLoaded()
                && Mc.INSTANCE.getCurrentScreen() != null;
    }

    public static void setMenuPreviewEnabled(boolean enabled) {
        menuPreviewEnabled = enabled;
    }

    public static boolean toggleMenuPreview() {
        menuPreviewEnabled = !menuPreviewEnabled;
        return menuPreviewEnabled;
    }

    public static void openFrom(Screen parent) {
        MinecraftClient.getInstance().setScreen(new HudEditorScreen(parent));
    }

    public static void closeCurrent() {
        if (Mc.INSTANCE.getCurrentScreen() instanceof HudEditorScreen editor) {
            editor.close();
        }
    }

    public static float entranceProgress() {
        if (!(Mc.INSTANCE.getCurrentScreen() instanceof HudEditorScreen editor)) {
            return 1.0f;
        }
        if (Mc.INSTANCE.isWorldLoaded()) {
            return 1.0f;
        }
        float linear = Math.min(1.0f,
                (System.nanoTime() - editor.openedAtNanos) / 380_000_000.0f);
        float inverse = 1.0f - linear;
        return 1.0f - inverse * inverse * inverse;
    }

    public static float elementEntranceProgress(int index) {
        if (!(Mc.INSTANCE.getCurrentScreen() instanceof HudEditorScreen editor)) {
            return 1.0f;
        }
        if (Mc.INSTANCE.isWorldLoaded()) {
            return 1.0f;
        }
        long delay = Math.max(0, index) * 28_000_000L;
        float linear = Math.min(1.0f, Math.max(0.0f,
                (System.nanoTime() - editor.openedAtNanos - delay) / 300_000_000.0f));
        float inverse = 1.0f - linear;
        return 1.0f - inverse * inverse * inverse;
    }

    public static boolean isPlayerListPreviewVisible() {
        return Mc.INSTANCE.getCurrentScreen() instanceof HudEditorScreen editor
                && editor.playerListPreviewVisible;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Spectra.INSTANCE != null
                && Spectra.INSTANCE.widgetStack().handleAddWidgetKeyPressed(keyCode)) {
            return true;
        }
        if (this.client != null
                && this.client.options.playerListKey.matchesKey(keyCode, scanCode)) {
            if (!this.playerListKeyDown) {
                this.playerListPreviewVisible = !this.playerListPreviewVisible;
                this.playerListKeyDown = true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (Spectra.INSTANCE != null
                && Spectra.INSTANCE.widgetStack().handleAddWidgetCharTyped(chr)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.client != null
                && this.client.options.playerListKey.matchesKey(keyCode, scanCode)) {
            this.playerListKeyDown = false;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if (Spectra.INSTANCE != null) {
            float entrance = entranceProgress();
            float scale = 0.94f + entrance * 0.06f;
            float centerX = context.getScaledWindowWidth() / 2.0f;
            float centerY = context.getScaledWindowHeight() / 2.0f;
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 0.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-centerX, -centerY, 0.0f);
            HudVanillaPreviewRenderer.render(context);
            context.getMatrices().pop();
            if (!Mc.INSTANCE.isWorldLoaded()) {
                Spectra.INSTANCE.widgetStack().update();
            }
            Spectra.INSTANCE.widgetStack().draw();
        }
    }

    @Override
    public void close() {
        if (Spectra.INSTANCE != null) {
            Spectra.INSTANCE.widgetStack().resetEditorMenus();
        }
        MinecraftClient.getInstance().setScreen(this.parent);
        if (Spectra.INSTANCE != null) {
            Spectra.INSTANCE.configManager().saveLocalConfig();
            if (this.restoreMenu) {
                Spectra.INSTANCE.menuWindow().resumeAfterHudEditor();
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
