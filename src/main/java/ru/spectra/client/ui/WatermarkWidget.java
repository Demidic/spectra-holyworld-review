package ru.spectra.client.ui;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.Translation;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.net.UserSession;
import ru.spectra.client.math.Easings;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.SpectraLogoRenderer;
import ru.spectra.client.type.HudInfoType;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.WatermarkPosition;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.SeparatorSetting;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.util.WeightedEngine;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.client.util.math.MatrixStack;

public class WatermarkWidget extends Draggable {
    private static final String CLIENT_LABEL = "spectra";
    private static final float BASE_HEIGHT = 30.0f;
    private static final float STYLE_SCALE = 0.8f;

    private final MsdfFont iconFont;
    private final MsdfFont font;
    private final MsdfFont brandFont;
    private final EnumMap<HudInfoType, RollingSection> rollingSections;
    private final EnumMap<HudInfoType, RollingTextParts> frameParts;
    public final MultiSelectSetting<HudInfoType> hudLines;
    public final ModeSetting<WatermarkPosition> position;
    public final NumberSetting scale;
    public final BooleanSetting use12hFormat;
    private float computedWidth = 250.0f;
    private String frameUsername = "User";

    public WatermarkWidget(BooleanSupplier visibility) {
        super("Watermark", visibility);
        this.iconFont = Fonts.MENU_ICON.get();
        this.font = Fonts.INTER_BOLD.get();
        this.brandFont = Fonts.INTER_BOLD.get();
        this.rollingSections = new EnumMap<>(HudInfoType.class);
        this.frameParts = new EnumMap<>(HudInfoType.class);
        this.position = new ModeSetting<WatermarkPosition>(
                Translation.clearText("Position")
        ).values(WatermarkPosition.class);
        this.hudLines = new MultiSelectSetting<HudInfoType>(
                Lang.WIDGET_WATERMARK_HUD_LINES,
                Translation.clearText("Choose which session details are shown in the watermark"))
                .values(HudInfoType.class)
                .select(HudInfoType.NICKNAME, HudInfoType.LATENCY, HudInfoType.FRAMERATE);
        this.scale = new NumberSetting(Lang.WIDGET_WATERMARK_SCALE).currentValue(1.0f).range(0.5f, 2.0f).step(0.05f);
        this.use12hFormat = new BooleanSetting(Lang.WIDGET_WATERMARK_USE_12H_FORMAT)
                .visible(() -> this.hudLines.isSelected(HudInfoType.CURRENT_TIME));
        addSettings(this.position, this.use12hFormat,
                new SeparatorSetting(), this.hudLines);
        setDragLocked(true);
        this.x = 30.0f;
        this.y = 30.0f;
    }

    @Override
    public void layout(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        this.frameParts.clear();
        this.frameUsername = username();
        float width = 11.0f + 12.0f + 6.0f + this.brandFont.getWidth(CLIENT_LABEL, 12.0f);
        if (this.hudLines.isSelected(HudInfoType.NICKNAME)) {
            width += sectionWidth(this.frameUsername, 12.0f);
        }
        for (HudInfoType type : HudInfoType.values()) {
            if (type != HudInfoType.NICKNAME && this.hudLines.isSelected(type)) {
                String text = textFor(type, Mc.INSTANCE);
                if (text != null && !text.isEmpty()) {
                    RollingTextParts parts = rollingTextParts(type, text);
                    this.frameParts.put(type, parts);
                    RollingSection state = rollingState(type, parts.animated());
                    width += 12.0f + 12.0f + 6.0f
                            + rollingWidth(state, fontSizeFor(type))
                            + this.font.getWidth(parts.suffix(), fontSizeFor(type));
                }
            }
        }
        this.computedWidth = width + 11.0f;
        applyFixedPosition(context);
    }

    private float sectionWidth(String text, float fontSize) {
        return 12.0f + 12.0f + 6.0f + this.font.getWidth(text, fontSize);
    }

