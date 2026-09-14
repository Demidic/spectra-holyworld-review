package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.model.WidgetBounds;
import ru.spectra.client.net.UserSession;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.util.SubscriptionFormatter;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.Locale;

public final class ProfilePopupContent implements PopupContent {
    private static final float AVATAR_SIZE = 46.0f;
    private static final float ROW_HEIGHT = 26.0f;
    private static final float SOCIAL_SIZE = 22.0f;
    static final String TELEGRAM_GLYPH = "\u0448";
    // The Discord drawing is mapped to lowercase Latin i in this icon atlas.
    // Uppercase I has no glyph and therefore renders nothing.
    static final String DISCORD_GLYPH = "i";
    private static final String TELEGRAM_URL = "https://t.me/spectravisuals";
    private static final String DISCORD_URL = "https://discord.gg/gPQ7yBY9zy";

    private final MsdfFont titleFont = Fonts.INTER_BOLD.get();
    private final MsdfFont textFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont iconFont = Fonts.MENU_ICON.get();
    private final ClickableBehavior telegramButton = new ClickableBehavior()
            .clickCallback(() -> openExternal(TELEGRAM_URL));
    private final ClickableBehavior discordButton = new ClickableBehavior()
            .clickCallback(() -> openExternal(DISCORD_URL));

    @Override
    public float preferredWidth() {
        return 248.0f;
    }

    @Override
    public float preferredHeight() {
        return 250.0f;
    }

    @Override
    public boolean showCloseButton() {
        return false;
    }

    @Override
    public void layout(LayoutScaleContext context, WidgetBounds bounds) {
        float footerY = bounds.y() + 194.0f;
        float right = bounds.x() + bounds.width();
        this.discordButton.setDimensions(
                right - SOCIAL_SIZE, footerY, SOCIAL_SIZE, SOCIAL_SIZE);
        this.telegramButton.setDimensions(
                right - SOCIAL_SIZE * 2.0f - 4.0f,
                footerY, SOCIAL_SIZE, SOCIAL_SIZE);
    }

    @Override
    public void render(DrawCtx context, WidgetBounds bounds) {
        UserSession session = Spectra.INSTANCE.userSession();
        ColorStack colors = context.colorStack();
        float x = bounds.x();
        float y = bounds.y();

        context.text(this.textFont,
                ClientLocalization.text("ACCOUNT PROFILE", "ПРОФИЛЬ АККАУНТА"),
                9, x, y + 1.0f, colors.computeColor(0xFF777883));

        float avatarY = y + 20.0f;
        if (session.avatarUrl() == null || session.avatarUrl().isBlank()) {
            context.fillRoundedRect(x, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    13.0f, colors.computeColor(0xFF17171B));
            String glyph = "4";
            float glyphWidth = context.textWidthPhysical(this.iconFont, glyph, 31);
            context.text(this.iconFont, glyph, 31,
                    x + (AVATAR_SIZE - glyphWidth) / 2.0f,
                    avatarY + (AVATAR_SIZE - this.iconFont.getHeight(31.0f)) / 2.0f,
                    colors.computeColor(0xFF696A75));
        } else {
            GlTexture avatar = session.texture();
            avatar.magFilter(9729);
            avatar.minFilter(9729);
            context.roundedTexture(avatar, x, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    13.0f, colors.white());
        }

        context.text(this.titleFont, session.username(), 15,
                x + AVATAR_SIZE + 12.0f, avatarY + 6.0f,
                colors.computeColor(0xFFF4F4F7));
        String role = displayRole(session.role());
        float roleWidth = context.textWidthPhysical(this.textFont, role, 9);
        float roleX = x + AVATAR_SIZE + 12.0f;
        float roleY = avatarY + 27.0f;
        context.fillRoundedRect(roleX, roleY, roleWidth + 14.0f, 18.0f,
                6.0f, colors.computeColor(
                        Spectra.INSTANCE.theme().palette().accent().argb(), 0.16f));
        context.text(this.textFont, role, 9, roleX + 7.0f,
                roleY + (18.0f - this.textFont.getHeight(9.0f)) / 2.0f,
                colors.computeColor(Spectra.INSTANCE.theme().palette().accentBright().argb()));

        float dividerY = y + 76.0f;
        context.fillRect(x, dividerY, bounds.width(), 1.0f,
                colors.computeColor(0xFF24252A));
        float rowY = dividerY + 3.0f;
        renderRow(context, x, rowY, bounds.width(),
                ClientLocalization.text("UID", "UID"), session.uid(), false);
        renderRow(context, x, rowY + ROW_HEIGHT, bounds.width(),
                ClientLocalization.text("Role", "Роль"), role, false);
        renderRow(context, x, rowY + ROW_HEIGHT * 2.0f, bounds.width(),
                ClientLocalization.text("Subscription", "Подписка"),
                SubscriptionFormatter.display(session.expire()), false);
        renderRow(context, x, rowY + ROW_HEIGHT * 3.0f, bounds.width(),
                ClientLocalization.text("Played", "Наиграно"),
                formatPlaytime(session.playedSeconds()), true);

        float footerDividerY = y + 187.0f;
        context.fillRect(x, footerDividerY, bounds.width(), 1.0f,
                colors.computeColor(0xFF24252A));
        renderFooter(context, bounds, footerDividerY + 7.0f);
    }

