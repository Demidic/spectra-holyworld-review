package ru.spectra.client.ui;
import ru.spectra.client.render.AnimationStack2;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.BlurEffect;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.SoftVisibilityTransition;
import ru.spectra.client.render.SvgTexture;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.math.Easings;
import ru.spectra.client.type.ButtonAction;
import ru.spectra.client.math.DeltaTimeTracker;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.FramebufferUtil;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.event.MouseButtonEvent2;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.module.HudModules;
import ru.spectra.client.util.ConfigAutoSaveScheduler;
import ru.spectra.client.util.SnapManager;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WidgetStack {
    private static final int EDITOR_HINT_RGB = 0xE5E5EA;
    private static final int EDITOR_HINT_ALPHA = 205;
    private static String freeDragHint() {
        return ClientLocalization.text("Hold CTRL while dragging to move freely",
                "\u0423\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0439\u0442\u0435 CTRL \u043F\u0440\u0438 \u043F\u0435\u0440\u0435\u0442\u0430\u0441\u043A\u0438\u0432\u0430\u043D\u0438\u0438 \u0434\u043B\u044F \u0441\u0432\u043E\u0431\u043E\u0434\u043D\u043E\u0433\u043E \u0434\u0432\u0438\u0436\u0435\u043D\u0438\u044F");
    }

    private static String advancedLabel() {
        return ClientLocalization.text("Open Advanced mode", "\u041E\u0442\u043A\u0440\u044B\u0442\u044C \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043D\u043D\u044B\u0439 \u0440\u0435\u0436\u0438\u043C");
    }
    /**
     * WebHUD was authored at a 125% interface scale. Keeping the HUD on this
     * fixed logical scale makes its dimensions independent from Minecraft's GUI
     * scale and matches the original layout on every supported resolution.
     */
    public static final float BASE_HUD_SCALE = 1.25f;
    public static float HUD_SCALE = BASE_HUD_SCALE;
    public ScreenResolution resolution;
    public final List<Draggable> widgets = new ArrayList();
    public final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    public final AnimationStack2 animationStack = new AnimationStack2();
    private final BlurEffect hudBlur = new BlurEffect();
    private final Matrix4f visibilityTransitionMatrix = new Matrix4f();
    private final Matrix4f visibilityCompositeMatrix = new Matrix4f();
    private final Vector4f visibilityTopLeft = new Vector4f();
    private final Vector4f visibilityBottomRight = new Vector4f();
    private Framebuffer visibilityTransitionFramebuffer;
    private boolean visibilityTransitionFailed;
    private boolean visibilityCaptureActive;
    public final SnapManager snapManager = new SnapManager();
    public final AnimatedFloat editOutlineAnimation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat gridAnimation = new AnimatedFloat(220, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat exitHoverAnimation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat addWidgetMenuAnimation = new AnimatedFloat(190, Easings.EASE_IN_OUT_CUBIC);
    private final MsdfFont addMenuTitleFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont addMenuTextFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont addMenuIconFont = Fonts.MENU_ICON.get();
    private final TextInputField addMenuSearch = new TextInputField(this.addMenuTextFont, 11);
    private final Map<Draggable, AnimatedFloat> playerListOcclusionAnimations = new IdentityHashMap<>();
    private final SvgTexture gridIcon = new SvgTexture(
            new ClasspathResource("/icons/hud/editor/grid.svg"), 128, 128
    );
    private final SvgTexture advancedIcon = new SvgTexture(
            new ClasspathResource("/icons/hud/editor/advanced.svg"), 128, 128
    );
    private final SvgTexture exitIcon = new SvgTexture(
            new ClasspathResource("/icons/hud/editor/exit.svg"), 128, 128
    );
    private boolean gridEnabled;
    private boolean addWidgetMenuOpen;
    private boolean hudBlurInitialized;
    private boolean hudBlurFailed;
    private float addWidgetAnchorX;
    private float addWidgetAnchorY;
    private float addWidgetMenuX;
    private float addWidgetMenuY;
    public Draggable activeWidget = null;

    public static void setHudScale(float scale) {
        HUD_SCALE = MathUtil.clamp(scale, 0.75f, 2.0f);
    }

    public static float hudScale() {
        return HUD_SCALE;
    }

    public void draw() {
        Mc class815Var = Mc.INSTANCE;
        ScreenResolution class710VarResolution = ScreenResolution.resolution();
        // GLFW reports a zero-sized framebuffer while the window is minimized.
        // Never let that transient state rewrite HUD anchors.
        if (class710VarResolution.screenWidth() < 64
                || class710VarResolution.screenHeight() < 64) {
            return;
        }
        float fMax = HUD_SCALE;
        this.resolution = new ScreenResolution(Math.max(1, Math.round(class710VarResolution.screenWidth() / fMax)), Math.max(1, Math.round(class710VarResolution.screenHeight() / fMax)));
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.scale(fMax, fMax, 1.0f);
        float entrance = HudEditorScreen.isAdvancedOpen()
                ? HudEditorScreen.entranceProgress() : 1.0f;
        if (entrance < 0.999f) {
            float entranceScale = 0.94f + entrance * 0.06f;
            float centerX = this.resolution.screenWidth() / 2.0f;
            float centerY = this.resolution.screenHeight() / 2.0f;
            matrixStack.translate(centerX, centerY, 0.0f);
            matrixStack.scale(entranceScale, entranceScale, 1.0f);
            matrixStack.translate(-centerX, -centerY, 0.0f);
        }
        WeightedEngine class141Var = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        Mouse mouse = class815Var.getMouse();
        for (Draggable class806Var : this.widgets) {
            class806Var.setSnapGrid(this.snapManager);
            class806Var.setSiblingWidgets(this.widgets);
        }
        if (HudEditorScreen.isEditing()) {
            boolean z = Spectra.INSTANCE.systemStatusOverlay().blocksHudPointer(
                    (float) (mouse.getX() / fMax),
                    (float) (mouse.getY() / fMax), this.resolution);
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                if (this.widgets.get(size).onCursor(new MouseMoveInput(z, mouse, this.resolution, fMax))) {
                    z = true;
                }
            }
        } else {
            this.widgets.forEach((v0) -> {
                v0.resetCursor();
            });
            this.snapManager.clear();
        }
        boolean zMethod005 = isAnyDragging();
        this.snapManager.animate(class141Var, zMethod005);
        this.animationStack.begin();
        boolean chatEditing = HudEditorScreen.isEditing()
                && this.widgets.stream().anyMatch(Draggable::isVisible);
        this.editOutlineAnimation
                .destination(chatEditing ? 1.0f : 0.0f)
                .animate(class141Var);
        this.gridAnimation
                .destination(this.gridEnabled && HudEditorScreen.isEditing() ? 1.0f : 0.0f)
                .animate(class141Var);
        this.exitHoverAnimation
                .destination(HudEditorScreen.isAdvancedOpen() && isAdvancedExitHovered(mouse)
                        ? 1.0f : 0.0f)
                .animate(class141Var);
        this.addWidgetMenuAnimation
                .destination(this.addWidgetMenuOpen && HudEditorScreen.isAdvancedOpen()
                        ? 1.0f : 0.0f)
                .animate(class141Var);
        this.addMenuSearch.animate(class141Var);
        this.widgets.forEach(widget -> {
            widget.animation(class141Var);
            playerListOcclusionAnimation(widget)
                    .destination(isOccludedByPlayerList(widget) ? 0.12f : 1.0f)
                    .animate(class141Var);
        });
        this.animationStack.end();
        DrawEngine class154VarDrawEngine = Spectra.INSTANCE.drawEngine();
        DragRenderContext class809Var = new DragRenderContext(this.resolution, mouse, matrixStack, class154VarDrawEngine, Spectra.INSTANCE.theme(), fMax);
        SurfaceStyle hudSurfaceStyle = Spectra.INSTANCE.configManager()
                .menuStateConfig().hudSurfaceStyle();
        int hudBlurTexture = captureHudBlur(hudSurfaceStyle);
        SpectraHudStyle.beginFrame(
                hudBlurTexture,
                hudSurfaceStyle == SurfaceStyle.BLURRED
        );
        class154VarDrawEngine.begin();
        boolean entranceFade = HudEditorScreen.isAdvancedOpen() && entrance < 0.999f;
        if (entranceFade) {
            class154VarDrawEngine.pushVertexAlpha(entrance);
        }
        try {
            renderGrid(class154VarDrawEngine, matrixStack);
            for (int index = 0; index < this.widgets.size(); index++) {
                Draggable widget = this.widgets.get(index);
                widget.layout(class809Var);
                boolean composited = !entranceFade
                        && renderWidgetVisibilityTransition(
                        class154VarDrawEngine, class809Var, widget, index);
                if (!composited) {
                    pushWidgetAlpha(class154VarDrawEngine, widget, index);
                    try {
                        widget.draw(class809Var);
                    } finally {
                        class154VarDrawEngine.popVertexAlpha();
                    }
                }
            }
            for (int index = 0; index < this.widgets.size(); index++) {
                Draggable widget = this.widgets.get(index);
                pushWidgetAlpha(class154VarDrawEngine, widget, index);
                try {
                    widget.renderEditOutline(class809Var, this.editOutlineAnimation.animatedValue());
                } finally {
                    class154VarDrawEngine.popVertexAlpha();
                }
            }
            for (int index = 0; index < this.widgets.size(); index++) {
                Draggable widget = this.widgets.get(index);
                pushWidgetAlpha(class154VarDrawEngine, widget, index);
                try {
                    if (widget.hasTooltip()) {
                        widget.drawTooltip(class809Var);
                    }
                } finally {
                    class154VarDrawEngine.popVertexAlpha();
                }
                widget.updateDrag(class809Var);
            }
            if (zMethod005) {
                this.snapManager.render(class809Var);
            }
            renderFreeDragHint(class154VarDrawEngine, matrixStack, this.editOutlineAnimation.animatedValue());
            renderEditorControls(class154VarDrawEngine, matrixStack,
                    this.editOutlineAnimation.animatedValue());
            renderAdvancedStatus(class154VarDrawEngine, matrixStack);
            renderAddWidgetMenu(class154VarDrawEngine, matrixStack);
        } finally {
            if (entranceFade) {
                class154VarDrawEngine.popVertexAlpha();
            }
            if (class154VarDrawEngine.building) {
                class154VarDrawEngine.end();
            }
            SpectraHudStyle.endFrame();
        }
    }

    private int captureHudBlur(SurfaceStyle surfaceStyle) {
        if (surfaceStyle != SurfaceStyle.BLURRED) {
            return -1;
        }
        if (this.hudBlurFailed) {
            return -1;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            if (!this.hudBlurInitialized) {
                this.hudBlur.init();
                this.hudBlurInitialized = true;
            }
            this.hudBlur.apply(28, 2.0f);
            Framebuffer blurred = this.hudBlur.getBlurFramebuffer();
            return blurred == null ? -1 : blurred.getColorAttachment();
        } catch (RuntimeException | LinkageError error) {
            this.hudBlurFailed = true;
            Spectra.LOGGER.error("HUD blur framebuffer pass failed; disabling the effect safely", error);
            return -1;
        } finally {
            client.getFramebuffer().beginWrite(true);
        }
    }

    public void warmUpVisibilityTransitionFramebuffer() {
        ensureVisibilityTransitionFramebuffer();
        this.gridIcon.id();
        this.advancedIcon.id();
        this.exitIcon.id();
        // Chat/HUD editor can be the first use of the blurred HUD surface.
        // Allocating only the composite target leaves shader linking on that frame.
        if (!this.hudBlurInitialized && !this.hudBlurFailed) {
            try {
                this.hudBlur.warmUp();
                this.hudBlurInitialized = true;
            } catch (RuntimeException | LinkageError error) {
                this.hudBlurFailed = true;
                Spectra.LOGGER.error("HUD blur warm-up failed; disabling the effect safely", error);
            }
        }
    }

    private boolean renderWidgetVisibilityTransition(
            DrawEngine draw, DragRenderContext context, Draggable widget, int index) {
        float progress = widget.softVisibilityProgress();
        if (!widget.isVisible()
                || widget.width() <= 0.0f
                || widget.height() <= 0.0f
                || !SoftVisibilityTransition.needsComposite(progress)) {
            return false;
        }
        draw.draw();
        if (!ensureVisibilityTransitionFramebuffer()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        try {
            FramebufferUtil.clearTransparent(this.visibilityTransitionFramebuffer);
            this.visibilityTransitionFramebuffer.beginWrite(true);
            // Capture at the widget's natural size. Scaling the finished texture
            // keeps immediate renderers (entity portraits, vanilla previews)
            // aligned with shapes emitted through DrawEngine.
            this.visibilityCaptureActive = true;
            try {
                widget.draw(context);
                draw.draw();
            } finally {
                this.visibilityCaptureActive = false;
            }
            this.visibilityTransitionFramebuffer.endWrite();
            client.getFramebuffer().beginWrite(true);

            pushWidgetAlpha(draw, widget, index);
            try {
                compositeVisibilityTransition(draw, context, widget, progress);
            } finally {
                draw.popVertexAlpha();
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            try {
                draw.draw();
            } catch (RuntimeException | LinkageError ignored) {
                // The direct render fallback below keeps the remaining HUD usable.
            }
            disableVisibilityTransition(error);
            return false;
        } finally {
            client.getFramebuffer().beginWrite(true);
        }
    }

    private boolean ensureVisibilityTransitionFramebuffer() {
        if (this.visibilityTransitionFailed) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return false;
        }
        try {
            this.visibilityTransitionFramebuffer = FramebufferUtil.ensureFramebuffer(
                    this.visibilityTransitionFramebuffer,
                    framebufferWidth,
                    framebufferHeight,
                    () -> new SimpleFramebuffer(framebufferWidth, framebufferHeight, false)
            );
            FramebufferUtil.resizeIfNeeded(
                    this.visibilityTransitionFramebuffer, framebufferWidth, framebufferHeight);
            return true;
        } catch (RuntimeException | LinkageError error) {
            disableVisibilityTransition(error);
            return false;
        } finally {
            client.getFramebuffer().beginWrite(true);
        }
    }

    private void compositeVisibilityTransition(
            DrawEngine draw, DragRenderContext context, Draggable widget,
            float progress) {
        MinecraftClient client = MinecraftClient.getInstance();
        float blurRadius = SoftVisibilityTransition.blurRadius(progress);
        Matrix4f sourceMatrix = this.visibilityTransitionMatrix
                .set(context.matrixStack().peek().getPositionMatrix());
        sourceMatrix.transform(this.visibilityTopLeft.set(
                widget.x, widget.y, 0.0f, 1.0f));
        sourceMatrix.transform(this.visibilityBottomRight.set(
                widget.x + widget.width(), widget.y + widget.height(), 0.0f, 1.0f));
        float sourceContentLeft = Math.min(this.visibilityTopLeft.x,
                this.visibilityBottomRight.x);
        float sourceContentTop = Math.min(this.visibilityTopLeft.y,
                this.visibilityBottomRight.y);
        float sourceContentRight = Math.max(this.visibilityTopLeft.x,
                this.visibilityBottomRight.x);
        float sourceContentBottom = Math.max(this.visibilityTopLeft.y,
                this.visibilityBottomRight.y);

        float contentScale = draw.contentScale;
        float matrixScale = Math.max(Math.abs(sourceMatrix.m00()),
                Math.abs(sourceMatrix.m11()));
        float padding = SoftVisibilityTransition.LAYER_PADDING
                * Math.max(1.0f, matrixScale) * contentScale;
        float framebufferWidth = client.getWindow().getFramebufferWidth();
        float framebufferHeight = client.getWindow().getFramebufferHeight();
        float sourceLeft = sourceContentLeft * contentScale - padding;
        float sourceTop = sourceContentTop * contentScale - padding;
        float sourceRight = sourceContentRight * contentScale + padding;
        float sourceBottom = sourceContentBottom * contentScale + padding;
        if (sourceRight <= sourceLeft || sourceBottom <= sourceTop) {
            return;
        }

        int texture = draw.bindTexture(
                this.visibilityTransitionFramebuffer.getColorAttachment());
        float inverseContentScale = 1.0f / Math.max(0.01f, contentScale);
        draw.softTexture(
                this.visibilityCompositeMatrix.identity(),
                sourceLeft * inverseContentScale,
                sourceTop * inverseContentScale,
                (sourceRight - sourceLeft) * inverseContentScale,
                (sourceBottom - sourceTop) * inverseContentScale,
                sourceLeft / framebufferWidth,
                1.0f - sourceTop / framebufferHeight,
                sourceRight / framebufferWidth,
                1.0f - sourceBottom / framebufferHeight,
                texture, 0xFFFFFFFF, blurRadius
        );
    }

    private void disableVisibilityTransition(Throwable error) {
        if (!this.visibilityTransitionFailed) {
            Spectra.LOGGER.error(
                    "HUD visibility blur failed; falling back to opacity-only transitions", error);
        }
        this.visibilityTransitionFailed = true;
    }

    boolean isVisibilityCaptureActive() {
        return this.visibilityCaptureActive;
    }

    private AnimatedFloat playerListOcclusionAnimation(Draggable widget) {
        return this.playerListOcclusionAnimations.computeIfAbsent(widget, ignored -> {
            AnimatedFloat animation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);
            animation.set(1.0f);
            return animation;
        });
    }

    private void pushWidgetAlpha(DrawEngine draw, Draggable widget, int index) {
        float entranceAlpha = HudEditorScreen.isAdvancedOpen()
                ? HudEditorScreen.elementEntranceProgress(index) : 1.0f;
        float occlusionAlpha = playerListOcclusionAnimation(widget).animatedValue();
        draw.pushVertexAlpha(
                entranceAlpha * occlusionAlpha * widget.moduleVisibilityProgress());
    }

    private boolean isOccludedByPlayerList(Draggable widget) {
        if (!HudEditorScreen.isPlayerListPreviewVisible()) {
            return false;
        }
        HudEditorOverlayWidget playerList = HudEditorOverlays.playerList();
        if (playerList == null || widget == playerList || !widget.isVisible()) {
            return false;
        }
        float left = playerList.contentX();
        float top = playerList.contentY();
        float right = left + playerList.contentWidth();
        float bottom = top + playerList.contentHeight();
        float widgetLeft = widget.x;
        float widgetTop = widget.y;
        float widgetWidth = widget.width();
        float widgetHeight = widget.height();
        if (widget instanceof HudEditorOverlayWidget overlay) {
            widgetLeft = overlay.contentX();
            widgetTop = overlay.contentY();
            widgetWidth = overlay.contentWidth();
            widgetHeight = overlay.contentHeight();
        }
        return widgetLeft < right && widgetLeft + widgetWidth > left
                && widgetTop < bottom && widgetTop + widgetHeight > top;
    }

    private void renderGrid(DrawEngine draw, MatrixStack matrices) {
        float opacity = this.gridAnimation.animatedValue();
        if (opacity <= 0.01f || this.resolution == null) {
            return;
        }
        float spacing = 24.0f;
        int minor = (MathUtil.clamp(Math.round(31.0f * opacity), 0, 255) << 24) | 0xFFFFFF;
        int major = (MathUtil.clamp(Math.round(66.0f * opacity), 0, 255) << 24) | 0xFFFFFF;
        int index = 0;
        for (float x = 0.0f; x <= this.resolution.screenWidth(); x += spacing, index++) {
            draw.rectangle(matrices.peek().getPositionMatrix(), x, 0.0f, 0.65f,
                    this.resolution.screenHeight(), index % 4 == 0 ? major : minor);
        }
        index = 0;
        for (float y = 0.0f; y <= this.resolution.screenHeight(); y += spacing, index++) {
            draw.rectangle(matrices.peek().getPositionMatrix(), 0.0f, y,
                    this.resolution.screenWidth(), 0.65f, index % 4 == 0 ? major : minor);
        }
    }

    private void renderEditorControls(DrawEngine draw, MatrixStack matrices, float animation) {
        if (animation <= 0.01f || this.resolution == null) {
            return;
        }
        // The editor controls use the exact same fixed-position fade as the
        // "Hold CTRL while dragging" hint above them.
        float y = controlsY();
        draw.pushVertexAlpha(animation);
        try {
        String gridLabel = gridControlLabel();
        float gridWidth = controlWidth(gridLabel);
        if (HudEditorScreen.isAdvancedOpen()) {
            renderControl(draw, matrices, gridControlX(), y, gridWidth,
                    gridLabel, this.gridIcon, this.gridEnabled);
            return;
        }
        renderControl(draw, matrices, gridControlX(), y, gridWidth,
                gridLabel, this.gridIcon, this.gridEnabled);
        renderControl(draw, matrices, advancedControlX(), y, controlWidth(advancedLabel()),
                advancedLabel(), this.advancedIcon, false);
        } finally {
            draw.popVertexAlpha();
        }
    }

    private void renderControl(DrawEngine draw, MatrixStack matrices, float x, float y,
                               float width, String label, SvgTexture icon, boolean active) {
        int inactiveColor = draw.colorStack().computeColor(EDITOR_HINT_RGB, EDITOR_HINT_ALPHA);
        int iconColor = active
                ? Spectra.INSTANCE.theme().palette().accentBright().argb()
                : inactiveColor;
        draw.textureVerticalC(matrices.peek().getPositionMatrix(), icon, x + 6.0f, y + 14.0f,
                12, 12, iconColor);
        MsdfFont font = Fonts.INTER_SEMIBOLD.get();
        float textX = x + 25.0f;
        float textY = y + (28.0f - font.getHeight(11.0f)) / 2.0f;
        draw.msdfFont(matrices.peek().getPositionMatrix(), font, label,
                textX + 0.75f, textY + 0.75f,
                11.0f, 0.05f, 0x87000000);
        draw.msdfFont(matrices.peek().getPositionMatrix(), font, label,
                textX, textY,
                11.0f, 0.05f, inactiveColor);
    }

    private void renderFreeDragHint(DrawEngine draw, MatrixStack matrices, float animation) {
        if (animation <= 0.01f || this.resolution == null
                || HudEditorScreen.isAdvancedOpen()
                || this.widgets.stream().noneMatch(Draggable::isVisible)) {
            return;
        }
        MsdfFont font = Fonts.INTER_SEMIBOLD.get();
        float fontSize = 11.0f;
        String hint = editorDragHint();
        float textWidth = font.getWidth(hint, fontSize);
        float x = (this.resolution.screenWidth() - textWidth) / 2.0f;
        float y = this.resolution.screenHeight() / 2.0f + 18.0f;
        int textAlpha = Math.round(EDITOR_HINT_ALPHA * animation);
        int shadowAlpha = Math.round(135.0f * animation);
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), font, hint,
                x + 0.75f, y + 0.75f, fontSize, 0.05f,
                draw.colorStack().computeColor(0x000000, shadowAlpha)
        );
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), font, hint,
                x, y, fontSize, 0.05f,
                draw.colorStack().computeColor(EDITOR_HINT_RGB, textAlpha)
        );
    }

    private String editorDragHint() {
        boolean verticalOnly = this.widgets.stream()
                .anyMatch(widget -> widget.isDragging() && widget.isHorizontalDragLocked());
        if (verticalOnly) {
            return ClientLocalization.text(
                    "Drag vertically to position notifications",
                    "\u041F\u0435\u0440\u0435\u0442\u0430\u0441\u043A\u0438\u0432\u0430\u0439\u0442\u0435 \u0443\u0432\u0435\u0434\u043E\u043C\u043B\u0435\u043D\u0438\u044F \u043F\u043E \u0432\u0435\u0440\u0442\u0438\u043A\u0430\u043B\u0438"
            );
        }
        return freeDragHint();
    }

    private void renderAdvancedStatus(DrawEngine draw, MatrixStack matrices) {
        if (!HudEditorScreen.isAdvancedOpen() || this.resolution == null) {
            return;
        }
        MsdfFont titleFont = Fonts.INTER_BOLD.get();
        MsdfFont textFont = Fonts.INTER_SEMIBOLD.get();
        String title = ClientLocalization.text("Advanced HUD mode", "\u0420\u0430\u0441\u0448\u0438\u0440\u0435\u043D\u043D\u044B\u0439 \u0440\u0435\u0436\u0438\u043C HUD");
        String exit = ClientLocalization.text("Exit advanced mode", "\u0412\u044B\u0439\u0442\u0438 \u0438\u0437 \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043D\u043D\u043E\u0433\u043E \u0440\u0435\u0436\u0438\u043C\u0430");
        float titleSize = 15.0f;
        float exitSize = 11.0f;
        float centerX = this.resolution.screenWidth() / 2.0f;
        float centerY = this.resolution.screenHeight() / 2.0f;
        float titleX = centerX - titleFont.getWidth(title, titleSize) / 2.0f;
        float titleY = centerY - 45.0f;
        draw.msdfFont(matrices.peek().getPositionMatrix(), titleFont, title,
                titleX + 0.75f, titleY + 0.75f, titleSize, 0.05f, 0xB0000000);
        draw.msdfFont(matrices.peek().getPositionMatrix(), titleFont, title,
                titleX, titleY, titleSize, 0.05f, 0xFFF4F4F7);

        float exitWidth = textFont.getWidth(exit, exitSize);
        float totalWidth = exitWidth + 21.0f;
        float exitX = centerX - totalWidth / 2.0f;
        float exitY = advancedExitY();
        float hover = this.exitHoverAnimation.animatedValue();
        int color = draw.colorStack().interpolate(0xFFB1B1BB, 0xFFFFFFFF, hover);
        draw.msdfFont(matrices.peek().getPositionMatrix(), textFont, exit,
                exitX, exitY, exitSize, 0.05f, color);
        draw.textureVerticalC(
                matrices.peek().getPositionMatrix(), this.exitIcon,
                exitX + exitWidth + 8.0f + hover * 5.0f,
                exitY + textFont.getHeight(exitSize) / 2.0f,
                12, 12, color
        );
    }

    public void click(MouseButtonEvent2 class300Var) {
        Mc class815Var = Mc.INSTANCE;
        if (HudEditorScreen.isEditing()) {
            Mouse mouse = class815Var.getMouse();
            float mouseX = (float) (mouse.getX() / HUD_SCALE);
            float mouseY = (float) (mouse.getY() / HUD_SCALE);
            boolean statusBlocked = Spectra.INSTANCE.systemStatusOverlay()
                    .blocksHudPointer(mouseX, mouseY, this.resolution);
            if (!statusBlocked
                    && class300Var.action() == ButtonAction.PRESS
                    && handleAddWidgetMenuClick(class300Var.button(), mouseX, mouseY)) {
                return;
            }
            if (!statusBlocked
                    && class300Var.button() == 0
                    && class300Var.action() == ButtonAction.PRESS
                    && handleEditorControlClick(mouse)) {
                return;
            }
            boolean z = statusBlocked;
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                Draggable class806Var = this.widgets.get(size);
                if (class806Var.onClick(new MouseButtonInput2(z, mouse, class300Var.action(), this.resolution, class300Var.button(), HUD_SCALE))) {
                    if (!z) {
                        markActive(class806Var);
                    }
                    z = true;
                }
            }
            if (class300Var.action() == ButtonAction.PRESS
                    && class300Var.button() == 1
                    && HudEditorScreen.isAdvancedOpen()
                    && !z
                    && !isPointOverVisibleWidget(mouseX, mouseY)
                    && !isPointOverEditorControl(mouseX, mouseY)) {
                openAddWidgetMenu(mouseX, mouseY);
                return;
            }
            if (class300Var.action() == ButtonAction.RELEASE) {
                this.snapManager.clear();
            }
        }
    }

    private boolean handleEditorControlClick(Mouse mouse) {
        if (this.resolution == null) {
            return false;
        }
        float mouseX = (float) (mouse.getX() / HUD_SCALE);
        float mouseY = (float) (mouse.getY() / HUD_SCALE);
        float y = controlsY();
        if (inside(mouseX, mouseY, gridControlX(), y,
                controlWidth(gridControlLabel()), 28.0f)) {
            this.gridEnabled = !this.gridEnabled;
            return true;
        }
        if (HudEditorScreen.isAdvancedOpen()
                && inside(mouseX, mouseY,
                advancedExitX() - 6.0f, advancedExitY() - 5.0f,
                advancedExitWidth() + 12.0f, 24.0f)) {
            HudEditorScreen.closeCurrent();
            return true;
        }
        if (HudEditorScreen.isAdvancedOpen()
                || !inside(mouseX, mouseY, advancedControlX(), y,
                controlWidth(advancedLabel()), 28.0f)) {
            return false;
        }
        Screen current = Mc.INSTANCE.getCurrentScreen();
        HudEditorScreen.openFrom(current);
        return true;
    }

    private float controlsY() {
        return this.resolution.screenHeight() / 2.0f
                + (HudEditorScreen.isAdvancedOpen() ? 52.0f : 40.0f);
    }

    private float gridControlX() {
        float gridWidth = controlWidth(gridControlLabel());
        if (HudEditorScreen.isAdvancedOpen()) {
            return this.resolution.screenWidth() / 2.0f - gridWidth / 2.0f;
        }
        float totalWidth = gridWidth + 24.0f + controlWidth(advancedLabel());
        return this.resolution.screenWidth() / 2.0f - totalWidth / 2.0f;
    }

    private float advancedControlX() {
        return gridControlX() + controlWidth(gridControlLabel()) + 24.0f;
    }

    private String gridControlLabel() {
        return this.gridEnabled
                ? ClientLocalization.text("Disable Grid", "\u0412\u044B\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u0441\u0435\u0442\u043A\u0443")
                : ClientLocalization.text("Enable Grid", "\u0412\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u0441\u0435\u0442\u043A\u0443");
    }

    private float controlWidth(String label) {
        return 31.0f + Fonts.INTER_SEMIBOLD.get().getWidth(label, 11.0f);
    }

    private boolean isAdvancedExitHovered(Mouse mouse) {
        if (this.resolution == null) {
            return false;
        }
        float mouseX = (float) (mouse.getX() / HUD_SCALE);
        float mouseY = (float) (mouse.getY() / HUD_SCALE);
        return inside(mouseX, mouseY,
                advancedExitX() - 6.0f, advancedExitY() - 5.0f,
                advancedExitWidth() + 12.0f, 24.0f);
    }

    private float advancedExitX() {
        return this.resolution.screenWidth() / 2.0f - advancedExitWidth() / 2.0f;
    }

    private float advancedExitY() {
        return this.resolution.screenHeight() / 2.0f + 25.0f;
    }

    private float advancedExitWidth() {
        return Fonts.INTER_SEMIBOLD.get().getWidth(
                ClientLocalization.text("Exit advanced mode", "\u0412\u044B\u0439\u0442\u0438 \u0438\u0437 \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043D\u043D\u043E\u0433\u043E \u0440\u0435\u0436\u0438\u043C\u0430"), 11.0f) + 21.0f;
    }

    private void renderAddWidgetMenu(DrawEngine draw, MatrixStack matrices) {
        float reveal = this.addWidgetMenuAnimation.animatedValue();
        if (!HudEditorScreen.isAdvancedOpen() || this.resolution == null || reveal <= 0.01f) {
            return;
        }
        List<HudModules.WidgetModule<?>> modules = filteredInactiveHudModules();
        updateAddWidgetMenuPlacement(modules.size());
        float width = 232.0f;
        float height = addWidgetMenuHeight(modules.size());
        float searchX = this.addWidgetMenuX + 9.0f;
        float searchY = this.addWidgetMenuY + 34.0f;
        float searchWidth = width - 18.0f;
        float searchHeight = 27.0f;
        float textX = searchX + 27.0f;
        this.addMenuSearch.listen(
                searchX, searchY, searchWidth, searchHeight,
                searchWidth - 36.0f, textX, 1.0f
        );

        draw.pushVertexAlpha(reveal);
        SpectraHudStyle.panel(
                draw, matrices, this.addWidgetMenuX, this.addWidgetMenuY,
                width, height, 9.0f
        );
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.addMenuTitleFont,
                ClientLocalization.text("Add HUD element", "\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C \u044D\u043B\u0435\u043C\u0435\u043D\u0442 HUD"), this.addWidgetMenuX + 10.0f,
                this.addWidgetMenuY + 10.0f,
                12.0f, 0.05f, SpectraHudStyle.TEXT
        );
        SpectraHudStyle.outlinedRect(
                draw, matrices, searchX, searchY, searchWidth, searchHeight, 6.0f,
                SpectraHudStyle.BORDER,
                SpectraHudStyle.panelColor(draw.colorStack(), SpectraHudStyle.PANEL_DARK)
        );
        draw.msdfFontVerticalC(
                matrices.peek().getPositionMatrix(), this.addMenuIconFont, "\u0424",
                searchX + 8.0f, searchY + searchHeight / 2.0f,
                11.0f, 0.05f, SpectraHudStyle.MUTED
        );
        draw.beginScissor(
                matrices.peek().getPositionMatrix(),
                textX, searchY, searchWidth - 35.0f, searchHeight
        );
        String query = this.addMenuSearch.text();
        float baselineY = searchY + (searchHeight - this.addMenuTextFont.getHeight(11.0f)) / 2.0f;
        float originX = textX - this.addMenuSearch.viewportOffset();
        if (this.addMenuSearch.hasSelection()) {
            float selectionX = originX + this.addMenuSearch.measureWidth(
                    query.substring(0, this.addMenuSearch.selMin())
            );
            float selectionRight = originX + this.addMenuSearch.measureWidth(
                    query.substring(0, this.addMenuSearch.selMax())
            );
            draw.roundedRectangle(
                    matrices.peek().getPositionMatrix(), selectionX, baselineY - 1.0f,
                    selectionRight - selectionX + 1.0f,
                    this.addMenuTextFont.getHeight(11.0f) + 2.0f,
                    3.0f,
                    (128 << 24) | (Spectra.INSTANCE.theme().palette().accent().argb() & 0xFFFFFF)
            );
        }
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.addMenuTextFont,
                query.isEmpty() && !this.addMenuSearch.focused()
                        ? ClientLocalization.text("Search HUD elements", "\u041F\u043E\u0438\u0441\u043A \u044D\u043B\u0435\u043C\u0435\u043D\u0442\u043E\u0432 HUD") : query,
                originX, baselineY, 11.0f, 0.05f,
                query.isEmpty() && !this.addMenuSearch.focused()
                        ? SpectraHudStyle.MUTED : SpectraHudStyle.TEXT
        );
        if (this.addMenuSearch.isCursorVisible() && !this.addMenuSearch.hasSelection()) {
            float cursorX = originX + this.addMenuSearch.measureWidth(
                    query.substring(0, this.addMenuSearch.cursorIndex())
            );
            draw.rectangle(
                    matrices.peek().getPositionMatrix(), cursorX, baselineY,
                    1.0f, this.addMenuTextFont.getHeight(11.0f), SpectraHudStyle.TEXT
            );
        }
        draw.endScissor();

        float rowY = this.addWidgetMenuY + 68.0f;
        if (modules.isEmpty()) {
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(), this.addMenuTextFont,
                    this.addMenuSearch.text().isBlank()
                            ? ClientLocalization.text("All HUD elements are enabled", "\u0412\u0441\u0435 \u044D\u043B\u0435\u043C\u0435\u043D\u0442\u044B HUD \u0432\u043A\u043B\u044E\u0447\u0435\u043D\u044B")
                            : ClientLocalization.text("Nothing found", "\u041D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E"),
                    this.addWidgetMenuX + 10.0f, rowY + 8.0f,
                    11.0f, 0.05f, SpectraHudStyle.MUTED
            );
        } else {
            float mouseX = (float) (Mc.INSTANCE.getMouse().getX() / HUD_SCALE);
            float mouseY = (float) (Mc.INSTANCE.getMouse().getY() / HUD_SCALE);
            for (HudModules.WidgetModule<?> module : modules) {
                boolean hovered = inside(
                        mouseX, mouseY,
                        this.addWidgetMenuX + 6.0f, rowY,
                        width - 12.0f, 38.0f
                );
                if (hovered) {
                    draw.roundedRectangle(
                            matrices.peek().getPositionMatrix(),
                            this.addWidgetMenuX + 6.0f, rowY,
                            width - 12.0f, 38.0f, 6.0f,
                            0x0DFFFFFF
                    );
                }
                int iconColor = Spectra.INSTANCE.theme().palette().accentBright().argb();
                draw.msdfFontVerticalCHorizontalC(
                        matrices.peek().getPositionMatrix(), this.addMenuIconFont,
                        addMenuGlyph(module),
                        this.addWidgetMenuX + 19.0f, rowY + 19.0f,
                        13.0f, 0.05f, iconColor
                );
                draw.msdfFont(
                        matrices.peek().getPositionMatrix(), this.addMenuTitleFont,
                        module.getName(), this.addWidgetMenuX + 34.0f, rowY + 6.0f,
                        11.0f, 0.05f, SpectraHudStyle.TEXT
                );
                draw.msdfFont(
                        matrices.peek().getPositionMatrix(), this.addMenuTextFont,
                        ModuleDescriptions.forModule(module.getName()),
                        this.addWidgetMenuX + 34.0f, rowY + 21.0f,
                        9.0f, 0.05f, SpectraHudStyle.MUTED
                );
                rowY += 38.0f;
            }
        }
        draw.popVertexAlpha();
    }

    private List<HudModules.WidgetModule<?>> filteredInactiveHudModules() {
        String query = this.addMenuSearch.text().strip().toLowerCase(Locale.ROOT);
        List<HudModules.WidgetModule<?>> result = new ArrayList<>();
        for (ru.spectra.client.module.Module candidate
                : Spectra.INSTANCE.moduleRepository().getModules()) {
            if (!(candidate instanceof HudModules.WidgetModule<?> module)
                    || module.isState()
                    || !module.isAvailable()) {
                continue;
            }
            String description = ModuleDescriptions.forModule(module.getName());
            if (query.isEmpty()
                    || module.getName().toLowerCase(Locale.ROOT).contains(query)
                    || description.toLowerCase(Locale.ROOT).contains(query)) {
                result.add(module);
            }
        }
        result.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(
                left.getName(), right.getName()
        ));
        return result;
    }

    private String addMenuGlyph(HudModules.WidgetModule<?> module) {
        return switch (module.getName()) {
            case "Active Effects" -> "u";
            case "Keybinds" -> "\u041B";
            case "Target HUD" -> "4";
            case "Cooldowns" -> "\u044A";
            default -> "j";
        };
    }

    private float addWidgetMenuHeight(int rowCount) {
        return 76.0f + Math.max(28.0f, rowCount * 38.0f);
    }

    private void updateAddWidgetMenuPlacement(int rowCount) {
        float width = 232.0f;
        float height = addWidgetMenuHeight(rowCount);
        float preferredX = this.addWidgetAnchorX + 8.0f;
        if (preferredX + width > this.resolution.screenWidth() - 6.0f) {
            preferredX = this.addWidgetAnchorX - width - 8.0f;
        }
        this.addWidgetMenuX = MathUtil.clamp(
                preferredX, 6.0f,
                Math.max(6.0f, this.resolution.screenWidth() - width - 6.0f)
        );
        this.addWidgetMenuY = MathUtil.clamp(
                this.addWidgetAnchorY, 6.0f,
                Math.max(6.0f, this.resolution.screenHeight() - height - 6.0f)
        );
    }

    private void openAddWidgetMenu(float mouseX, float mouseY) {
        this.addWidgetAnchorX = mouseX;
        this.addWidgetAnchorY = mouseY;
        this.addWidgetMenuOpen = true;
        this.addMenuSearch.clearText();
        this.addMenuSearch.focusAtEnd();
        this.addWidgetMenuAnimation.set(0.0f);
        this.addWidgetMenuAnimation.destination(1.0f);
    }

    private boolean handleAddWidgetMenuClick(int button, float mouseX, float mouseY) {
        if (!this.addWidgetMenuOpen || this.resolution == null) {
            return false;
        }
        List<HudModules.WidgetModule<?>> modules = filteredInactiveHudModules();
        updateAddWidgetMenuPlacement(modules.size());
        float width = 232.0f;
        float height = addWidgetMenuHeight(modules.size());
        boolean insidePanel = inside(
                mouseX, mouseY, this.addWidgetMenuX, this.addWidgetMenuY, width, height
        );
        if (!insidePanel) {
            this.addWidgetMenuOpen = false;
            this.addMenuSearch.focused(false);
            return false;
        }
        if (button != 0) {
            return true;
        }
        float searchX = this.addWidgetMenuX + 9.0f;
        float searchY = this.addWidgetMenuY + 34.0f;
        if (inside(mouseX, mouseY, searchX, searchY, width - 18.0f, 27.0f)) {
            this.addMenuSearch.focused(true);
            int cursor = this.addMenuSearch.calculateCursorPosition(Math.round(mouseX));
            this.addMenuSearch.cursorIndex = cursor;
            this.addMenuSearch.selectionAnchor = cursor;
            this.addMenuSearch.selectionEnd = cursor;
            this.addMenuSearch.ensureCursorVisible(cursor);
            return true;
        }
        float rowY = this.addWidgetMenuY + 68.0f;
        for (HudModules.WidgetModule<?> module : modules) {
            if (inside(mouseX, mouseY, this.addWidgetMenuX + 6.0f, rowY,
                    width - 12.0f, 38.0f)) {
                activateHudWidget(module);
                return true;
            }
            rowY += 38.0f;
        }
        return true;
    }

    private void activateHudWidget(HudModules.WidgetModule<?> module) {
        Draggable widget = module.widget();
        float widgetWidth = Math.max(1.0f, widget.width());
        float widgetHeight = Math.max(1.0f, widget.height());
        float x = widget.isHorizontalDragLocked()
                ? (this.resolution.screenWidth() - widgetWidth) / 2.0f
                : MathUtil.clamp(
                        this.addWidgetAnchorX - widgetWidth / 2.0f,
                        0.0f,
                        Math.max(0.0f, this.resolution.screenWidth() - widgetWidth)
                );
        float y = MathUtil.clamp(
                this.addWidgetAnchorY - widgetHeight / 2.0f,
                0.0f,
                Math.max(0.0f, this.resolution.screenHeight() - widgetHeight)
        );
        widget.applySavedPosition(x, y);
        module.setState(true);
        markActive(widget);
        ConfigAutoSaveScheduler.scheduleAutoSave();
        this.addWidgetMenuOpen = false;
        this.addMenuSearch.focused(false);
    }

    private boolean isPointOverVisibleWidget(float mouseX, float mouseY) {
        return this.widgets.stream().anyMatch(widget -> widget.isVisible()
                && inside(mouseX, mouseY, widget.x, widget.y, widget.width(), widget.height()));
    }

    private boolean isPointOverEditorControl(float mouseX, float mouseY) {
        float controlsY = controlsY();
        return inside(mouseX, mouseY, gridControlX(), controlsY,
                controlWidth(gridControlLabel()), 28.0f)
                || inside(mouseX, mouseY, advancedExitX() - 6.0f,
                advancedExitY() - 5.0f, advancedExitWidth() + 12.0f, 24.0f);
    }

    public boolean handleAddWidgetKeyPressed(int keyCode) {
        if (!this.addWidgetMenuOpen || !this.addMenuSearch.focused()) {
            return false;
        }
        boolean shift = Screen.hasShiftDown();
        if (keyCode == 256) {
            this.addWidgetMenuOpen = false;
            this.addMenuSearch.focused(false);
            return true;
        }
        if (Screen.hasControlDown()) {
            switch (keyCode) {
                case 65 -> this.addMenuSearch.selectAll();
                case 67 -> this.addMenuSearch.copy();
                case 86 -> this.addMenuSearch.paste();
                case 88 -> this.addMenuSearch.cut();
                default -> {
                    return false;
                }
            }
            return true;
        }
        switch (keyCode) {
            case 257 -> {
                List<HudModules.WidgetModule<?>> modules = filteredInactiveHudModules();
                if (modules.size() == 1) {
                    activateHudWidget(modules.getFirst());
                }
                return true;
            }
            case 259 -> this.addMenuSearch.backspace();
            case 261 -> this.addMenuSearch.deleteForward();
            case 262 -> this.addMenuSearch.moveCursor(1, shift);
            case 263 -> this.addMenuSearch.moveCursor(-1, shift);
            case 268 -> this.addMenuSearch.moveToStart(shift);
            case 269 -> this.addMenuSearch.moveToEnd(shift);
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean handleAddWidgetCharTyped(char character) {
        if (!this.addWidgetMenuOpen || !this.addMenuSearch.focused()) {
            return false;
        }
        this.addMenuSearch.insertCodePoint(character);
        return true;
    }

    public void resetEditorMenus() {
        this.addWidgetMenuOpen = false;
        this.addWidgetMenuAnimation.set(0.0f);
        this.addMenuSearch.focused(false);
    }

    public float playerListOcclusionOpacity(Draggable widget) {
        return playerListOcclusionAnimation(widget).animatedValue();
    }

    private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isAnyDragging() {
        for (Draggable class806Var : this.widgets) {
            if (class806Var.isDragging() && class806Var.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public void handleResize(int i, int i2) {
        this.widgets.forEach(class806Var -> {
            class806Var.handleResize(i, i2);
        });
    }

    public void update() {
        this.widgets.forEach((v0) -> {
            v0.updateWidget();
        });
    }

    public void markActive(Draggable class806Var) {
        if (class806Var == this.activeWidget || class806Var == null) {
            return;
        }
        this.activeWidget = class806Var;
        if (this.widgets.size() > 1) {
            bringToFront(class806Var);
        }
    }

    public void newWidget(Draggable class806Var) {
        if (this.widgets.contains(class806Var)) {
            return;
        }
        this.widgets.add(class806Var);
        class806Var.setSnapGrid(this.snapManager);
        class806Var.setSiblingWidgets(this.widgets);
    }

    public void bringToFront(Draggable class806Var) {
        ArrayList arrayList = new ArrayList(this.widgets);
        int size = this.widgets.size() - 1;
        int iIndexOf = this.widgets.indexOf(class806Var);
        if (iIndexOf == size) {
            return;
        }
        int i = 0;
        for (int i2 = size; i2 >= 0; i2--) {
            if (i2 == iIndexOf) {
                i = 1;
            } else {
                this.widgets.set((i2 - 1) + i, (Draggable) arrayList.get(i2));
            }
        }
        this.widgets.set(size, class806Var);
    }

    public List<Draggable> widgets() {
        return this.widgets;
    }
}
