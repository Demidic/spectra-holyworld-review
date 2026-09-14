package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.Lang;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.type.Mc;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.ui.setting.Setting;
import ru.spectra.client.util.SnapManager;
import ru.spectra.client.model.SnapResult;
import ru.spectra.client.util.StencilBufferUtil;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.SoftVisibilityTransition;
import ru.spectra.client.util.WeightedEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Draggable {
    private final ru.spectra.client.util.RectangleMeshCache outlineMesh =
            new ru.spectra.client.util.RectangleMeshCache();
    public final String name;
    public float x;
    public float y;
    public float aC;
    public float L;
    public BooleanSupplier visibility;
    public BooleanSupplier availability = () -> true;
    public boolean dragging;
    public boolean clicked;
    public boolean cursorHovered;
    public SnapManager snapGrid;
    public List<Draggable> siblingWidgets;

    public ScreenResolution resolution;
    public final List<Setting> settings = new ArrayList();

    public ToggleAnimator clickAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator hoveredAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator tooltipAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat visibilityAnimation =
            new AnimatedFloat(220, Easings.EASE_IN_OUT_CUBIC);
    private boolean visibilityInitialized;
    public boolean excludeFromSnapGrid = false;
    public boolean dragLocked = false;

    public final Stopwatch lastResizeTimer = new Stopwatch(false);

    public Draggable(String str, BooleanSupplier booleanSupplier) {
        this.name = str;
        this.visibility = booleanSupplier;
        // Start hidden deliberately: the first real appearance should animate.
        // Calling isContentVisible() here would also invoke a subclass before its
        // fields have been initialized.
        this.visibilityAnimation.set(0.0f);
    }

    public void addSettings(Setting... class661VarArr) {
        this.settings.addAll(Arrays.asList(class661VarArr));
    }

    public void draw(DragRenderContext class809Var) {
        if (this.resolution == null || this.resolution.screenWidth() <= 0 || this.resolution.screenHeight() <= 0) {
            this.resolution = ScreenResolution.resolution();
        } else if (isVisible()) {
            render(class809Var);
        }
    }

    public void handleResize(int i, int i2) {
        if (i < 64 || i2 < 64) {
            return;
        }
        this.resolution = new ScreenResolution(i, i2);
        this.lastResizeTimer.reset();
    }

    public void applySavedPosition(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public void updateDrag(DragRenderContext class809Var) {
        ScreenResolution class710VarResolution;
        Mouse mouse = Mc.INSTANCE.getMouse();
        Screen currentScreen = Mc.INSTANCE.getCurrentScreen();
        MenuWindow class776VarMenuWindow = Spectra.INSTANCE.menuWindow();
        if (class776VarMenuWindow != null
                && class776VarMenuWindow.visible()
                && !HudEditorScreen.isMenuPreviewOpen()) {
            WidgetSettingsPopup class810Var = (WidgetSettingsPopup) Spectra.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (class810Var != null) {
                class810Var.closeWindow();
                return;
            }
            return;
        }
        if (!HudEditorScreen.isEditing()) {
            this.dragging = false;
            this.clicked = false;
            if (this.snapGrid != null) {
                this.snapGrid.clear();
            }
            WidgetSettingsPopup class810Var2 = (WidgetSettingsPopup) Spectra.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (class810Var2 != null) {
                class810Var2.close();
                return;
            }
            return;
        }
        if (!this.dragging || (class710VarResolution = class809Var.resolution()) == null || class710VarResolution.screenWidth() <= 0 || class710VarResolution.screenHeight() <= 0) {
            return;
        }
        float fScaleFactor = class809Var.scaleFactor();
        float x = (float) (mouse.getX() / ((double) fScaleFactor));
        float y = (float) (mouse.getY() / ((double) fScaleFactor));
        float f = x - this.aC;
        float f2 = y - this.L;
        boolean horizontalLocked = isHorizontalDragLocked();
        if (horizontalLocked) {
            f = (class710VarResolution.screenWidth() - width()) / 2.0f;
        }
        boolean freeDrag = Screen.hasControlDown();
        if (freeDrag && this.snapGrid != null) {
            this.snapGrid.clear();
        }
        if (this.snapGrid == null || this.siblingWidgets == null || isExcludeFromSnapGrid() || freeDrag) {
            this.x = f;
            this.y = f2;
        } else {
            SnapResult class813VarCalculateSnap = this.snapGrid.calculateSnap(this, this.siblingWidgets, f, f2, class710VarResolution.screenWidth(), class710VarResolution.screenHeight());
            this.x = class813VarCalculateSnap.x;
            this.y = class813VarCalculateSnap.y;
        }
        boolean z = this.x < 0.0f || this.y < 0.0f || this.x + width() > ((float) class710VarResolution.screenWidth()) || this.y + height() > ((float) class710VarResolution.screenHeight());
        if (class710VarResolution.isExceeding(width(), height()) && z) {
            this.x = MathUtil.clamp(this.x, 0.0f, class710VarResolution.screenWidth() - width());
            this.y = MathUtil.clamp(this.y, 0.0f, class710VarResolution.screenHeight() - height());
        }
    }

    public void renderEditOutline(DragRenderContext context, float animation) {
        if (animation <= 0.01f || !isVisible() || this.dragLocked) {
            return;
        }
        renderEditOutlineBounds(context, animation, this.x, this.y, width(), height());
    }

    protected void renderEditOutlineBounds(DragRenderContext context, float animation,
                                           float boundsX, float boundsY,
                                           float widgetWidth, float widgetHeight) {
        if (widgetWidth <= 0.5f || widgetHeight <= 0.5f) {
            return;
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        ScreenResolution outlineResolution = context.resolution();
        float maxWidth = outlineResolution == null ? Float.MAX_VALUE : outlineResolution.screenWidth();
        float maxHeight = outlineResolution == null ? Float.MAX_VALUE : outlineResolution.screenHeight();
        // Widgets aligned to a screen edge used to place the outer half of the
        // dashed border outside the framebuffer. Clamp each edge so the top
        // and left segments remain visible at x/y = 0.
        float left = Math.max(0.0f, boundsX - 2.5f);
        float top = Math.max(0.0f, boundsY - 2.5f);
        float right = Math.min(maxWidth, boundsX + widgetWidth + 2.5f);
        float bottom = Math.min(maxHeight, boundsY + widgetHeight + 2.5f);
        float dash = 4.0f;
        float gap = 3.0f;
        // A sub-pixel horizontal rectangle can be discarded by rasterization
        // at some HUD scales even though its vertical counterpart survives.
        float thickness = Math.max(1.15f, 1.0f / Math.max(0.01f, context.scaleFactor()));
        int alpha = MathUtil.clamp(Math.round(104.0f * animation), 0, 255);
        int color = (alpha << 24) | 0xA4A6AD;
        float horizontalTop = Math.min(bottom - thickness, top + 0.25f);
        float horizontalBottom = Math.max(top, bottom - thickness - 0.25f);
        if (this.outlineMesh.replayOrBegin(draw, matrices.peek().getPositionMatrix(),
                left, top, right, bottom, thickness, horizontalTop, horizontalBottom,
                color, Mc.INSTANCE.getWindow().getFramebufferHeight())) {
            return;
        }
        for (float cursor = left; cursor < right; cursor += dash + gap) {
            float length = Math.min(dash, right - cursor);
            draw.rectangle(matrices.peek().getPositionMatrix(), cursor, horizontalTop, length, thickness, color);
            draw.rectangle(matrices.peek().getPositionMatrix(), cursor, horizontalBottom, length, thickness, color);
        }
        for (float cursor = top; cursor < bottom; cursor += dash + gap) {
            float length = Math.min(dash, bottom - cursor);
            draw.rectangle(matrices.peek().getPositionMatrix(), left, cursor, thickness, length, color);
            draw.rectangle(matrices.peek().getPositionMatrix(), right - thickness, cursor, thickness, length, color);
        }
        this.outlineMesh.finish(draw);
    }

    public void animation(WeightedEngine class141Var) {
        this.hoveredAnimation.state(this.cursorHovered).animate(class141Var);
        this.tooltipAnimation.state(this.cursorHovered && HudEditorScreen.isEditing()).animate(class141Var);
        this.clickAnimation.state(this.clicked).animate(class141Var);
        // Let the widget update its raw content state first. The shared fade is
        // intentionally independent from any old row/panel opacity animation.
        animate(class141Var);
        boolean targetVisible = SoftVisibilityTransition.target(
                isEnabled(), isContentVisible(), HudEditorScreen.isEditing());
        // Loading a config may enable many widgets in the same first frame.
        // Settling that initial state avoids launching a full layer transition
        // for every HUD element at once. Later content/module changes animate.
        if (!this.visibilityInitialized) {
            this.visibilityAnimation.set(targetVisible ? 1.0f : 0.0f);
            this.visibilityInitialized = true;
            return;
        }
        this.visibilityAnimation
                .destination(targetVisible ? 1.0f : 0.0f)
                .animate(class141Var);
    }

    public void updateWidget() {
        if (isEnabled()) {
            update();
        }
    }

    public boolean onCursor(MouseMoveInput class808Var) {
        boolean zIsWithinBounds = class808Var.isWithinBounds(this.x, this.y, width(), height());
        boolean zCursor = cursor(class808Var, zIsWithinBounds);
        if (!isEnabled()) {
            this.cursorHovered = false;
            return false;
        }
        if (class808Var.intercepted()) {
            this.cursorHovered = false;
        } else {
            this.cursorHovered = zIsWithinBounds;
            if (zIsWithinBounds) {
                return true;
            }
        }
        return zCursor;
    }

    public boolean onClick(MouseButtonInput2 class807Var) {
        if (!isEnabled()) {
            return false;
        }
        if (this.dragLocked) {
            this.dragging = false;
            this.clicked = false;
            return false;
        }
        boolean zIsWithinBounds = class807Var.isWithinBounds(this.x, this.y, width(), height());
        boolean zClick = click(class807Var, zIsWithinBounds);
        if (class807Var.isLeftButtonPressed()) {
            if (!class807Var.intercepted() && class807Var.press() && zIsWithinBounds) {
                if (zClick) {
                    return true;
                }
                this.clicked = true;
                float fMouseX = class807Var.mouseX();
                float fMouseY = class807Var.mouseY();
                this.aC = fMouseX - this.x;
                this.L = fMouseY - this.y;
                this.dragging = true;
                return true;
            }
            if (class807Var.release()) {
                this.clicked = false;
                this.dragging = false;
                if (this.snapGrid == null) {
                    return true;
                }
                this.snapGrid.clear();
                return true;
            }
        }
        return false;
    }

    public void drawTooltip(DragRenderContext class809Var) {
        float fSmoothAnimation = this.tooltipAnimation.smoothAnimation();
        if (fSmoothAnimation <= 0.0f) {
            return;
        }
        DrawEngine class154VarDrawEngine = class809Var.drawEngine();
        MatrixStack matrixStack = class809Var.matrixStack();
        ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
        float widgetWidth = width();
        float widgetHeight = height();
        if (widgetWidth <= 0.5f || widgetHeight <= 0.5f) {
            return;
        }
        int overlayAlpha = MathUtil.clamp(Math.round(154.0f * fSmoothAnimation), 0, 255);
        class154VarDrawEngine.roundedRectangle(
                matrixStack.peek().getPositionMatrix(),
                this.x, this.y, widgetWidth, widgetHeight, SpectraHudStyle.RADIUS,
                (overlayAlpha << 24) | 0x09090B
        );
        MsdfFont font = Fonts.INTER_SEMIBOLD.get();
        String label = ClientLocalization.text("Move ", "Переместить ") + tooltipName();
        float fontSize = 12.0f;
        float textX = this.x + (widgetWidth - font.getWidth(label, fontSize)) / 2.0f;
        float textY = this.y + (widgetHeight - font.getHeight(fontSize)) / 2.0f;
        class154VarDrawEngine.msdfFont(
                matrixStack.peek().getPositionMatrix(), font, label,
                textX, textY, fontSize, 0.05f,
                class115VarColorStack.computeColor(0xFFFFFF, Math.round(235.0f * fSmoothAnimation))
        );
    }

    private String tooltipName() {
        return switch (this.name) {
            case "PotionList" -> "Active Effects";
            case "Hotkeys" -> "Keybinds";
            case "TargetHud" -> "Target HUD";
            case "Structures" -> "Structures";
            default -> this.name.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
        };
    }

    public boolean hasTooltip() {
        return !this.dragLocked;
    }

    public abstract void layout(DragRenderContext class809Var);

    public abstract void render(DragRenderContext class809Var);

    public abstract boolean click(MouseButtonInput2 class807Var, boolean z);

    public abstract boolean cursor(MouseMoveInput class808Var, boolean z);

    public abstract void animate(WeightedEngine class141Var);

    public abstract void update();

    public abstract float width();

    public abstract float height();

    public void resetCursor() {
        this.cursorHovered = false;
    }

    public boolean isVisible() {
        // Enabled-but-empty widgets must keep receiving update/animation calls,
        // while a disabled widget remains renderable until its exit is complete.
        return this.availability.getAsBoolean()
                && (isEnabled() || !this.visibilityAnimation.isZero());
    }

    public boolean isEnabled() {
        return this.availability.getAsBoolean() && this.visibility.getAsBoolean();
    }

    public float moduleVisibilityProgress() {
        return SoftVisibilityTransition.progress(
                this.visibilityAnimation.animatedValue());
    }

    /**
     * Dynamic widgets report only their raw populated/empty state. The base owns
     * the transition so every HUD element uses exactly one shared visibility fade.
     */
    protected boolean isContentVisible() {
        return true;
    }

    public final float softVisibilityProgress() {
        return moduleVisibilityProgress();
    }

    public String getName() {
        return this.name;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getDragX() {
        return this.aC;
    }

    public float getDragY() {
        return this.L;
    }

    public BooleanSupplier getVisibility() {
        return this.visibility;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public ToggleAnimator getClickAnimation() {
        return this.clickAnimation;
    }

    public ToggleAnimator getHoveredAnimation() {
        return this.hoveredAnimation;
    }

    public ToggleAnimator getTooltipAnimation() {
        return this.tooltipAnimation;
    }

    public boolean isClicked() {
        return this.clicked;
    }

    public boolean isCursorHovered() {
        return this.cursorHovered;
    }

    public SnapManager getSnapGrid() {
        return this.snapGrid;
    }

    public List<Draggable> getSiblingWidgets() {
        return this.siblingWidgets;
    }

    public boolean isExcludeFromSnapGrid() {
        return this.excludeFromSnapGrid;
    }

    public boolean isDragLocked() {
        return this.dragLocked;
    }

    /** Horizontal axis policy used by dragging and snap guides. */
    public boolean isHorizontalDragLocked() {
        return false;
    }

    public ScreenResolution getResolution() {
        return this.resolution;
    }

    public Stopwatch getLastResizeTimer() {
        return this.lastResizeTimer;
    }

    public void setX(float f) {
        this.x = f;
    }

    public void setY(float f) {
        this.y = f;
    }

    public void setDragX(float f) {
        this.aC = f;
    }

    public void setDragY(float f) {
        this.L = f;
    }

    public void setSnapGrid(SnapManager class811Var) {
        this.snapGrid = class811Var;
    }

    public void setSiblingWidgets(List<Draggable> list) {
        this.siblingWidgets = list;
    }

    public void setExcludeFromSnapGrid(boolean z) {
        this.excludeFromSnapGrid = z;
    }

    public void setDragLocked(boolean dragLocked) {
        this.dragLocked = dragLocked;
        if (dragLocked) {
            this.dragging = false;
            this.clicked = false;
        }
    }
}