    private void renderRow(DrawCtx context, float x, float y, float width,
                           String label, String value, boolean last) {
        ColorStack colors = context.colorStack();
        float textY = y + (ROW_HEIGHT - this.textFont.getHeight(10.0f)) / 2.0f;
        context.text(this.textFont, label, 10, x + 1.0f, textY,
                colors.computeColor(0xFF7C7D88));
        String safeValue = value == null || value.isBlank() ? "—" : value;
        float valueLimit = width - this.textFont.getWidth(label, 10.0f) - 24.0f;
        safeValue = fit(safeValue, valueLimit);
        float valueWidth = context.textWidthPhysical(this.textFont, safeValue, 10);
        context.text(this.textFont, safeValue, 10, x + width - valueWidth - 1.0f, textY,
                colors.computeColor(0xFFE8E8EC));
        if (!last) {
            context.fillRect(x, y + ROW_HEIGHT - 1.0f, width, 1.0f,
                    colors.computeColor(0xFF222329));
        }
    }

    private void renderFooter(DrawCtx context, WidgetBounds bounds, float footerY) {
        ColorStack colors = context.colorStack();
        String version = currentVersion();
        float versionY = footerY + (SOCIAL_SIZE - this.textFont.getHeight(9.0f)) / 2.0f;
        context.text(this.textFont, version, 9, bounds.x() + 1.0f, versionY,
                colors.computeColor(0xFF62636D));
        renderSocialButton(context, this.telegramButton, TELEGRAM_GLYPH, 12.0f);
        renderSocialButton(context, this.discordButton, DISCORD_GLYPH, 13.0f);
    }

    private void renderSocialButton(DrawCtx context, ClickableBehavior button,
                                    String glyph, float glyphSize) {
        ColorStack colors = context.colorStack();
        float hover = button.hoverAnimation().smoothAnimation();
        if (hover > 0.01f) {
            context.fillRoundedRect(button.ownerX(), button.ownerY(),
                    button.ownerW(), button.ownerH(), 6.0f,
                    colors.computeColor(0xFFFFFFFF, 0.055f * hover));
        }
        int color = colors.computeColor(hover > 0.01f ? 0xFFD7D7DD : 0xFF858690);
        float glyphWidth = context.textWidthPhysical(this.iconFont, glyph,
                Math.round(glyphSize));
        context.text(this.iconFont, glyph, Math.round(glyphSize),
                button.ownerX() + (button.ownerW() - glyphWidth) / 2.0f,
                button.ownerY() + (button.ownerH() - this.iconFont.getHeight(glyphSize)) / 2.0f,
                color);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed,
                               WidgetBounds contentBounds) {
        if (this.discordButton.handleInput(context, consumed)) {
            return true;
        }
        return this.telegramButton.handleInput(context, consumed);
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.telegramButton.animate(engine);
        this.discordButton.animate(engine);
    }

    private String fit(String value, float maxWidth) {
        if (this.textFont.getWidth(value, 10.0f) <= maxWidth) {
            return value;
        }
        int length = value.length();
        while (length > 1
                && this.textFont.getWidth(value.substring(0, length) + "...", 10.0f)
                > maxWidth) {
            length--;
        }
        return value.substring(0, length) + "...";
    }

    private static String displayRole(String role) {
        String normalized = role == null ? "user" : role.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "developer", "admin", "head_admin" ->
                    ClientLocalization.text("Administrator", "Администратор");
            case "moderator", "support" ->
                    ClientLocalization.text("Moderator", "Модератор");
            default -> ClientLocalization.text("User", "Пользователь");
        };
    }

    static String formatPlaytime(long seconds) {
        long safe = Math.max(0L, seconds);
        long tenths = Math.round(safe / 360.0d);
        String hours = tenths % 10L == 0L
                ? Long.toString(tenths / 10L)
                : tenths / 10L + "." + Math.abs(tenths % 10L);
        return ClientLocalization.text(hours + " h", hours.replace('.', ',') + " ч");
    }

    private static String currentVersion() {
        String version = Spectra.INSTANCE.clientInfo().version();
        if (version == null || version.isBlank()) {
            return "Spectra";
        }
        return version.startsWith("v") ? version : "v" + version;
    }

    private static void openExternal(String url) {
        Util.getOperatingSystem().open(URI.create(url));
    }
}
