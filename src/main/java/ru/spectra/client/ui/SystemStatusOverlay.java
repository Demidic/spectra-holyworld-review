package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.math.DeltaTimeTracker;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.SystemStatusState;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.AnimationStack2;
import ru.spectra.client.render.BlurEffect;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.WeightedEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Fixed top-right service messages. This is not a HUD widget and is never
 * registered in the HUD editor. It renders at the end of every client frame,
 * above the world and every Minecraft/Spectra screen.
 */
public final class SystemStatusOverlay {
    private static final String INFO_GLYPH = "6";
    private static final float RIGHT_MARGIN = 16.0f;
    private static final float TOP_MARGIN = 16.0f;
    private static final float ONE_LINE_CARD_HEIGHT = 48.0f;
    private static final float TWO_LINE_CARD_HEIGHT = 61.0f;
    private static final float CARD_GAP = 6.0f;
    // The former 305 px floor left a large empty tail on short status text.
    // Keep only a structural minimum; normal cards now hug their longest line.
    private static final float MIN_CARD_WIDTH = 160.0f;
    private static final float MAX_CARD_WIDTH = 390.0f;

    private final SystemStatusState state;
    private final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    private final AnimationStack2 animationStack = new AnimationStack2();
    private final BlurEffect blur = new BlurEffect();
    private final AnimatedFloat updateAnimation =
            new AnimatedFloat(240, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat installedAnimation =
            new AnimatedFloat(240, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat connectionAnimation =
            new AnimatedFloat(240, Easings.EASE_IN_OUT_CUBIC);
    private final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.MENU_ICON.get();
    private boolean blurInitialized;
    private boolean blurFailed;

    public SystemStatusOverlay(SystemStatusState state) {
        this.state = state;
    }

    public boolean shouldRender() {
        return showInstalled()
                || showUpdate()
                || this.state.serversUnavailable()
                || !this.installedAnimation.isZero()
                || !this.updateAnimation.isZero()
                || !this.connectionAnimation.isZero();
    }

    /** Draws once at the end of the client frame, above both screens and HUD. */
    public void draw() {
        if (!shouldRender()) {
            return;
        }
        ScreenResolution framebufferResolution = ScreenResolution.resolution();
        if (framebufferResolution.screenWidth() < 64
                || framebufferResolution.screenHeight() < 64) {
            return;
        }

        float scale = WidgetStack.hudScale();
        ScreenResolution logicalResolution = new ScreenResolution(
                Math.max(1, Math.round(framebufferResolution.screenWidth() / scale)),
                Math.max(1, Math.round(framebufferResolution.screenHeight() / scale))
        );
        MatrixStack matrices = new MatrixStack();
        matrices.scale(scale, scale, 1.0f);
        WeightedEngine engine = new WeightedEngine(
                this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        this.animationStack.begin();
        animate(engine);
        this.animationStack.end();

        SurfaceStyle surfaceStyle = Spectra.INSTANCE.configManager()
                .menuStateConfig().hudSurfaceStyle();
        int blurTexture = captureBlur(surfaceStyle);
        SpectraHudStyle.beginFrame(
                blurTexture, surfaceStyle == SurfaceStyle.BLURRED);
        DrawEngine draw = Spectra.INSTANCE.drawEngine();
        draw.begin();
        try {
            render(draw, matrices, logicalResolution);
        } finally {
            if (draw.building) {
                draw.end();
            }
            SpectraHudStyle.endFrame();
        }
    }

    private void animate(WeightedEngine engine) {
        this.installedAnimation.destination(showInstalled() ? 1.0f : 0.0f)
                .animate(engine);
        this.updateAnimation.destination(showUpdate() ? 1.0f : 0.0f).animate(engine);
        this.connectionAnimation
                .destination(this.state.serversUnavailable() ? 1.0f : 0.0f)
                .animate(engine);
    }

    private void render(DrawEngine draw, MatrixStack matrices,
                        ScreenResolution resolution) {
        float y = TOP_MARGIN;
        float installedProgress = this.installedAnimation.animatedValue();
        if (installedProgress > 0.001f) {
            CardText text = installedText();
            renderCard(draw, matrices, resolution, text, y,
                    installedProgress, "T", ThemePalette.deepGreen.argb());
            y += (cardHeight(text) + CARD_GAP) * installedProgress;
        }

        float updateProgress = this.updateAnimation.animatedValue();
        if (updateProgress > 0.001f) {
            CardText text = updateText();
            renderCard(draw, matrices, resolution, text, y, updateProgress,
                    INFO_GLYPH, 0xFFFFFFFF);
            y += (cardHeight(text) + CARD_GAP) * updateProgress;
        }

        float connectionProgress = this.connectionAnimation.animatedValue();
        if (connectionProgress > 0.001f) {
            renderCard(draw, matrices, resolution, connectionText(), y,
                    connectionProgress, INFO_GLYPH, 0xFFFFFFFF);
        }
    }

    private void renderCard(DrawEngine draw, MatrixStack matrices,
                            ScreenResolution resolution, CardText text,
                            float y, float progress, String iconGlyph,
                            int iconColor) {
        float width = cardWidth(text);
        float height = cardHeight(text);
        float x = resolution.screenWidth() - RIGHT_MARGIN - width
                + (1.0f - progress) * 22.0f;
        float animatedY = y - (1.0f - progress) * 5.0f;
        ColorStack colors = draw.colorStack();

        draw.pushVertexAlpha(progress);
        try {
            SpectraHudStyle.outlinedRect(
                    draw, matrices, x, animatedY, width, height,
                    SpectraHudStyle.RADIUS,
                    SpectraHudStyle.color(colors, SpectraHudStyle.OUTER_BORDER),
                    SpectraHudStyle.panelColor(colors, SpectraHudStyle.PANEL_DARK)
            );

            // Plain menu-icon glyph: no tile, fill or outline around it.
            draw.msdfFontVerticalCHorizontalC(
                    matrices.peek().getPositionMatrix(), this.iconFont, iconGlyph,
                    x + 22.0f, animatedY + height / 2.0f,
                    15.0f, 0.05f,
                    SpectraHudStyle.color(colors, iconColor)
            );
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(), this.titleFont, text.title(),
                    x + 43.0f, animatedY + (text.secondLine().isEmpty()
                            ? 7.0f : 9.0f),
                    12.0f, 0.05f,
                    SpectraHudStyle.color(colors, SpectraHudStyle.TEXT)
            );
            int descriptionColor = SpectraHudStyle.color(
                    colors, SpectraHudStyle.MUTED);
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(), this.descriptionFont,
                    text.firstLine(), x + 43.0f,
                    animatedY + (text.secondLine().isEmpty() ? 27.0f : 29.0f),
                    9.5f, 0.05f, descriptionColor
            );
            if (!text.secondLine().isEmpty()) {
                draw.msdfFont(
                        matrices.peek().getPositionMatrix(), this.descriptionFont,
                        text.secondLine(), x + 43.0f, animatedY + 42.0f,
                        9.5f, 0.05f, descriptionColor
                );
            }
        } finally {
            draw.popVertexAlpha();
        }
    }

    /** Prevents HUD editor widgets underneath the fixed card from hovering/dragging. */
    public boolean blocksHudPointer(float mouseX, float mouseY,
                                    ScreenResolution resolution) {
        if (resolution == null || !shouldRender()) {
            return false;
        }
        float y = TOP_MARGIN;
        float installedProgress = this.installedAnimation.animatedValue();
        if (showInstalled() || installedProgress > 0.001f) {
            CardText text = installedText();
            float height = cardHeight(text);
            float width = cardWidth(text);
            float x = resolution.screenWidth() - RIGHT_MARGIN - width;
            if (inside(mouseX, mouseY, x, y, width, height)) {
                return true;
            }
            y += (height + CARD_GAP) * Math.max(installedProgress,
                    showInstalled() ? 1.0f : 0.0f);
        }
        if (showUpdate() || this.updateAnimation.animatedValue() > 0.001f) {
            CardText text = updateText();
            float height = cardHeight(text);
            float width = cardWidth(text);
            float x = resolution.screenWidth() - RIGHT_MARGIN - width;
            if (inside(mouseX, mouseY, x, y, width, height)) {
                return true;
            }
            y += (height + CARD_GAP) * Math.max(
                    this.updateAnimation.animatedValue(),
                    showUpdate() ? 1.0f : 0.0f);
        }
        if (this.state.serversUnavailable()
                || this.connectionAnimation.animatedValue() > 0.001f) {
            CardText text = connectionText();
            float width = cardWidth(text);
            float x = resolution.screenWidth() - RIGHT_MARGIN - width;
            return inside(mouseX, mouseY, x, y, width, cardHeight(text));
        }
        return false;
    }

    public boolean blocksCurrentPointer() {
        return handleCurrentPointerClick(false);
    }

    public boolean handleCurrentPointerClick(boolean primaryButton) {
        ScreenResolution framebufferResolution = ScreenResolution.resolution();
        float scale = WidgetStack.hudScale();
        ScreenResolution logicalResolution = new ScreenResolution(
                Math.max(1, Math.round(framebufferResolution.screenWidth() / scale)),
                Math.max(1, Math.round(framebufferResolution.screenHeight() / scale))
        );
        float mouseX = (float) (Mc.INSTANCE.getMouse().getX() / scale);
        float mouseY = (float) (Mc.INSTANCE.getMouse().getY() / scale);
        if (primaryButton && showInstalled()
                && !this.state.releaseNotesUrl().isBlank()) {
            CardText text = installedText();
            float width = cardWidth(text);
            float x = logicalResolution.screenWidth() - RIGHT_MARGIN - width;
            if (inside(mouseX, mouseY, x, TOP_MARGIN, width,
                    cardHeight(text))) {
                net.minecraft.util.Util.getOperatingSystem().open("https://spectravisuals.su/news");
            }
        }
        return blocksHudPointer(mouseX, mouseY, logicalResolution);
    }

    private float cardWidth(CardText text) {
        float contentWidth = Math.max(
                this.titleFont.getWidth(text.title(), 12.0f),
                Math.max(this.descriptionFont.getWidth(text.firstLine(), 9.5f),
                        this.descriptionFont.getWidth(text.secondLine(), 9.5f))
        );
        return Math.max(MIN_CARD_WIDTH,
                Math.min(MAX_CARD_WIDTH, contentWidth + 56.0f));
    }

    private static float cardHeight(CardText text) {
        return text.secondLine().isEmpty()
                ? ONE_LINE_CARD_HEIGHT : TWO_LINE_CARD_HEIGHT;
    }

    private CardText installedText() {
        boolean linked = !this.state.releaseNotesUrl().isBlank();
        return new CardText(
                ClientLocalization.text(
                        "Update installed",
                        "Обновление установлено"),
                ClientLocalization.text(
                        "Spectra has been updated. Enjoy the game!",
                        "Spectra обновлена. Приятной игры!"),
                linked ? ClientLocalization.text(
                        "Click to view full details.",
                        "Нажмите, чтобы увидеть все подробности.") : ""
        );
    }

    private CardText updateText() {
        return new CardText(
                ClientLocalization.text(
                        "New update available",
                        "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u043e\u0432\u043e\u0435 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435"),
                ClientLocalization.text(
                        "A new update is available to download.",
                        "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u043e\u0432\u043e\u0435 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0434\u043b\u044f \u0441\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u044f."),
                ClientLocalization.text(
                        "Exit and restart the game to install it.",
                        "\u0414\u043b\u044f \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0438 \u0432\u044b\u0439\u0434\u0438\u0442\u0435 \u0438\u0437 \u0438\u0433\u0440\u044b \u0438 \u0437\u0430\u0439\u0434\u0438\u0442\u0435 \u0441\u043d\u043e\u0432\u0430.")
        );
    }

    private CardText connectionText() {
        String title = switch (this.state.connectionIssue()) {
            case TIMEOUT -> ClientLocalization.text("Server response delayed", "Сервер отвечает с задержкой");
            case SERVICE -> ClientLocalization.text("Spectra service unavailable", "Сервис Spectra временно недоступен");
            case RATE_LIMIT -> ClientLocalization.text("Server is limiting requests", "Сервер ограничил частоту запросов");
            case SESSION_EXPIRED -> ClientLocalization.text("Session expired", "Сессия истекла");
            case ACCESS_DENIED -> ClientLocalization.text("Access denied", "Доступ запрещён");
            case TLS, INVALID_RESPONSE -> ClientLocalization.text("Server verification failed", "Ошибка проверки сервера");
            default -> ClientLocalization.text("Connection interrupted", "Соединение прервано");
        };
        String description = this.state.connectionIssue().retryable()
                ? ClientLocalization.text("Reconnecting automatically…", "Автоматически восстанавливаем соединение…")
                : ClientLocalization.text("Sign in again through the launcher.", "Войдите заново через лаунчер.");
        return new CardText(title, description, "");
    }

    private int captureBlur(SurfaceStyle surfaceStyle) {
        if (surfaceStyle != SurfaceStyle.BLURRED || this.blurFailed) {
            return -1;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            if (!this.blurInitialized) {
                this.blur.init();
                this.blurInitialized = true;
            }
            this.blur.apply(28, 2.0f);
            Framebuffer blurred = this.blur.getBlurFramebuffer();
            return blurred == null ? -1 : blurred.getColorAttachment();
        } catch (RuntimeException | LinkageError error) {
            this.blurFailed = true;
            Spectra.LOGGER.error(
                    "System status blur pass failed; disabling it safely", error);
            return -1;
        } finally {
            client.getFramebuffer().beginWrite(true);
        }
    }

    private boolean showUpdate() {
        return this.state.updateAvailable();
    }

    private boolean showInstalled() {
        return this.state.updateInstalled();
    }

    private static boolean inside(float pointX, float pointY,
                                  float x, float y, float width, float height) {
        return pointX >= x && pointX <= x + width
                && pointY >= y && pointY <= y + height;
    }

    private record CardText(String title, String firstLine, String secondLine) {
    }
}
