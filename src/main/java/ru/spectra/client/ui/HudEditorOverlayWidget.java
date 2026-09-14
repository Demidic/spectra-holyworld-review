package ru.spectra.client.ui;

import ru.spectra.client.module.HudModules;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.math.Easings;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.ConfigAutoSaveScheduler;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;
import net.minecraft.client.util.math.MatrixStack;
import ru.spectra.client.render.ScreenResolution;

public final class HudEditorOverlayWidget extends Draggable {
    public enum Kind {
        BOSS_BAR,
        SCOREBOARD,
        PLAYER_LIST
    }

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float SCALE_STEP = 0.1f;
    private static final float PANEL_WIDTH = 154.0f;
    private static final float SLIDER_GAP = 8.0f;
    private static final float SLIDER_HEIGHT = 30.0f;
    private static final float TOGGLE_ROW_HEIGHT = 22.0f;

    private final Kind kind;
    private final NumberSetting scale;
    private final BooleanSetting customized;
    private final BooleanSetting removeBackground;
    private final AnimatedFloat scaleAnimation = new AnimatedFloat(150, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator removeBackgroundAnimation = new ToggleAnimator(170, Easings.EASE_IN_OUT_CUBIC);
    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    private boolean initialized;
    private boolean scaling;
    private float pendingScale = Float.NaN;
    private long lastScaleMoveAt;
    private boolean scaleDirty;

    HudEditorOverlayWidget(Kind kind) {
        super(switch (kind) {
            case BOSS_BAR -> "Boss Bar";
            case SCOREBOARD -> "Scoreboard";
            case PLAYER_LIST -> "Player List";
        }, kind == Kind.PLAYER_LIST
                ? HudEditorScreen::isPlayerListPreviewVisible
                : HudEditorScreen::isAdvancedOpen);
        this.kind = kind;
        this.scale = new NumberSetting(Translation.clearText("Scale"))
                .currentValue(1.0f)
                .range(MIN_SCALE, MAX_SCALE)
                .step(SCALE_STEP);
        this.customized = new BooleanSetting(Translation.clearText("Custom placement")).setValue(false);
        this.removeBackground = new BooleanSetting(Translation.clearText("Remove background")).setValue(false);
        this.scaleAnimation.set(1.0f);
        addSettings(this.scale);
        if (kind != Kind.PLAYER_LIST) {
            addSettings(this.customized);
        }
        if (kind != Kind.BOSS_BAR) {
            addSettings(this.removeBackground);
        }
        setExcludeFromSnapGrid(false);
    }

    @Override
    public void layout(DragRenderContext context) {
        layoutForResolution(context.resolution());
    }

    public void refreshRuntimeLayout() {
        ScreenResolution physical = ScreenResolution.resolution();
        if (physical.screenWidth() < 64 || physical.screenHeight() < 64) {
            return;
        }
        ScreenResolution logical = new ScreenResolution(
                Math.max(1, Math.round(physical.screenWidth() / WidgetStack.HUD_SCALE)),
                Math.max(1, Math.round(physical.screenHeight() / WidgetStack.HUD_SCALE))
        );
        layoutForResolution(logical);
    }

    private void layoutForResolution(ScreenResolution currentResolution) {
        this.resolution = currentResolution;
        if (this.kind == Kind.PLAYER_LIST) {
            this.x = (currentResolution.screenWidth() - width()) / 2.0f;
            this.y = vanillaLogical(HudVanillaPreviewRenderer.PLAYER_LIST_TOP);
            this.initialized = true;
            return;
        }
        // Match every regular HUD widget: once a custom position exists, layout
        // never rewrites x/y. In particular, ChatScreen must not reinterpret a
        // resize as a request to re-anchor the scoreboard to the right edge.
        if (!this.initialized || !this.customized.isValue()) {
            placeAtDefault(currentResolution);
            this.initialized = true;
        }
    }

    private void placeAtDefault(ScreenResolution currentResolution) {
        if (this.kind == Kind.BOSS_BAR) {
            this.x = (currentResolution.screenWidth() - width()) / 2.0f;
            this.y = vanillaLogical(3.0f + HudModules.watermarkBossBarOffset());
            return;
        }
        this.x = currentResolution.screenWidth() - width() - vanillaLogical(3.0f);
        this.y = (currentResolution.screenHeight() - contentHeight()) / 2.0f;
    }

    @Override
    public void render(DragRenderContext context) {
        if (this.scaling && this.scaleDirty
                && System.currentTimeMillis() - this.lastScaleMoveAt >= 1000L) {
            applyPendingScale();
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        float panelX = panelX();
        float panelY = sliderY();
        float panelWidth = PANEL_WIDTH;
        float panelHeight = settingsPanelHeight();
        int accent = context.theme().palette().accentBright().argb();

        SpectraHudStyle.outlinedRect(
                draw, matrices, panelX, panelY, panelWidth, panelHeight, 8.0f,
                SpectraHudStyle.BORDER, 0xE80F0F12
        );
        float displayedScale = scale();
        String percent = Math.round(displayedScale * 100.0f) + "%";
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.font, "Scale",
                panelX + 8.0f, panelY + 5.0f, 10.0f, 0.05f, SpectraHudStyle.TEXT
        );
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.font, percent,
                panelX + panelWidth - 8.0f - this.font.getWidth(percent, 10.0f),
                panelY + 5.0f, 10.0f, 0.05f, SpectraHudStyle.MUTED
        );

        float trackX = panelX + 8.0f;
        float trackY = panelY + 22.0f;
        float trackWidth = panelWidth - 16.0f;
        draw.roundedRectangle(
                matrices.peek().getPositionMatrix(), trackX, trackY,
                trackWidth, 3.0f, 1.5f, 0xFF29292F
        );
        float progress = (displayedScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        float fillWidth = Math.max(3.0f, trackWidth * progress);
        draw.roundedRectangle(
                matrices.peek().getPositionMatrix(), trackX, trackY,
                fillWidth, 3.0f, 1.5f, accent
        );
        draw.circle(
                matrices.peek().getPositionMatrix(),
                trackX + trackWidth * progress, trackY + 1.5f,
                4.0f, 0xFFFFFFFF
        );
        float settingY = panelY + SLIDER_HEIGHT;
        if (this.kind != Kind.BOSS_BAR) {
            renderToggleRow(draw, matrices, panelX, settingY, panelWidth,
                    ClientLocalization.text("Remove background", "\u0423\u0431\u0440\u0430\u0442\u044C \u0444\u043E\u043D"),
                    this.removeBackgroundAnimation.smoothAnimation(), accent);
        }
    }

    private void renderToggleRow(
            DrawEngine draw, MatrixStack matrices, float panelX, float rowY,
            float panelWidth, String label, float progress, int accent) {
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.font, label,
                panelX + 8.0f, rowY + 5.5f, 9.5f, 0.05f, SpectraHudStyle.TEXT
        );
        float switchWidth = 24.0f;
        float switchHeight = 12.0f;
        float switchX = panelX + panelWidth - switchWidth - 8.0f;
        float switchY = rowY + 5.0f;
        draw.roundedRectangle(
                matrices.peek().getPositionMatrix(), switchX, switchY,
                switchWidth, switchHeight, 6.0f,
                draw.colorStack().interpolate(0xFF29292F, accent, progress)
        );
        draw.circle(
                matrices.peek().getPositionMatrix(),
                switchX + 6.0f + (switchWidth - 12.0f) * progress,
                switchY + 6.0f, 4.5f, 0xFFFFFFFF
        );
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        if (input.release() && this.scaling) {
            applyPendingScale();
            this.scaling = false;
            return true;
        }
        if (!hovered || !input.press() || !input.isLeftButtonPressed()) {
            return false;
        }
        if (inside(input.mouseX(), input.mouseY(), panelX(), sliderY(), PANEL_WIDTH, SLIDER_HEIGHT)) {
            this.scaling = true;
            this.customized.setValue(true);
            updatePendingScale(input.mouseX());
            return true;
        }
        float settingY = sliderY() + SLIDER_HEIGHT;
        if (this.kind != Kind.BOSS_BAR
                && inside(input.mouseX(), input.mouseY(), panelX(), settingY, PANEL_WIDTH, TOGGLE_ROW_HEIGHT)) {
            this.removeBackground.switchValue();
            ConfigAutoSaveScheduler.scheduleAutoSave();
            return true;
        }
        if (this.kind == Kind.PLAYER_LIST) {
            return true;
        }
        this.customized.setValue(true);
        return false;
    }