    @Override
    public void render(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        int iconColor = context.theme().palette().accentBright().argb();
        float localScale = this.scale.currentValue() * STYLE_SCALE
                * WidgetStack.BASE_HUD_SCALE / WidgetStack.HUD_SCALE;
        matrices.push();
        matrices.translate(this.x, this.y, 0.0f);
        matrices.scale(localScale, localScale, 1.0f);
        matrices.translate(-this.x, -this.y, 0.0f);

        SpectraHudStyle.outlinedRect(
                draw, matrices, this.x, this.y, this.computedWidth, BASE_HEIGHT, 10.0f,
                SpectraHudStyle.OUTER_BORDER,
                SpectraHudStyle.panelColor(draw.colorStack(), SpectraHudStyle.PANEL_DARK)
        );

        float centerY = this.y + BASE_HEIGHT / 2.0f;
        float cursor = this.x + 11.0f;
        SpectraLogoRenderer.drawCentered(
                draw, matrices.peek().getPositionMatrix(), cursor, centerY, 12.0f, 0xFFFFFFFF);
        cursor += 18.0f;
        draw.msdfFont(
                matrices.peek().getPositionMatrix(),
                this.brandFont,
                CLIENT_LABEL,
                cursor,
                centerY - this.brandFont.getHeight(12.0f) / 2.0f,
                12.0f,
                0.05f,
                SpectraHudStyle.TEXT
        );
        cursor += this.brandFont.getWidth(CLIENT_LABEL, 12.0f);
        if (this.hudLines.isSelected(HudInfoType.NICKNAME)) {
            cursor = drawSection(draw, matrices, cursor, iconFor(HudInfoType.NICKNAME),
                    this.frameUsername, 12.0f, 0xFFFFFFFF, iconColor);
        }
        for (HudInfoType type : HudInfoType.values()) {
            if (type != HudInfoType.NICKNAME && this.hudLines.isSelected(type)) {
                RollingTextParts parts = this.frameParts.get(type);
                if (parts != null) {
                    cursor = drawRollingSection(draw, matrices, cursor, iconFor(type),
                            rollingState(type, parts.animated()), parts.suffix(),
                            fontSizeFor(type), 0xFFD2D2D2, iconColor);
                }
            }
        }
        matrices.pop();
    }

    private float drawSection(DrawEngine draw, MatrixStack matrices, float cursor, String icon,
                              String text, float fontSize, int textColor, int iconColor) {
        float centerY = this.y + BASE_HEIGHT / 2.0f;
        cursor += 12.0f;
        draw.msdfFontVerticalC(
                matrices.peek().getPositionMatrix(), this.iconFont, icon,
                cursor, centerY, 12.0f, 0.05f, iconColor
        );
        cursor += 18.0f;
        draw.msdfFont(
                matrices.peek().getPositionMatrix(),
                this.font,
                text,
                cursor,
                centerY - this.font.getHeight(fontSize) / 2.0f,
                fontSize,
                0.05f,
                textColor
        );
        return cursor + this.font.getWidth(text, fontSize);
    }

