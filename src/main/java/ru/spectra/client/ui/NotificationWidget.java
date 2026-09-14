package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.event.ModuleStateEvent;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.type.NotificationDirection;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WeightedEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class NotificationWidget extends Draggable {
    public float width;
    public float height;
    public final GlTexture backgroundTexture;
    public static final float rowHeight = 24.0f;
    private static final float ROW_GAP = 4.0f;
    private static final float FONT_SIZE = 11.0f;
    private static final float ICON_SIZE = 14.0f;
    private static final float EVENT_ICON_SIZE = 10.0f;
    private static final int ERROR_ICON_COLOR = 0xFFDB3254;
    private static final int INFO_ICON_COLOR = 0xFF8187FF;
    private static final int WARNING_ICON_COLOR = 0xFFFFA463;
    private static final int SUCCESS_ICON_COLOR = 0xFF26C68C;
    private static final float MIN_WIDTH = 110.0f;
    private static final float MAX_WIDTH = 400.0f;
    public Notification previewNotification;
    public final AnimatedFloat previewAnimation;
    public static final Translation placeholderText = Lang.NOTIFICATIONS_PLACEHOLDER;
    public int previewTypeIndex;
    public final Stopwatch previewCycleStopwatch;
    public final List<ModuleStateEvent> pendingModuleEvents;
    public final Stopwatch moduleEventStopwatch;
    public final MsdfFont font;
    private final MsdfFont iconFont;
    private final GlTexture cometIcon;

    public final ModeSetting<NotificationDirection> direction;

    public final BooleanSetting itemPickUp;
    private boolean positionInitialized;
    private int lastLayoutScreenWidth;
    private int lastLayoutScreenHeight;

    public NotificationWidget(BooleanSupplier booleanSupplier) {
        super("Notifications", booleanSupplier);
        this.backgroundTexture = new GlTexture(new ClasspathResource("/textures/hud_background.png"));
        this.previewAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.previewTypeIndex = 0;
        this.previewCycleStopwatch = new Stopwatch();
        this.pendingModuleEvents = new ArrayList();
        this.moduleEventStopwatch = new Stopwatch();
        this.font = Fonts.INTER_BOLD.get();
        this.iconFont = Fonts.MENU_ICON.get();
        this.cometIcon = new GlTexture(new ClasspathResource("/icons/notify/comet.png"));
        this.direction = new ModeSetting(Lang.WIDGET_NOTIFICATIONS_DIRECTION, Lang.WIDGET_NOTIFICATIONS_DIRECTION_DESC).values(NotificationDirection.class);
        this.itemPickUp = new BooleanSetting(Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP, Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP_DESC);
        // Direction is resolved automatically from the widget position.
        addSettings(this.itemPickUp);
        Spectra.INSTANCE.eventDispatcher().register(ModuleStateEvent.class, class080Var -> {
            if (isEnabled() && Mc.INSTANCE.isWorldLoaded() && class080Var.module().isVisibleInMenu()) {
                synchronized (this.pendingModuleEvents) {
                    this.pendingModuleEvents.add(class080Var);
                    this.moduleEventStopwatch.reset();
                }
            }
        });
        this.x = 0.0f;
        this.y = 0.0f;
        this.width = 400.0f;
        this.height = rowHeight;
    }

    @Override
    public void applySavedPosition(float x, float y) {
        super.applySavedPosition(x, y);
        this.positionInitialized = true;
    }

    @Override
    public void layout(DragRenderContext context) {
        preservePositionAcrossResize(context);
        float nextWidth = MIN_WIDTH;
        float nextHeight = 0.0f;
        if (this.previewNotification != null && this.previewNotification.valueAnimation().animatedValue() > 0.0f) {
            nextWidth = notificationWidth(this.previewNotification);
            nextHeight = rowHeight;
        } else {
            float offset = 0.0f;
            for (Notification notification : Spectra.INSTANCE.notificationRepository().getNotifications()) {
                float progress = notification.valueAnimation().animatedValue();
                if (progress <= 0.0f) {
                    continue;
                }
                nextWidth = Math.max(nextWidth, notificationWidth(notification));
                nextHeight = offset + rowHeight;
                offset += (rowHeight + ROW_GAP) * progress;
            }
        }
        this.width = nextWidth;
        this.height = nextHeight;
        if (context.resolution() != null) {
            float screenWidth = context.resolution().screenWidth();
            float screenHeight = context.resolution().screenHeight();
            if (!this.positionInitialized
                    || this.y + Math.max(this.height, rowHeight) < 0.0f || this.y > screenHeight) {
                this.y = screenHeight * 0.28f;
                this.positionInitialized = true;
            }
            this.x = (screenWidth - this.width) / 2.0f;
            this.y = MathUtil.clamp(this.y, 0.0f,
                    Math.max(0.0f, screenHeight - Math.max(this.height, rowHeight)));
            this.lastLayoutScreenWidth = Math.round(screenWidth);
            this.lastLayoutScreenHeight = Math.round(screenHeight);
        }
    }

    private void preservePositionAcrossResize(DragRenderContext context) {
        if (context.resolution() == null) {
            return;
        }
        int screenWidth = context.resolution().screenWidth();
        int screenHeight = context.resolution().screenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        if (this.lastLayoutScreenWidth <= 0 || this.lastLayoutScreenHeight <= 0) {
            this.lastLayoutScreenWidth = screenWidth;
            this.lastLayoutScreenHeight = screenHeight;
            return;
        }
        if (!this.positionInitialized
                || (screenWidth == this.lastLayoutScreenWidth
                && screenHeight == this.lastLayoutScreenHeight)) {
            return;
        }

        float visibleHeight = Math.max(this.height, rowHeight);
        float centerRatioY = (this.y + visibleHeight / 2.0f)
                / this.lastLayoutScreenHeight;
        this.x = (screenWidth - this.width) / 2.0f;
        this.y = centerRatioY * screenHeight - visibleHeight / 2.0f;
    }

    @Override
    public void render(DragRenderContext class809Var) throws MatchException {
        if (isVisible()) {
            MatrixStack matrixStack = class809Var.matrixStack();
            DrawEngine class154VarDrawEngine = class809Var.drawEngine();
            ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
            float f = 0.0f;
            List<Notification> notifications = Spectra.INSTANCE.notificationRepository().getNotifications();
            if (this.previewNotification != null) {
                float fAnimatedValue = this.previewNotification.valueAnimation().animatedValue();
                if (fAnimatedValue > 0.0f) {
                    float widthWithStyles = notificationWidth(this.previewNotification);
                    float rowX = this.x + (this.width - widthWithStyles) / 2.0f;
                    renderNotification(class809Var.drawEngine(), class809Var.theme().palette(), class809Var.matrixStack(), class809Var.drawEngine().colorStack(), this.previewNotification, rowX, this.y, widthWithStyles, rowHeight, fAnimatedValue);
                    return;
                }
            }
            for (int i = 0; i < notifications.size(); i++) {
                Notification class657Var = notifications.get(i);
                float fAnimatedValue2 = class657Var.valueAnimation().animatedValue();
                if (fAnimatedValue2 > 0.0f) {
                    float widthWithStyles2 = notificationWidth(class657Var);
                    float rowX = this.x + (this.width - widthWithStyles2) / 2.0f;
                    renderNotification(class154VarDrawEngine, class809Var.theme().palette(), matrixStack, class115VarColorStack, class657Var, rowX, this.y + f, widthWithStyles2, rowHeight, fAnimatedValue2);
                    f += (rowHeight + ROW_GAP) * fAnimatedValue2;
                }
            }
        }
    }

    public void renderNotification(DrawEngine class154Var, ThemePalette class764Var, MatrixStack matrixStack, ColorStack class115Var, Notification class657Var, float f, float f2, float f3, float f4, float f5) {
        SpectraHudStyle.outlinedRect(class154Var, matrixStack, f, f2, f3, f4, 8.0f,
                class115Var.computeColor(SpectraHudStyle.NOTIFICATION_BORDER, f5),
                SpectraHudStyle.panelAlpha(class115Var, SpectraHudStyle.PANEL_DARK, f5));
        float f6 = f + 7.0f;
        if (class657Var.itemStack() == null || class657Var.itemStack().isEmpty()) {
            if (isModuleToggle(class657Var.type())) {
                renderModuleToggle(class154Var, class764Var, matrixStack,
                        class115Var, class657Var, f6,
                        f2 + (f4 - 14.0f) / 2.0f, f5);
            } else {
                renderTypeIcon(class154Var, matrixStack, class115Var,
                        class657Var.type(), f6, f2 + f4 / 2.0f, f5);
            }
        } else {
            class154Var.itemStack(matrixStack.peek().getPositionMatrix(), class657Var.itemStack(), f6, (f2 + (f4 / 2.0f)) - 6.0f, 0.375f, f5);
        }
        boolean itemIcon = class657Var.itemStack() != null && !class657Var.itemStack().isEmpty();
        float textX = f6 + (isModuleToggle(class657Var.type()) && !itemIcon
                ? 28.0f : itemIcon ? 18.0f : 20.0f);
        class154Var.msdfText(matrixStack.peek().getPositionMatrix(), this.font, class657Var.message(), textX, (f2 + (f4 / 2.0f)) - (this.font.getHeight(FONT_SIZE) / 2.0f), FONT_SIZE, class115Var.computeColor(SpectraHudStyle.TEXT, f5));
    }

    private float notificationWidth(Notification notification) {
        float textWidth = this.font.getWidthWithStyles(notification.message(), FONT_SIZE);
        boolean itemIcon = notification.itemStack() != null && !notification.itemStack().isEmpty();
        float leading = isModuleToggle(notification.type()) && !itemIcon
                ? 45.0f : itemIcon ? 35.0f : 37.0f;
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, leading + textWidth));
    }

    private void renderTypeIcon(DrawEngine draw, MatrixStack matrices, ColorStack colors,
                                NotificationType type, float x, float centerY, float opacity) {
        String glyph = iconGlyph(type);
        if (glyph == null) {
            draw.texture(
                    matrices.peek().getPositionMatrix(),
                    x + (ICON_SIZE - EVENT_ICON_SIZE) / 2.0f,
                    centerY - EVENT_ICON_SIZE / 2.0f,
                    EVENT_ICON_SIZE, EVENT_ICON_SIZE,
                    draw.bindTexture(this.cometIcon.textureWithSTB()),
                    colors.computeColor(0xFFFFFF, opacity)
            );
            return;
        }
        int color = iconColor(type);
        if (hasIconBackground(type)) {
            draw.roundedRectangle(
                    matrices.peek().getPositionMatrix(),
                    x, centerY - ICON_SIZE / 2.0f,
                    ICON_SIZE, ICON_SIZE, 4.0f,
                    colors.computeColor(color, 0.18f * opacity)
            );
        }
        draw.msdfFontVerticalCHorizontalC(
                matrices.peek().getPositionMatrix(), this.iconFont, glyph,
                x + ICON_SIZE / 2.0f, centerY,
                iconGlyphSize(type),
                0.05f, colors.computeColor(color, opacity)
        );
    }

    static float iconGlyphSize(NotificationType type) {
        return switch (type) {
            case INFO -> 7.5f;
            case SUCCESS -> 8.5f;
            case ERROR, WARNING -> 10.0f;
            default -> EVENT_ICON_SIZE;
        };
    }

    static String iconGlyph(NotificationType type) {
        return switch (type) {
            case ERROR -> "\u0442";
            case INFO -> "\u041D";
            case WARNING -> "6";
            case SUCCESS -> "T";
            case EVENT -> null;
            default -> null;
        };
    }

    static int iconColor(NotificationType type) {
        return switch (type) {
            case ERROR -> ERROR_ICON_COLOR;
            case INFO -> INFO_ICON_COLOR;
            case WARNING -> WARNING_ICON_COLOR;
            case SUCCESS -> SUCCESS_ICON_COLOR;
            default -> 0xFFFFFFFF;
        };
    }

    static boolean hasIconBackground(NotificationType type) {
        return type == NotificationType.INFO || type == NotificationType.SUCCESS;
    }

    private void renderModuleToggle(DrawEngine draw, ThemePalette palette,
                                    MatrixStack matrices, ColorStack colors,
                                    Notification notification, float x,
                                    float y, float opacity) {
        float progress =
                notification.moduleToggleAnimation().smoothAnimation();
        int offBackground = colors.computeColor(
                0xFFFFFF, Math.round(11.0f * opacity));
        int onBackground = colors.computeColor(
                0x8B87FF, Math.round(194.0f * opacity));
        int background =
                colors.interpolate(offBackground, onBackground, progress);
        draw.roundedRectangle(
                matrices.peek().getPositionMatrix(),
                x, y, 22.0f, 14.0f, 7.0f, background
        );
        float knobX = x + 7.0f + 8.0f * progress;
        int offKnob = colors.computeColor(
                0x7D7D89, Math.round(255.0f * opacity));
        int onKnob = colors.computeColor(
                0xFFFFFF, Math.round(255.0f * opacity));
        draw.circle(
                matrices.peek().getPositionMatrix(),
                knobX, y + 7.0f, 5.0f,
                colors.interpolate(offKnob, onKnob, progress)
        );
    }

    private static boolean isModuleToggle(NotificationType type) {
        return type == NotificationType.MODULE_ENABLED
                || type == NotificationType.MODULE_DISABLED;
    }

    @Override
    public void animate(WeightedEngine class141Var) {
        if (isVisible()) {
            Spectra.INSTANCE.notificationRepository().animate(class141Var);
            this.previewAnimation.animate(class141Var);
            if (this.previewNotification != null) {
                this.previewNotification.moduleToggleAnimation()
                        .animate(class141Var);
            }
        }
    }

    @Override
    protected boolean isContentVisible() {
        if (this.previewNotification != null
                && this.previewNotification.valueAnimation().destination() > 0.5f) {
            return true;
        }
        for (Notification notification : Spectra.INSTANCE.notificationRepository().getNotifications()) {
            if (notification.valueAnimation().destination() > 0.5f) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update() {
        if (isVisible()) {
            synchronized (this.pendingModuleEvents) {
                if (!this.pendingModuleEvents.isEmpty()) {
                    ArrayList<ModuleStateEvent> arrayList = new ArrayList(this.pendingModuleEvents);
                    this.pendingModuleEvents.clear();
                    Map<String, Boolean> latestStates = new LinkedHashMap<>();
                    for (ModuleStateEvent class080Var : arrayList) {
                        if (!class080Var.module().isVisibleInMenu()) {
                            continue;
                        }
                        latestStates.put(class080Var.module().getName(), class080Var.moduleState());
                    }
                    for (Map.Entry<String, Boolean> state : latestStates.entrySet()) {
                        boolean enabled = state.getValue();
                        String message = (enabled
                                ? Lang.NOTIFICATIONS_ENABLED_SINGLE.effective()
                                : Lang.NOTIFICATIONS_DISABLED_SINGLE.effective())
                                .replace("{module}", state.getKey());
                        Spectra.INSTANCE.notificationRepository().upsert(
                                "module:" + state.getKey().toLowerCase(java.util.Locale.ROOT),
                                enabled ? NotificationType.MODULE_ENABLED : NotificationType.MODULE_DISABLED,
                                Text.literal(message),
                                3L,
                                TimeUnit.SECONDS
                        );
                    }
                }
            }
            Spectra.INSTANCE.notificationRepository().tick();
            boolean z = HudEditorScreen.isEditing();
            boolean z2 = !Spectra.INSTANCE.notificationRepository().getNotifications().isEmpty();
            if (!z || z2) {
                if (this.previewNotification != null) {
                    this.previewAnimation.destination(0.0f);
                    if (this.previewAnimation.animatedValue() <= 0.01f) {
                        this.previewNotification = null;
                        return;
                    }
                    return;
                }
                return;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (this.previewNotification == null) {
                this.previewAnimation.destination(1.0f);
                this.previewNotification = new Notification(this.previewAnimation, NotificationType.INFO, Text.literal(placeholderText.effective()), jCurrentTimeMillis, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
                return;
            }
            this.previewAnimation.destination(1.0f);
            if (this.previewCycleStopwatch.hasElapsed(700L, TimeUnit.MILLISECONDS)) {
                NotificationType[] class659VarArrValues = NotificationType.values();
                this.previewTypeIndex = (this.previewTypeIndex + 1) % class659VarArrValues.length;
                this.previewNotification = new Notification(this.previewAnimation, class659VarArrValues[this.previewTypeIndex], Text.literal(placeholderText.effective()), jCurrentTimeMillis, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
            }
        }
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public boolean click(MouseButtonInput2 class807Var, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput class808Var, boolean z) {
        return z && !class808Var.intercepted();
    }

    @Override
    public boolean isHorizontalDragLocked() {
        return true;
    }

    public BooleanSetting getItemPickUp() {
        return this.itemPickUp;
    }
}