    private void updatePendingScale(float mouseX) {
        float trackX = panelX() + 8.0f;
        float trackWidth = PANEL_WIDTH - 16.0f;
        float progress = MathUtil.clamp((mouseX - trackX) / trackWidth, 0.0f, 1.0f);
        float next = MathUtil.clamp(MIN_SCALE + progress * (MAX_SCALE - MIN_SCALE), MIN_SCALE, MAX_SCALE);
        if (Float.isFinite(this.pendingScale)
                && Math.abs(this.pendingScale - next) < 0.0001f) {
            return;
        }
        this.pendingScale = next;
        this.lastScaleMoveAt = System.currentTimeMillis();
        this.scaleDirty = true;
    }

    private float targetScale() {
        return this.scaling && Float.isFinite(this.pendingScale)
                ? this.pendingScale
                : this.scale.currentValue();
    }

    private void applyPendingScale() {
        if (!this.scaleDirty || !Float.isFinite(this.pendingScale)) {
            return;
        }
        float stepped = Math.round(this.pendingScale / SCALE_STEP) * SCALE_STEP;
        this.scale.setCurrentValue(MathUtil.clamp(stepped, MIN_SCALE, MAX_SCALE));
        this.scaleDirty = false;
        ConfigAutoSaveScheduler.scheduleAutoSave();
    }

