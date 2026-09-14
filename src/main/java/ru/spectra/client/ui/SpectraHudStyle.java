package ru.spectra.client.ui;

import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.util.DrawEngine;
import net.minecraft.client.util.math.MatrixStack;

final class SpectraHudStyle {
    static final int PANEL = 0xBD0F0F0F;
    static final int PANEL_DARK = 0xC90F0F0F;
    static final int PANEL_TRANSLUCENT = 0xA80F0F0F;
    static final int BORDER = 0xFF2E3038;
    static final int OUTER_BORDER = 0xFF202227;
    static final int NOTIFICATION_BORDER = OUTER_BORDER;
    static final int SLOT_BORDER = BORDER;
    static final int TEXT = 0xFFE3E3E3;
    static final int MUTED = 0xFF868791;
    static final int ACCENT = 0xFF6F6ADF;
    static final int RED = 0xFFFF5252;
    static final int ORANGE = 0xFFFF9428;
    static final int DARK_ORANGE = 0xFFD65000;
    static final int GREEN = 0xFF26C68C;
    static final float RADIUS = 8.0f;
    static final float BORDER_WIDTH = 1.25f;
    private static int frameBlurTexture = -1;
    private static boolean frameBlurredSurface;

    private SpectraHudStyle() {
    }

    static void beginFrame(int blurTexture, boolean blurredSurface) {
        frameBlurTexture = blurTexture;
        frameBlurredSurface = blurredSurface;
    }

    static void endFrame() {
        frameBlurTexture = -1;
        frameBlurredSurface = false;
    }

    static int alpha(ColorStack colors, int color, float alpha) {
        float sourceAlpha = ((color >>> 24) & 0xFF) / 255.0f;
        return colors.computeColor(color, sourceAlpha * alpha);
    }

    static int color(ColorStack colors, int color) {
        return colors.computeColor(color, (color >>> 24) & 0xFF);
    }

    static int panelColor(ColorStack colors, int color) {
        int sourceAlpha = frameBlurredSurface ? (color >>> 24) & 0xFF : 0xFF;
        return colors.computeColor(color, sourceAlpha);
    }

    static int panelAlpha(ColorStack colors, int color, float alpha) {
        int sourceAlpha = frameBlurredSurface ? (color >>> 24) & 0xFF : 0xFF;
        return colors.computeColor(color, sourceAlpha / 255.0f * alpha);
    }

    static void panel(DrawEngine draw, MatrixStack matrices, float x, float y, float width, float height, float radius) {
        ColorStack colors = draw.colorStack();
        outlinedRect(draw, matrices, x, y, width, height, radius,
                color(colors, OUTER_BORDER), panelColor(colors, PANEL));
    }

    static void header(DrawEngine draw, MatrixStack matrices,
                       MsdfFont titleFont, String title,
                       MsdfFont iconFont, String iconGlyph,
                       float x, float y, float width,
                       int titleColor, int iconColor) {
        float centerY = y + 18.5f;
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), titleFont, title,
                x + 9.0f,
                centerY - titleFont.getHeight(13.0f) / 2.0f,
                13.0f, 0.05f, titleColor
        );
        draw.msdfFontVerticalCHorizontalC(
                matrices.peek().getPositionMatrix(), iconFont, iconGlyph,
                x + width - 16.0f, centerY,
                14.0f, 0.05f, iconColor
        );
    }

    static void outlinedRect(DrawEngine draw, MatrixStack matrices,
                             float x, float y, float width, float height, float radius,
                             int borderColor, int fillColor) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        draw.glassPanel(
                matrices.peek().getPositionMatrix(),
                x, y, width, height, radius, BORDER_WIDTH,
                borderColor, fillColor, frameBlurTexture
        );
    }
}
