package ru.spectra.ui;

import ru.spectra.client.ui.HudEditorScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Adds Spectra actions without replacing vanilla or third-party title buttons.
 */
public final class TitleScreenIntegration {
    private static final String HUD_EDITOR_LABEL = "Настроить HUD";
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int FULL_WIDTH = 200;
    private static final int HALF_WIDTH = 98;

    private static final Map<Screen, Integer> PENDING_LAYOUTS =
            new WeakHashMap<>();
    private static int nextLayoutGeneration;

    private TitleScreenIntegration() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof TitleScreen)) {
                return;
            }

            int generation = ++nextLayoutGeneration;
            PENDING_LAYOUTS.put(screen, generation);

            /*
             * Run on the first frame, after every global AFTER_INIT listener
             * has had a chance to add buttons. This makes the layout independent
             * of mod initialization order.
             */
            ScreenEvents.beforeRender(screen).register(
                    (renderedScreen, context, mouseX, mouseY, tickDelta) -> {
                        Integer pending = PENDING_LAYOUTS.get(renderedScreen);
                        if (pending == null || pending != generation) {
                            return;
                        }

                        integrate(renderedScreen, width, height);
                        PENDING_LAYOUTS.remove(renderedScreen);
                    }
            );
        });
    }

    private static void integrate(Screen screen, int width, int height) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        ButtonWidget hudEditor = findHudEditorButton(buttons);
        if (hudEditor == null) {
            hudEditor = ButtonWidget.builder(
                    Text.literal(HUD_EDITOR_LABEL),
                    button -> HudEditorScreen.openFrom(screen)
            ).dimensions(0, 0, FULL_WIDTH, BUTTON_HEIGHT).build();
            buttons.add(hudEditor);
        }

        layoutCentralButtons(buttons, hudEditor, width, height);
    }

    private static ButtonWidget findHudEditorButton(
            List<ClickableWidget> buttons
    ) {
        for (ClickableWidget widget : buttons) {
            if (widget instanceof ButtonWidget button
                    && HUD_EDITOR_LABEL.equals(button.getMessage().getString())) {
                return button;
            }
        }
        return null;
    }

    private static void layoutCentralButtons(
            List<ClickableWidget> buttons,
            ButtonWidget hudEditor,
            int screenWidth,
            int screenHeight
    ) {
        int screenCenter = screenWidth / 2;
        List<ButtonWidget> primary = new ArrayList<>();
        ButtonWidget options = null;
        ButtonWidget quit = null;

        for (ClickableWidget widget : buttons) {
            if (!(widget instanceof ButtonWidget button)
                    || button == hudEditor
                    || !isCentralMenuButton(button, screenCenter, screenHeight)) {
                continue;
            }

            String translationKey = translationKey(button.getMessage());
            if ("menu.options".equals(translationKey)) {
                options = button;
            } else if ("menu.quit".equals(translationKey)) {
                quit = button;
            } else {
                primary.add(button);
            }
        }

        int footerRows = options != null || quit != null ? 1 : 0;
        int rowCount = primary.size() + footerRows + 1;
        int contentHeight = BUTTON_HEIGHT + (rowCount - 1) * ROW_STEP;
        int vanillaStart = screenHeight / 4 + 48;
        int lowestStart = Math.max(32, screenHeight - 32 - contentHeight);
        int startY = Math.min(vanillaStart, lowestStart);
        int fullX = screenCenter - FULL_WIDTH / 2;
        int y = startY;

        for (ButtonWidget button : primary) {
            place(button, fullX, y, FULL_WIDTH);
            y += ROW_STEP;
        }

        if (options != null && quit != null) {
            place(options, fullX, y, HALF_WIDTH);
            place(quit, screenCenter + 2, y, HALF_WIDTH);
            y += ROW_STEP;
        } else if (options != null) {
            place(options, fullX, y, FULL_WIDTH);
            y += ROW_STEP;
        } else if (quit != null) {
            place(quit, fullX, y, FULL_WIDTH);
            y += ROW_STEP;
        }

        place(hudEditor, fullX, y, FULL_WIDTH);
    }

    private static boolean isCentralMenuButton(
            ButtonWidget button,
            int screenCenter,
            int screenHeight
    ) {
        int buttonCenter = button.getX() + button.getWidth() / 2;
        return button.visible
                && button.getWidth() >= 90
                && button.getHeight() == BUTTON_HEIGHT
                && Math.abs(buttonCenter - screenCenter) <= 110
                && button.getY() >= 24
                && button.getY() < screenHeight - 20;
    }

    private static String translationKey(Text text) {
        if (text.getContent() instanceof TranslatableTextContent translatable) {
            return translatable.getKey();
        }
        return "";
    }

    private static void place(
            ButtonWidget button,
            int x,
            int y,
            int width
    ) {
        button.setDimensionsAndPosition(width, BUTTON_HEIGHT, x, y);
    }
}