    private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        if (this.scaling) {
            updatePendingScale((float) (input.mouse().getX() / input.scaleFactor()));
            return true;
        }
        return hovered;
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.scaleAnimation.destination(targetScale()).animate(engine);
        this.removeBackgroundAnimation
                .state(this.removeBackground.isValue())
                .animate(engine);
    }

    @Override
    public void update() {
    }

    @Override
    public float width() {
        return Math.max(PANEL_WIDTH, contentWidth());
    }

    @Override
    public float height() {
        return contentHeight() + SLIDER_GAP + settingsPanelHeight();
    }

    private float settingsPanelHeight() {
        return SLIDER_HEIGHT
                + (this.kind == Kind.BOSS_BAR ? 0.0f : TOGGLE_ROW_HEIGHT);
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public void renderEditOutline(DragRenderContext context, float animation) {
        if (animation <= 0.01f || !isVisible()) {
            return;
        }
        renderEditOutlineBounds(
                context, animation,
                contentX(), contentY(), contentWidth(), contentHeight()
        );
    }

    @Override
    public void applySavedPosition(float x, float y) {
        super.applySavedPosition(x, y);
        this.initialized = true;
    }

    public boolean customized() {
        return this.kind == Kind.PLAYER_LIST
                || (this.initialized && this.customized.isValue());
    }

    public float scale() {
        return HudEditorScreen.isAdvancedOpen()
                ? this.scaleAnimation.animatedValue()
                : this.scale.currentValue();
    }

    public Kind kind() {
        return this.kind;
    }

    public float contentX() {
        return this.x + (width() - contentWidth()) / 2.0f;
    }

    public float contentY() {
        return this.y;
    }

    public float contentWidth() {
        return baseContentWidth() * scale();
    }

    public float contentHeight() {
        return baseContentHeight() * scale();
    }

    private float sliderY() {
        return this.y + contentHeight() + SLIDER_GAP;
    }

    private float panelX() {
        return this.x + (width() - PANEL_WIDTH) / 2.0f;
    }

    private float baseContentWidth() {
        return switch (this.kind) {
            case BOSS_BAR -> vanillaLogical(HudVanillaPreviewRenderer.BOSS_BAR_WIDTH);
            case SCOREBOARD -> vanillaLogical(HudVanillaPreviewRenderer.scoreboardWidth());
            case PLAYER_LIST -> vanillaLogical(HudVanillaPreviewRenderer.PLAYER_LIST_WIDTH);
        };
    }

    private float baseContentHeight() {
        return switch (this.kind) {
            case BOSS_BAR -> vanillaLogical(HudVanillaPreviewRenderer.bossBarHeight());
            case SCOREBOARD -> vanillaLogical(HudVanillaPreviewRenderer.scoreboardHeight());
            case PLAYER_LIST -> vanillaLogical(HudVanillaPreviewRenderer.PLAYER_LIST_HEIGHT);
        };
    }

    public boolean showBackground() {
        return this.kind == Kind.BOSS_BAR || !this.removeBackground.isValue();
    }

    private static float vanillaLogical(float guiPixels) {
        double guiScale = Math.max(1.0d, Mc.INSTANCE.getWindow().getScaleFactor());
        return (float) (guiPixels * guiScale / WidgetStack.HUD_SCALE);
    }
}
