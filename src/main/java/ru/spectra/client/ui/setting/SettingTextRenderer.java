package ru.spectra.client.ui.setting;

import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.MsdfFont;

/**
 * Draws wrapped setting text with an explicit left edge for every line.
 * The generic MSDF text path keeps the previous line's pen position after a
 * newline, which makes continuation lines drift underneath right-side
 * controls.
 */
final class SettingTextRenderer {
    private SettingTextRenderer() {
    }

    static void drawWrapped(DrawCtx context, MsdfFont font, String text,
                            int size, float x, float y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float lineY = y;
        float lineHeight = font.getHeight(size);
        for (String line : text.split("\\n", -1)) {
            context.text(font, line, size, x, lineY, color);
            lineY += lineHeight;
        }
    }
}
