package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.net.InputInterceptor;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.OverlayCommandQueue;
import ru.spectra.client.util.OverlayManager;
import ru.spectra.client.util.FramebufferUtil;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.net.TooltipService;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.WindowController;
import ru.spectra.client.util.DrawEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public class MenuWindow extends AbstractWindow {
    public static float MENU_WIDTH = 814.0f;
    public static float MENU_HEIGHT = 500.0f;
    public static float SIDEBAR_WIDTH = 188.0f;
    public static float CONTENT_WIDTH = MENU_WIDTH - SIDEBAR_WIDTH;
    static final int BLURRED_SURFACE = 0xE6080809;
    private static final int BLURRED_OUTER_BORDER = 0xFF17181C;
    public static float EXPANDED_HEADER_HEIGHT = 45.0f;
    public static float COLLAPSED_HEADER_HEIGHT = 45.0f;
    public static final float[] scaleOptions = {0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
    public boolean needsCentering;
    public final ToggleAnimator openAnimation;
    public final MenuHeaderContainer headerContainer;

    public final MenuContentArea contentArea;

    public final ChatPanel chat;

    public final DraggableContainer draggableBehavior;

    public final SearchOverlay searchContainer;

    public final ActionConfirmDialog actionConfirmationDialogContainer;

    public final PopupWindow popupWindow;

    public final PlainContainer tooltipLayer;

    public final TooltipService tooltipService;

    public final OverlayManager priorityOverlayHandler;
    public boolean menuOpen;
    private int openKey = 344;
    private Framebuffer animationFramebuffer;
    private float lastLogicalWidth = -1.0f;
    private float lastLogicalHeight = -1.0f;

    public MenuWindow() {
        super(MENU_WIDTH, MENU_HEIGHT);
        this.needsCentering = true;
        this.openAnimation = new ToggleAnimator(150, Easings.LINEAR);
        this.headerContainer = new MenuHeaderContainer();
        this.draggableBehavior = new DraggableContainer();
        this.searchContainer = new SearchOverlay();
        this.actionConfirmationDialogContainer = new ActionConfirmDialog();
        this.popupWindow = new PopupWindow();
        this.tooltipLayer = new PlainContainer();
        this.tooltipService = new TooltipService(this.tooltipLayer);
        this.priorityOverlayHandler = new OverlayManager();
        this.menuOpen = false;
        this.contentArea = new MenuContentArea();
        this.visible = false;
        this.chat = new ChatPanel();
        addChild(this.draggableBehavior);
        addChild(this.chat);
        addChild(this.contentArea);
        addChild(this.tooltipLayer);
        addChild(this.headerContainer);
        addChild(this.searchContainer);
        addChild(this.actionConfirmationDialogContainer);
        addChild(this.popupWindow);
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        float logicalWidth = class698Var.logicalWidth();
        float logicalHeight = class698Var.logicalHeight();
        if (logicalWidth != this.lastLogicalWidth || logicalHeight != this.lastLogicalHeight) {
            this.lastLogicalWidth = logicalWidth;
            this.lastLogicalHeight = logicalHeight;
            this.needsCentering = true;
        }
        if (this.needsCentering) {
            center(class698Var);
            this.needsCentering = false;
        }
        if (!this.menuOpen && this.openAnimation.isZero()) {
            this.chat.visible(false);
            this.headerContainer.visible(false);
            this.contentArea.visible(false);
            this.draggableBehavior.visible(false);
            return;
        }
        this.chat.setPosition(x() + MENU_WIDTH, y());
        this.chat.visible(false);
        this.headerContainer.setPosition(x(), y());
        this.headerContainer.visible(true);
        this.contentArea.setPosition(x() + SIDEBAR_WIDTH, y() + COLLAPSED_HEADER_HEIGHT);
        this.contentArea.visible(!this.headerContainer.isSettingsView());
        this.searchContainer.setPosition(x(), y());
        this.searchContainer.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.actionConfirmationDialogContainer.setPosition(x(), y());
        this.popupWindow.setPosition(x(), y());
        this.popupWindow.setSize(MENU_WIDTH, MENU_HEIGHT);
        super.layout(class698Var);
        setSize(MENU_WIDTH, MENU_HEIGHT);
        this.draggableBehavior.setPosition(x(), y());
        this.draggableBehavior.setSize(width(), height());
        this.draggableBehavior.visible(true);
        super.onMenuDrag(this.draggableBehavior.isDragging());
    }

    @Override
    public void render(DrawCtx class699Var) {
        InputInterceptor class631VarWindow = class699Var.window();
        // Backdrop effects are rendered inside the same scaled/composited pass
        // as the menu, so WindowController's legacy global dim must stay off.
        Spectra.INSTANCE.windowController().backgroundAlpha(0);
        if (!this.openAnimation.isZero()) {
            if (this.visible) {
                class631VarWindow.interceptCursorfScreenNotPresent(true);
                class631VarWindow.interceptKeyboardIfScreenPresent(true);
            }
            if (this.openAnimation.isOne()) {
                renderMenuContents(class699Var);
            } else {
                renderAnimatedComposite(class699Var, this.openAnimation.smoothAnimation());
            }
            if (!this.menuOpen || class631VarWindow.cursorVisible()) {
                return;
            }
            this.openAnimation.state(false);
            this.menuOpen = false;
            close();
        }
    }

    public void renderChild(Widget class682Var, DrawCtx class699Var) {
        if (class682Var instanceof WidgetParent) {
            ((WidgetParent) class682Var).render(class699Var);
        }
    }

    public void renderBackground(DrawCtx class699Var) {
        renderSurface(class699Var, x(), y(), MENU_WIDTH, MENU_HEIGHT, 12.0f);
    }

    /** Keeps every menu-owned flyout on the exact same normal/blurred surface. */
    static void renderSurface(DrawCtx class699Var, float x, float y,
                              float width, float height, float radius) {
        ColorStack class115VarColorStack = class699Var.colorStack();
        if (Spectra.INSTANCE.configManager().menuStateConfig().menuSurfaceStyle()
                == SurfaceStyle.BLURRED) {
            Framebuffer blurred = Spectra.INSTANCE.windowController()
                    .headerBlur().getBlurFramebuffer();
            int blurTexture = blurred == null ? -1 : blurred.getColorAttachment();
            class699Var.glassPanel(
                    blurTexture,
                    x, y, width, height,
                    radius, SpectraHudStyle.BORDER_WIDTH,
                    SpectraHudStyle.color(class115VarColorStack, BLURRED_OUTER_BORDER),
                    SpectraHudStyle.color(class115VarColorStack, BLURRED_SURFACE)
            );
            return;
        }
        class699Var.fillRoundedRect(x, y, width, height, radius,
                class115VarColorStack.computeColor(0xFF080809));
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.openAnimation.animate(class141Var);
        boolean zIsZero = this.openAnimation.isZero();
        if (!this.menuOpen && zIsZero) {
            if (this.visible) {
                close();
            }
            return;
        }
        this.visible = this.menuOpen || !zIsZero;
        if (this.menuOpen || !zIsZero) {
            super.animation(class141Var);
            return;
        }
        this.chat.visible(false);
        this.headerContainer.visible(false);
        this.contentArea.visible(false);
        this.draggableBehavior.visible(false);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean z2 = this.menuOpen;
        boolean zHandleInput = z;
        boolean zExpandedState = this.headerContainer.expandedState();
        if (z2) {
            boolean zHandleInput2 = zHandleInput | this.popupWindow.handleInput(class688Var, zHandleInput);
            zHandleInput2 |= this.actionConfirmationDialogContainer.handleInput(class688Var, zHandleInput2);
            boolean zHandleInput3 = zHandleInput2 | this.searchContainer.handleInput(class688Var, zHandleInput2);
            if (!zExpandedState) {
                zHandleInput3 |= this.priorityOverlayHandler.handlePopupInput(class688Var, zHandleInput3);
            }
            boolean zHandleInput4 = zHandleInput3 | this.headerContainer.handleInput(class688Var, zHandleInput3);
            if (zExpandedState) {
                zHandleInput4 |= this.priorityOverlayHandler.handlePopupInput(class688Var, zHandleInput4);
            }
            boolean zHandleInput6 = zHandleInput4 | this.contentArea.handleInput(class688Var, zHandleInput4);
            zHandleInput = zHandleInput6 | this.draggableBehavior.handleInput(class688Var, zHandleInput6);
        }
        if (!zHandleInput) {
            InputEvent class691VarInputEvent = class688Var.inputEvent();
            if (class691VarInputEvent instanceof KeyInput) {
                KeyInput class696Var = (KeyInput) class691VarInputEvent;
                WindowController class686VarWindowController = Spectra.INSTANCE.windowController();
                if (class696Var.keyAction().press()) {
                    int iKeyCode = class696Var.keyCode();
                    if (iKeyCode == this.openKey) {
                        this.menuOpen = !this.menuOpen;
                        if (this.menuOpen) {
                            requestCentering();
                            prepareForUniformOpen();
                        }
                        this.openAnimation.state(this.menuOpen);
                        if (this.menuOpen) {
                            class686VarWindowController.showCursor(class686VarWindowController.window());
                            class686VarWindowController.recenterMouse();
                        } else {
                            class686VarWindowController.revertCursor(class686VarWindowController.window());
                            class686VarWindowController.recenterMouse();
                        }
                        // Keep the whole window registered until its common
                        // fade reaches zero. Closing it here made the sidebar
                        // disappear immediately while card draw commands from
                        // the scrolling layer survived for another frame.
                        if (this.menuOpen) {
                            open();
                        }
                        return true;
                    }
                    if (iKeyCode == 256 && visible()) {
                        this.menuOpen = false;
                        this.openAnimation.state(false);
                        class686VarWindowController.revertCursor(class686VarWindowController.window());
                        class686VarWindowController.recenterMouse();
                        return true;
                    }
                }
            }
        }
        return zHandleInput;
    }

    private void prepareForUniformOpen() {
        MenuTabElement currentTab = Spectra.INSTANCE.tabsController().current();
        if (currentTab == null) {
            return;
        }

        // The window already supplies the opening fade. Keep the selected tab and
        // its cards at their completed visual state so they do not fade or slide
        // independently underneath it.
        currentTab.currentTabAnimation.force(true);
        currentTab.forceImmediateFramePositioning = true;
        currentTab.markFramesDirty();

        for (AbstractFrame frame : currentTab.frames()) {
            if (frame instanceof ModuleCard card) {
                card.snapTo(card.x(), card.y());
                boolean enabled = card.module().isState();
                card.toggleSwitch.toggleSwitchAnimation().force(enabled);
                card.toggleSwitch.lastState = enabled;
            }
        }
    }

    private void renderMenuContents(DrawCtx context) {
        renderBackdrop(context);
        context.matrixStack().push();
        renderBackground(context);
        renderChild(this.draggableBehavior, context);
        renderChild(this.headerContainer, context);
        if (!this.headerContainer.isSettingsView()) {
            renderChild(this.contentArea, context);
        }
        this.priorityOverlayHandler.renderPopup(context);
        renderChild(this.tooltipLayer, context);
        renderChild(this.searchContainer, context);
        renderChild(this.actionConfirmationDialogContainer, context);
        renderChild(this.popupWindow, context);
        context.matrixStack().pop();
    }

    private void renderBackdrop(DrawCtx context) {
        var preferences = Spectra.INSTANCE.configManager().menuStateConfig();
        float screenWidth = context.layoutContext().logicalWidth();
        float screenHeight = context.layoutContext().logicalHeight();
        if (preferences.isMenuBlurBackgroundEnabled()) {
            context.roundedBlur(
                    Spectra.INSTANCE.windowController().headerBlur()
                            .getBlurFramebuffer().getColorAttachment(),
                    0.0f, 0.0f, screenWidth, screenHeight,
                    0.0f, context.colorStack().white());
        }
        if (preferences.isMenuDimBackgroundEnabled()) {
            context.fillRect(0.0f, 0.0f, screenWidth, screenHeight,
                    context.colorStack().computeColor(0x000000, 150));
        }
    }

    /**
     * Fading every primitive separately compounds alpha wherever cards overlap
     * the menu background. Render the complete menu at full opacity first and
     * blend that one texture once, so background, sidebar, divider and cards
     * have exactly the same visual progress.
     */
    private void renderAnimatedComposite(DrawCtx context, float progress) {
        DrawEngine draw = context.drawEngine();
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        // Flush the already queued world dimming into the main framebuffer
        // before changing the render target.
        draw.draw();
        this.animationFramebuffer = FramebufferUtil.ensureFramebuffer(
                this.animationFramebuffer,
                framebufferWidth,
                framebufferHeight,
                () -> new SimpleFramebuffer(framebufferWidth, framebufferHeight, false)
        );
        FramebufferUtil.resizeIfNeeded(this.animationFramebuffer, framebufferWidth, framebufferHeight);
        FramebufferUtil.clearTransparent(this.animationFramebuffer);

        this.animationFramebuffer.beginWrite(true);
        renderMenuContents(context);
        draw.draw();
        this.animationFramebuffer.endWrite();

        client.getFramebuffer().beginWrite(true);
        int texture = draw.bindTexture(this.animationFramebuffer.getColorAttachment());
        int color = context.colorStack().computeColor(
                0xFFFFFF,
                Math.round(MathUtil.clamp(progress, 0.0f, 1.0f) * 255.0f)
        );
        draw.texture(
                context.matrixStack().peek().getPositionMatrix(),
                0.0f,
                0.0f,
                framebufferWidth,
                framebufferHeight,
                texture,
                color,
                true
        );
    }

    /**
     * Allocates the full-window animation target during renderer startup instead
     * of stalling the first menu-open frame with a driver allocation.
     */
    public void warmUpAnimationFramebuffer() {
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }
        this.animationFramebuffer = FramebufferUtil.ensureFramebuffer(
                this.animationFramebuffer,
                framebufferWidth,
                framebufferHeight,
                () -> new SimpleFramebuffer(framebufferWidth, framebufferHeight, false)
        );
        FramebufferUtil.resizeIfNeeded(
                this.animationFramebuffer,
                framebufferWidth,
                framebufferHeight
        );
        client.getFramebuffer().beginWrite(true);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue class677Var) {
        boolean overlayNeedsBlur = this.searchContainer.opened
                || !this.searchContainer.openAnimator.isZero()
                || this.actionConfirmationDialogContainer.isOpen();
        if (this.openAnimation.isZero()) {
            return;
        }
        if (Spectra.INSTANCE.configManager().menuStateConfig().isMenuBlurBackgroundEnabled()
                || Spectra.INSTANCE.configManager().menuStateConfig().menuSurfaceStyle()
                == SurfaceStyle.BLURRED) {
            // Force capture of the rendered world for the full-screen menu blur.
            class677Var.record(context -> { });
        }
        if (!overlayNeedsBlur) {
            return;
        }
        class677Var.requireSearchBlur();
        OverlayCommandQueue class677Var2 = new OverlayCommandQueue();
        super.collectBlurElements(class677Var2);
        class677Var.record(class699Var -> {
            ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
            class115VarColorStack.push();
            class699Var.matrixStack().push();
            if (!this.openAnimation.isOne()) {
                class115VarColorStack.alpha(this.openAnimation.smoothAnimation());
            }
            renderBackground(class699Var);
            class677Var2.renderRecorded(class699Var);
            class699Var.matrixStack().pop();
            class115VarColorStack.pop();
        });
    }

    @Override
    public void close() {
        this.popupWindow.close();
        ColorPickerWindow class773Var = (ColorPickerWindow) Spectra.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
        if (class773Var != null) {
            class773Var.closePicker();
        }
        this.draggableBehavior.setDragging(false);
        super.close();
    }

    public void suspendForHudEditor() {
        this.menuOpen = false;
        this.openAnimation.force(false);
        close();
    }

    public void resumeAfterHudEditor() {
        this.menuOpen = true;
        this.openAnimation.force(true);
        requestCentering();
        prepareForUniformOpen();
        open();
    }

    public int openKey() {
        return this.openKey;
    }

    public void setOpenKey(int key) {
        if (key > 0 && key != 256) {
            this.openKey = key;
        }
    }

    public void collapseExpandedHeader() {
        if (this.headerContainer.expandedState()) {
            this.headerContainer.exchange();
        }
    }

    public void requestCentering() {
        this.needsCentering = true;
    }

    public MenuContentArea contentArea() {
        return this.contentArea;
    }

    public ChatPanel chat() {
        return this.chat;
    }

    public DraggableContainer draggableBehavior() {
        return this.draggableBehavior;
    }

    public SearchOverlay searchContainer() {
        return this.searchContainer;
    }

    public PopupWindow popupWindow() {
        return this.popupWindow;
    }

    public ActionConfirmDialog actionConfirmationDialogContainer() {
        return this.actionConfirmationDialogContainer;
    }

    public TooltipService tooltipService() {
        return this.tooltipService;
    }

    public OverlayManager priorityOverlayHandler() {
        return this.priorityOverlayHandler;
    }
}
