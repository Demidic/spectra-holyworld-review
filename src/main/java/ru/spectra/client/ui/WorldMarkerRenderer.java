package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.util.DrawEngine;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

/** Renders ordinary waypoints and server events with one shared HUD layout. */
public final class WorldMarkerRenderer {
    public static final float WIDTH = 204.0f;
    public static final float HEIGHT = 64.0f;

    private static final float ICON_AREA = 44.0f;
    private static final float TITLE_SIZE = 12.0f;
    private static final float META_SIZE = 10.0f;
    private static final float TIMER_SIZE = 10.0f;

    private static final MsdfFont TITLE_FONT = Fonts.INTER_BOLD.get();
    private static final MsdfFont META_FONT = Fonts.INTER_SEMIBOLD.get();
    private static final MsdfFont ICON_FONT = Fonts.MENU_ICON.get();

    private WorldMarkerRenderer() {
    }

    public static void renderWaypoint(DrawEngine draw, MatrixStack matrices,
                                      Vector2f anchor, String name, Vec3d position) {
        render(draw, matrices, anchor, name, position, null, null);
    }

    public static void renderEvent(DrawEngine draw, MatrixStack matrices,
                                   Vector2f anchor, String name, Vec3d position,
                                   GlTexture icon, String timer) {
        render(draw, matrices, anchor, name, position, icon, timer);
    }

    private static void render(DrawEngine draw, MatrixStack matrices,
                               Vector2f anchor, String name, Vec3d position,
                               GlTexture eventIcon, String timer) {
        ColorStack colors = draw.colorStack();
        ThemePalette palette = Spectra.INSTANCE.theme().palette();
        float x = anchor.x - WIDTH / 2.0f;
        float y = anchor.y - HEIGHT / 2.0f;
        float contentX = x + 60.0f;
        float right = x + WIDTH - 10.0f;

        SpectraHudStyle.panel(draw, matrices, x, y, WIDTH, HEIGHT, 8.0f);

        if (eventIcon == null) {
            draw.msdfFontVerticalCHorizontalC(
                    matrices.peek().getPositionMatrix(), ICON_FONT,
                    CoordsWidget.COORDINATES_ICON_GLYPH,
                    x + 8.0f + ICON_AREA / 2.0f, y + HEIGHT / 2.0f,
                    19.0f, 0.05f, colors.computeColor(0xFFFFFFFF)
            );
        } else {
            float iconSize = 32.0f;
            draw.texture(
                    matrices.peek().getPositionMatrix(),
                    x + 8.0f + (ICON_AREA - iconSize) / 2.0f,
                    y + (HEIGHT - iconSize) / 2.0f,
                    iconSize, iconSize,
                    draw.bindTexture(eventIcon.textureWithSTB()),
                    colors.computeColor(0xFFFFFFFF)
            );
        }

        float timerWidth = timer == null ? 0.0f : META_FONT.getWidth(timer, TIMER_SIZE);
        float titleLimit = Math.max(24.0f, right - contentX - (timerWidth > 0.0f ? timerWidth + 10.0f : 0.0f));
        String fittedName = fit(name, TITLE_FONT, TITLE_SIZE, titleLimit);
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), TITLE_FONT, fittedName,
                contentX, y + 16.0f - TITLE_FONT.getHeight(TITLE_SIZE) / 2.0f,
                TITLE_SIZE, 0.05f,
                colors.computeColor(palette.text().tone(100).argb())
        );

        if (timer != null) {
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(), META_FONT, timer,
                    right - timerWidth,
                    y + 16.0f - META_FONT.getHeight(TIMER_SIZE) / 2.0f,
                    TIMER_SIZE, 0.05f,
                    colors.computeColor(palette.accent().argb())
            );
        }

        String coordinates = coordinates(position);
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), META_FONT, coordinates,
                contentX, y + 42.0f - META_FONT.getHeight(META_SIZE) / 2.0f,
                META_SIZE, 0.05f,
                colors.computeColor(palette.text().tone(400).argb())
        );
    }

    private static String coordinates(Vec3d position) {
        return "X " + (int) Math.floor(position.x)
                + "  Y " + (int) Math.floor(position.y)
                + "  Z " + (int) Math.floor(position.z);
    }

    private static String fit(String value, MsdfFont font, float size, float maxWidth) {
        if (value == null || value.isBlank()) {
            return "Marker";
        }
        if (font.getWidth(value, size) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        int length = value.length();
        while (length > 1 && font.getWidth(value.substring(0, length) + suffix, size) > maxWidth) {
            length--;
        }
        return value.substring(0, length) + suffix;
    }
}
