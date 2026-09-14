package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.module.FTHelperModule;
import ru.spectra.client.module.HWHelperModule;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.util.WeightedEngine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public class StructuresWidget extends Draggable {
    private static final float ARC_RADIUS = 18.0f;
    private static final float ARC_THICKNESS = 2.25f;
    private static final float ITEM_SIZE = 18.0f;
    private static final float TEXT_SIZE = 11.0f;
    private static final float TEXT_GAP = 6.0f;
    private static final float SLOT_WIDTH = 44.0f;
    private static final float SLOT_GAP = 10.0f;

    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    private final EnumMap<StructureType, TimerState> timers =
            new EnumMap<>(StructureType.class);
    private final List<StructureType> renderedTypes = new ArrayList<>();
    private float width = SLOT_WIDTH;
    private final float height =
            ARC_RADIUS * 2.0f + TEXT_GAP + Fonts.INTER_SEMIBOLD.get().getHeight(TEXT_SIZE);

    public StructuresWidget(BooleanSupplier visibility) {
        super("Structures", visibility);
        this.x = 10.0f;
        this.y = 50.0f;
        for (StructureType type : StructureType.values()) {
            this.timers.put(type, new TimerState());
        }
    }

    @Override
    public void layout(DragRenderContext context) {
        this.renderedTypes.clear();
        boolean preview = HudEditorScreen.isEditing();
        for (StructureType type : StructureType.values()) {
            TimerState state = this.timers.get(type);
            if (preview || state.remainingTime > 0.0d
                    || state.visibility.animatedValue() > 0.01f) {
                this.renderedTypes.add(type);
            }
        }
        int count = Math.max(1, this.renderedTypes.size());
        this.width = SLOT_WIDTH * count + SLOT_GAP * Math.max(0, count - 1);
    }

    @Override
    public void render(DragRenderContext context) {
        if (this.renderedTypes.isEmpty()) {
            return;
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        ColorStack colors = draw.colorStack();
        ThemePalette palette = Spectra.INSTANCE.theme().palette();
        for (int index = 0; index < this.renderedTypes.size(); index++) {
            StructureType type = this.renderedTypes.get(index);
            TimerState state = this.timers.get(type);
            float visibility = state.visibility.animatedValue();
            if (visibility <= 0.01f) {
                continue;
            }
            float slotX = this.x + index * (SLOT_WIDTH + SLOT_GAP);
            float centerX = slotX + SLOT_WIDTH / 2.0f;
            float centerY = this.y + ARC_RADIUS;
            float progress = MathUtil.clamp(
                    state.progress.animatedValue(), 0.0f, 1.0f);

            colors.push();
            colors.alpha(visibility);
            int trackColor = colors.computeColor(
                    palette.surfaceBackground().tone(300).argb());
            int progressColor = colors.computeColor(palette.accent().argb());
            draw.arc(matrices.peek().getPositionMatrix(), centerX, centerY,
                    ARC_RADIUS, 0.0f, 360.0f, ARC_THICKNESS, trackColor);
            drawProgressArc(draw, matrices, centerX, centerY, progress, progressColor);
            draw.itemStack(
                    matrices.peek().getPositionMatrix(),
                    type.iconItem.getDefaultStack(),
                    centerX - ITEM_SIZE / 2.0f,
                    centerY - ITEM_SIZE / 2.0f,
                    ITEM_SIZE / 32.0f,
                    1.0f
            );

            String time = formatTime(state.displayedTime(HudEditorScreen.isEditing()));
            float textX = slotX + (SLOT_WIDTH - this.font.getWidth(time, TEXT_SIZE)) / 2.0f;
            float textY = this.y + ARC_RADIUS * 2.0f + TEXT_GAP;
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(), this.font, time,
                    textX, textY, TEXT_SIZE, 0.05f,
                    colors.computeColor(palette.text().tone(100).argb())
            );
            colors.pop();
        }
    }

    private void drawProgressArc(DrawEngine draw, MatrixStack matrices,
                                 float centerX, float centerY,
                                 float progress, int color) {
        float start = 270.0f - 360.0f * progress;
        if (start >= 0.0f) {
            draw.arc(matrices.peek().getPositionMatrix(), centerX, centerY,
                    ARC_RADIUS, start, 270.0f, ARC_THICKNESS, color);
            return;
        }
        draw.arc(matrices.peek().getPositionMatrix(), centerX, centerY,
                ARC_RADIUS, 360.0f + start, 360.0f, ARC_THICKNESS, color);
        draw.arc(matrices.peek().getPositionMatrix(), centerX, centerY,
                ARC_RADIUS, 0.0f, 270.0f, ARC_THICKNESS, color);
    }

    private static String formatTime(double seconds) {
        return String.format(Locale.US, "%.3fs", Math.max(0.0d, seconds));
    }

    private double computeRemainingTime(StructureType type) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return 0.0d;
        }
        FTHelperModule helper = Spectra.INSTANCE.moduleRepository().get(FTHelperModule.class);
        HWHelperModule hwHelper = Spectra.INSTANCE.moduleRepository().get(HWHelperModule.class);
        long now = System.currentTimeMillis();
        double ftTime = !helper.isState() || !helper.structureTimer.isValue() ? 0.0d
                : helper.getStructures().stream()
                .filter(structure -> structure.item() == type.trackedItem
                        && structure.anarchy() == ServerUtil.getAnarchy()
                        && ServerUtil.getWorldType().equals(structure.world())
                        && structure.vec().distanceTo(player.getPos()) <= 12.0d)
                .mapToDouble(structure -> (structure.time() - now) / 1000.0d)
                .filter(time -> time > 0.0d)
                .max()
                .orElse(0.0d);
        double hwTime = !hwHelper.isState() || !hwHelper.structureTimerSetting.isValue() ? 0.0d
                : hwHelper.getStructures().stream()
                .filter(structure -> structure.item() == type.trackedItem
                        && structure.anarchy() == ServerUtil.getAnarchy()
                        && ServerUtil.getWorldType().equals(structure.world())
                        && structure.vec().distanceTo(player.getPos()) <= 12.0d)
                .mapToDouble(structure -> (structure.time() - now) / 1000.0d)
                .filter(time -> time > 0.0d)
                .max()
                .orElse(0.0d);
        return Math.max(ftTime, hwTime);
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
        boolean preview = HudEditorScreen.isEditing();
        long now = System.currentTimeMillis();
        for (StructureType type : StructureType.values()) {
            TimerState state = this.timers.get(type);
            state.remainingTime = computeRemainingTime(type);
            boolean active = state.remainingTime > 0.0d;
            if (active) {
                state.lastActiveTime = state.remainingTime;
                state.maxTime = Math.max(state.maxTime, state.remainingTime);
                state.progress.destination((float) MathUtil.clamp(
                        state.remainingTime / Math.max(state.maxTime, 0.001d),
                        0.0d, 1.0d));
            } else {
                state.maxTime = 0.0d;
                state.updatePreview(now, preview);
                double phase = (now - state.previewStartedAt)
                        / Math.max(1.0d, state.previewDuration * 1000.0d);
                state.progress.destination((float) (0.5d - 0.5d
                        * Math.cos(phase * Math.PI * 4.0d)));
            }
            state.visibility.destination(preview || active ? 1.0f : 0.0f);
            state.visibility.animate(engine);
            state.progress.animate(engine);
        }
    }

    @Override
    protected boolean isContentVisible() {
        for (TimerState state : this.timers.values()) {
            if (state.visibility.destination() > 0.5f) {
                return true;
            }
        }
        return false;
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
        return this.height;
    }

    private enum StructureType {
        TRAP(Items.NETHERITE_SCRAP, Items.NETHERITE_SCRAP),
        PLAST(Items.DRIED_KELP, Items.DRIED_KELP),
        HW_STUN(Items.NETHER_STAR, Items.NETHER_STAR),
        HW_EXPLOSIVE_TRAP(Items.PRISMARINE_SHARD, Items.PRISMARINE_SHARD);

        private final Item trackedItem;
        private final Item iconItem;

        StructureType(Item trackedItem, Item iconItem) {
            this.trackedItem = trackedItem;
            this.iconItem = iconItem;
        }
    }

    private static final class TimerState {
        private final AnimatedFloat visibility =
                new AnimatedFloat(250, Easings.LINEAR);
        private final AnimatedFloat progress =
                new AnimatedFloat(250, Easings.LINEAR);
        private double remainingTime;
        private double maxTime;
        private double lastActiveTime;
        private double previewRemainingTime;
        private double previewDuration;
        private long previewStartedAt;

        private TimerState() {
            resetPreview(System.currentTimeMillis());
        }

        private void updatePreview(long now, boolean preview) {
            if (!preview) {
                return;
            }
            double elapsed = (now - this.previewStartedAt) / 1000.0d;
            if (elapsed >= this.previewDuration || elapsed < 0.0d) {
                resetPreview(now);
                elapsed = 0.0d;
            }
            this.previewRemainingTime =
                    Math.max(0.0d, this.previewDuration - elapsed);
        }

        private void resetPreview(long now) {
            this.previewDuration =
                    ThreadLocalRandom.current().nextDouble(10.0d, 20.0001d);
            this.previewRemainingTime = this.previewDuration;
            this.previewStartedAt = now;
        }

        private double displayedTime(boolean preview) {
            if (this.remainingTime > 0.0d) {
                return this.remainingTime;
            }
            if (preview) {
                return this.previewRemainingTime;
            }
            return this.lastActiveTime;
        }
    }
}
