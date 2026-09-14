package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.WeightedEngine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public class CoordsWidget extends Draggable {
    static final String COORDINATES_ICON_GLYPH = "\u042E";
    private static final float DEFAULT_MARGIN = 7.0f;
    private static final float TITLE_SIZE = 11.0f;
    private static final float VALUE_SIZE = 11.0f;
    private static final float TEXT_GAP = 7.0f;
    private static final float HEIGHT = 26.0f;
    private static final float HORIZONTAL_PADDING = 8.0f;
    private static final float ICON_SIZE = 12.0f;
    private static final float ICON_GAP = 6.0f;
    private static final float CORNER_RADIUS = 8.0f;
    private static final float DEFAULT_ANCHOR_TOLERANCE = 44.0f;

    private final MsdfFont titleFont = Fonts.INTER_BOLD.get();
    private final MsdfFont valueFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont menuIconFont = Fonts.MENU_ICON.get();
    private final ToggleAnimator chatSlideAnimator =
            new ToggleAnimator(220, Easings.EASE_OUT_CUBIC);

    private float width;
    private boolean positionInitialized;
    private boolean chatAnchored;
    private long previewSecond = Long.MIN_VALUE;
    private int[] previewCoords = {128, 76, -342};
    private String displayedValues = "X 0  Y 0  Z 0";
    private int displayedX = Integer.MIN_VALUE;
    private int displayedY = Integer.MIN_VALUE;
    private int displayedZ = Integer.MIN_VALUE;

    public CoordsWidget(BooleanSupplier visibility) {
        super("Coords", visibility);
        this.x = DEFAULT_MARGIN;
        this.y = DEFAULT_MARGIN;
    }

    @Override
    public void applySavedPosition(float x, float y) {
        super.applySavedPosition(x, y);
        this.positionInitialized = true;
    }

    @Override
    public void layout(DragRenderContext context) {
        if (!isVisible() || context.resolution() == null) {
            return;
        }
        ScreenResolution resolution = context.resolution();
        updateDisplayedValues();
        this.width = calculateWidth(this.displayedValues);
        float defaultY = resolution.screenHeight() - HEIGHT - DEFAULT_MARGIN;
        if (!this.positionInitialized) {
            this.x = DEFAULT_MARGIN;
            this.y = defaultY;
            this.positionInitialized = true;
        }

        boolean chatOpen = Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
        float chatOffset = (float) ((12.0d * Mc.INSTANCE.getWindow().getScaleFactor())
                / Math.max(0.01d, context.scaleFactor()));
        float anchoredY = defaultY - chatOffset * this.chatSlideAnimator.smoothAnimation();

        if (chatOpen && !this.chatAnchored && !this.dragging && isNearDefaultAnchor(defaultY)) {
            this.chatAnchored = true;
        }
        if (this.chatAnchored && this.dragging
                && (Math.abs(this.x - DEFAULT_MARGIN) > DEFAULT_ANCHOR_TOLERANCE
                || Math.abs(this.y - anchoredY) > DEFAULT_ANCHOR_TOLERANCE)) {
            this.chatAnchored = false;
        }
        if (this.chatAnchored) {
            this.y = anchoredY;
            if (!chatOpen && this.chatSlideAnimator.smoothAnimation() <= 0.01f) {
                this.chatAnchored = false;
            }
        }
    }

    private boolean isNearDefaultAnchor(float defaultY) {
        return Math.abs(this.x - DEFAULT_MARGIN) <= DEFAULT_ANCHOR_TOLERANCE
                && Math.abs(this.y - defaultY) <= DEFAULT_ANCHOR_TOLERANCE;
    }

    @Override
    public void render(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        ColorStack colors = draw.colorStack();
        ThemePalette palette = Spectra.INSTANCE.theme().palette();
        String title = "Coordinates";
        String values = this.displayedValues;
        float centerY = this.y + HEIGHT / 2.0f;
        float titleY = centerY - this.titleFont.getHeight(TITLE_SIZE) / 2.0f;
        float iconX = this.x + HORIZONTAL_PADDING;
        float textX = iconX + ICON_SIZE + ICON_GAP;
        float valueX = textX + this.titleFont.getWidth(title, TITLE_SIZE) + TEXT_GAP;
        float valueY = centerY - this.valueFont.getHeight(VALUE_SIZE) / 2.0f;
        SpectraHudStyle.panel(draw, matrices, this.x, this.y, this.width, HEIGHT, CORNER_RADIUS);
        draw.msdfFontVerticalCHorizontalC(
                matrices.peek().getPositionMatrix(), this.menuIconFont, COORDINATES_ICON_GLYPH,
                iconX + ICON_SIZE / 2.0f, centerY,
                ICON_SIZE, 0.05f, colors.computeColor(0xFFFFFFFF)
        );
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.titleFont, title,
                textX, titleY, TITLE_SIZE, 0.05f,
                colors.computeColor(palette.text().tone(100).argb()));
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.valueFont, values,
                valueX, valueY, VALUE_SIZE, 0.05f,
                colors.computeColor(palette.text().tone(400).argb()));
    }

    private float calculateWidth(String values) {
        return this.titleFont.getWidth("Coordinates", TITLE_SIZE)
                + ICON_SIZE + ICON_GAP
                + TEXT_GAP
                + this.valueFont.getWidth(values, VALUE_SIZE)
                + HORIZONTAL_PADDING * 2.0f;
    }

    private static String formatValues(int x, int y, int z) {
        return "X " + x + "  Y " + y + "  Z " + z;
    }

    private void updateDisplayedValues() {
        if (!HudEditorScreen.isEditing() || Mc.INSTANCE.isWorldLoaded()) {
            ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
            if (player == null) {
                setDisplayedValues(0, 0, 0);
            } else {
                setDisplayedValues((int) player.getX(), (int) player.getY(), (int) player.getZ());
            }
            return;
        }
        long second = System.currentTimeMillis() / 1000L;
        if (second != this.previewSecond) {
            this.previewSecond = second;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            this.previewCoords[0] = random.nextInt(-5000, 5001);
            this.previewCoords[1] = random.nextInt(40, 181);
            this.previewCoords[2] = random.nextInt(-5000, 5001);
        }
        setDisplayedValues(this.previewCoords[0], this.previewCoords[1], this.previewCoords[2]);
    }

    private void setDisplayedValues(int x, int y, int z) {
        if (x == this.displayedX && y == this.displayedY && z == this.displayedZ) {
            return;
        }
        this.displayedX = x;
        this.displayedY = y;
        this.displayedZ = z;
        this.displayedValues = formatValues(x, y, z);
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return false;
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.chatSlideAnimator
                .state(Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen)
                .animate(engine);
    }

    @Override
    public void update() {
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    public static int[] getPlayerCoords() {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        return player == null
                ? new int[]{0, 0, 0}
                : new int[]{(int) player.getX(), (int) player.getY(), (int) player.getZ()};
    }
}