    private float drawRollingSection(DrawEngine draw, MatrixStack matrices, float cursor, String icon,
                                     RollingSection state, String suffix, float fontSize,
                                     int textColor, int iconColor) {
        float centerY = this.y + BASE_HEIGHT / 2.0f;
        cursor += 12.0f;
        draw.msdfFontVerticalC(
                matrices.peek().getPositionMatrix(), this.iconFont, icon,
                cursor, centerY, 12.0f, 0.05f, iconColor
        );
        cursor += 18.0f;
        float progress = state.animation.animatedValue();
        float width = rollingWidth(state, fontSize);
        float baseY = centerY - this.font.getHeight(fontSize) / 2.0f;
        if (!state.animating()) {
            draw.msdfFont(matrices.peek().getPositionMatrix(), this.font, state.current, cursor, baseY, fontSize, 0.05f, textColor);
            draw.msdfFont(matrices.peek().getPositionMatrix(), this.font, suffix, cursor + width, baseY, fontSize, 0.05f, textColor);
            return cursor + width + this.font.getWidth(suffix, fontSize);
        }
        float direction = compareLeadingNumber(state.current, state.previous) >= 0 ? -1.0f : 1.0f;
        float travel = 14.0f;
        draw.beginScissor(matrices.peek().getPositionMatrix(), cursor, centerY - 7.0f, width, 14.0f);
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.font, state.previous, cursor, baseY + direction * progress * travel, fontSize, 0.05f, textColor);
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.font, state.current, cursor, baseY - direction * (1.0f - progress) * travel, fontSize, 0.05f, textColor);
        draw.endScissor();
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.font, suffix, cursor + width, baseY, fontSize, 0.05f, textColor);
        return cursor + width + this.font.getWidth(suffix, fontSize);
    }

    private RollingSection rollingState(HudInfoType type, String value) {
        RollingSection state = this.rollingSections.computeIfAbsent(type, ignored -> new RollingSection());
        state.initialize(value);
        return state;
    }

    private float rollingWidth(RollingSection state, float fontSize) {
        float currentWidth = this.font.getWidth(state.current, fontSize);
        return state.animating() ? Math.max(currentWidth, this.font.getWidth(state.previous, fontSize)) : currentWidth;
    }

    private static int compareLeadingNumber(String left, String right) {
        return Integer.compare(leadingNumber(left), leadingNumber(right));
    }

    private static int leadingNumber(String value) {
        int number = 0;
        boolean found = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isDigit(character)) {
                break;
            }
            found = true;
            number = number * 10 + (character - '0');
        }
        return found ? number : 0;
    }

    private static RollingTextParts rollingTextParts(HudInfoType type, String value) {
        if (type != HudInfoType.LATENCY && type != HudInfoType.FRAMERATE) {
            return new RollingTextParts(value, "");
        }
        int split = 0;
        while (split < value.length() && Character.isDigit(value.charAt(split))) {
            split++;
        }
        return split == 0
                ? new RollingTextParts(value, "")
                : new RollingTextParts(value.substring(0, split), value.substring(split));
    }

    private String username() {
        UserSession profile = Spectra.INSTANCE == null ? null : Spectra.INSTANCE.userSession();
        String minecraftName = Mc.INSTANCE.getSession() == null
                ? null : Mc.INSTANCE.getSession().getUsername();
        return accountUsername(profile, minecraftName);
    }

    static String accountUsername(UserSession profile, String minecraftName) {
        if (profile != null && profile.username() != null && !profile.username().isBlank()) {
            return profile.username();
        }
        if (minecraftName != null && !minecraftName.isBlank()) {
            return minecraftName;
        }
        return "User";
    }

    private String iconFor(HudInfoType type) {
        return switch (type) {
            case NICKNAME -> "4";
            case LATENCY -> "з";
            case FRAMERATE -> "j";
            case CURRENT_TIME -> isDaytimeIcon() ? "ф" : "Ч";
            case SERVER_NAME -> "у";
        };
    }

    private boolean isDaytimeIcon() {
        int hour = LocalTime.now().getHour();
        return hour >= 6 && hour < 18;
    }

    private int fontSizeFor(HudInfoType type) {
        return 12;
    }

    private String textFor(HudInfoType type, Mc mc) {
        return switch (type) {
            case NICKNAME -> username();
            case LATENCY -> getPing() + " ping";
            case FRAMERATE -> mc.getCurrentFps() + " fps";
            case CURRENT_TIME -> formatTime();
            case SERVER_NAME -> serverAddress();
        };
    }

    private static final DateTimeFormatter CLOCK_12H = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter CLOCK_24H = DateTimeFormatter.ofPattern("HH:mm");

    private String formatTime() {
        return LocalTime.now().format(this.use12hFormat.isValue()
                ? CLOCK_12H
                : CLOCK_24H);
    }

    private String serverAddress() {
        String address = ServerUtil.getServerIp();
        return address == null ? "" : address.toLowerCase(Locale.ROOT);
    }

    private int getPing() {
        if (!Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getNetworkHandler() == null || Mc.INSTANCE.getPlayer() == null) {
            return 0;
        }
        var entry = Mc.INSTANCE.getNetworkHandler().getPlayerListEntry(Mc.INSTANCE.getPlayer().getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private void applyFixedPosition(DragRenderContext context) {
        float topMargin = height();
        if (this.position.isSelected(WatermarkPosition.TOP_CENTER)) {
            this.x = Math.max(0.0f, (context.resolution().screenWidth() - width()) / 2.0f);
            this.y = topMargin;
            return;
        }
        this.x = 30.0f;
        this.y = topMargin;
    }

    public boolean isTopCenter() {
        return this.position.isSelected(WatermarkPosition.TOP_CENTER);
    }

    public float bossBarOffset() {
        if (!isTopCenter()) {
            return 0.0f;
        }
        double guiScale = Math.max(1.0d, Mc.INSTANCE.getWindow().getScaleFactor());
        float bottomInFramebufferPixels = (height() + height()) * WidgetStack.HUD_SCALE;
        float gapInFramebufferPixels = 8.0f;
        // Vanilla draws the boss name nine GUI pixels above the bar. Align the
        // title, not merely the bar texture, below the centered watermark.
        float desiredBossBarY = (float) ((bottomInFramebufferPixels + gapInFramebufferPixels) / guiScale) + 9.0f;
        return Math.max(0.0f, desiredBossBarY - 12.0f);
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return hovered && !input.intercepted();
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.rollingSections.values().forEach(section -> section.animation.animate(engine));
    }

    @Override
    public void update() {
        for (HudInfoType type : HudInfoType.values()) {
            if (type == HudInfoType.NICKNAME || !this.hudLines.isSelected(type)) {
                continue;
            }
            String value = textFor(type, Mc.INSTANCE);
            if (value != null && !value.isEmpty()) {
                RollingTextParts parts = rollingTextParts(type, value);
                rollingState(type, parts.animated()).offer(parts.animated());
            }
        }
    }

    @Override
    public float width() {
        return this.computedWidth * this.scale.currentValue() * STYLE_SCALE
                * WidgetStack.BASE_HUD_SCALE / WidgetStack.HUD_SCALE;
    }

    @Override
    public float height() {
        return BASE_HEIGHT * this.scale.currentValue() * STYLE_SCALE
                * WidgetStack.BASE_HUD_SCALE / WidgetStack.HUD_SCALE;
    }

    private static final class RollingSection {
        private final AnimatedFloat animation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);
        private String previous = "";
        private String current = "";
        private String pending;

        private void initialize(String value) {
            if (!this.current.isEmpty()) {
                return;
            }
            this.previous = value;
            this.current = value;
            this.animation.set(1.0f);
        }

        private void offer(String value) {
            initialize(value);
            if (this.animation.isAtDestination() && this.pending != null) {
                String next = this.pending;
                this.pending = null;
                if (!Objects.equals(next, this.current)) {
                    start(next);
                }
            }
            if (Objects.equals(value, this.current)) {
                return;
            }
            if (this.animation.isAtDestination()) {
                start(value);
            } else {
                this.pending = value;
            }
        }

        private void start(String value) {
            this.previous = this.current;
            this.current = value;
            this.animation.set(0.0f);
            this.animation.destination(1.0f);
        }

        private boolean animating() {
            return !this.animation.isAtDestination() && !Objects.equals(this.previous, this.current);
        }
    }

    private record RollingTextParts(String animated, String suffix) {
    }
}
