package ru.spectra.client.ui;

import ru.spectra.client.math.Easings;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import ru.spectra.mixin.accessors.ItemCooldownEntryAccessor;
import ru.spectra.mixin.accessors.ItemCooldownManagerAccessor;

public final class CooldownsWidget extends Draggable {
    private static final float MIN_WIDTH = 205.0f;
    private static final float MAX_WIDTH = 420.0f;
    private static final float HEADER_HEIGHT = 32.0f;
    private static final float BODY_TOP_PADDING = 3.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float BOTTOM_PADDING = 5.0f;
    private static final int MAX_ROWS = 5;
    private static final List<Item> PREVIEW_ITEMS = List.of(
            Items.ENDER_PEARL,
            Items.GOLDEN_APPLE,
            Items.CHORUS_FRUIT,
            Items.FIRE_CHARGE,
            Items.TOTEM_OF_UNDYING
    );

    private final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont rowFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont timeFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont menuIconFont = Fonts.MENU_ICON.get();
    private final AnimatedFloat visibilityAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat widthAnimation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat[] previewRowAnimations = {
            new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
            new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
            new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC)
    };
    private final Map<Item, CooldownEntry> entries = new HashMap<>();
    private List<CooldownEntry> frameEntries = List.of();
    private float width = MIN_WIDTH;
    private float height = HEADER_HEIGHT + BODY_TOP_PADDING + BOTTOM_PADDING;

    public CooldownsWidget(BooleanSupplier visibility) {
        super("Cooldowns", visibility);
        this.x = 2502.0f;
        this.y = 304.0f;
        this.widthAnimation.set(MIN_WIDTH);
    }

    @Override
    public void layout(DragRenderContext context) {
        if (!isVisible()) {
            this.frameEntries = List.of();
            return;
        }
        this.frameEntries = visibleEntries();
        float animatedRows = 0.0f;
        for (CooldownEntry entry : this.frameEntries) {
            animatedRows += normalizedProgress(entry.animation);
        }
        if (this.frameEntries.isEmpty()) {
            for (AnimatedFloat animation : this.previewRowAnimations) {
                animatedRows += animation.animatedValue();
            }
        }
        this.height = HEADER_HEIGHT + BODY_TOP_PADDING + ROW_HEIGHT * animatedRows + BOTTOM_PADDING;
    }

    @Override
    public void render(DragRenderContext context) {
        float panelAlpha = this.visibilityAnimation.animatedValue();
        if (!isVisible() || panelAlpha <= 0.01f) {
            return;
        }
        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        ColorStack colors = draw.colorStack();
        colors.push();
        colors.alpha(panelAlpha);

        SpectraHudStyle.panel(draw, matrices, this.x, this.y, this.width, this.height, SpectraHudStyle.RADIUS);
        SpectraHudStyle.header(
                draw, matrices,
                this.titleFont, "Cooldowns", this.menuIconFont, "ъ",
                this.x, this.y, this.width,
                colors.computeColor(SpectraHudStyle.TEXT),
                colors.computeColor(0xFFFFFFFF)
        );

        float rowY = this.y + HEADER_HEIGHT + BODY_TOP_PADDING;
        List<CooldownEntry> realEntries = this.frameEntries;
        for (CooldownEntry entry : realEntries) {
            float rowAlpha = normalizedProgress(entry.animation);
            if (rowAlpha <= 0.01f) {
                continue;
            }
            float centerY = rowY + (ROW_HEIGHT / 2.0f) * rowAlpha;
            colors.push();
            colors.alpha(rowAlpha);
            colors.push();
            colors.alpha(cooldownPulse(entry.remainingSeconds));
            draw.itemStack(matrices.peek().getPositionMatrix(), entry.stack, this.x + 9.0f, centerY - 6.5f, 0.40625f, colors.alphaMultiplier());
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(),
                    this.rowFont,
                    entry.name,
                    this.x + 27.0f,
                    centerY + 0.5f - this.rowFont.getHeight(12.0f) / 2.0f,
                    12.0f,
                    0.05f,
                    colors.computeColor(SpectraHudStyle.TEXT)
            );
            colors.pop();
            float timeWidth = this.timeFont.getWidth(entry.time, 10.0f) + 12.0f;
            float timeX = this.x + this.width - 9.0f - timeWidth;
            SpectraHudStyle.outlinedRect(
                    draw, matrices, timeX, centerY - 8.0f, timeWidth, 16.0f, 6.0f,
                    colors.computeColor(SpectraHudStyle.BORDER),
                    SpectraHudStyle.panelColor(colors, SpectraHudStyle.PANEL)
            );
            draw.msdfFont(
                    matrices.peek().getPositionMatrix(),
                    this.timeFont,
                    entry.time,
                    timeX + 6.0f,
                    centerY + 0.5f - this.timeFont.getHeight(10.0f) / 2.0f,
                    10.0f,
                    0.05f,
                    colors.computeColor(SpectraHudStyle.MUTED)
            );
            colors.pop();
            rowY += ROW_HEIGHT * rowAlpha;
        }
        if (realEntries.isEmpty()) {
            long tick = previewTick();
            int first = Math.floorMod((int) tick, PREVIEW_ITEMS.size());
            String[] times = {"4.8s", "3.2s", "1.6s"};
            for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
                float rowAlpha = this.previewRowAnimations[slot].animatedValue();
                if (rowAlpha <= 0.01f) {
                    continue;
                }
                float animatedRowY = rowY + (1.0f - rowAlpha) * 6.0f;
                float centerY = animatedRowY + (ROW_HEIGHT / 2.0f) * rowAlpha;
                colors.push();
                colors.alpha(rowAlpha);
                drawCooldownRow(
                        draw,
                        matrices,
                        colors,
                        PREVIEW_ITEMS.get((first + slot) % PREVIEW_ITEMS.size()).getDefaultStack(),
                        ClientLocalization.text("Example Cooldown", "Пример перезарядки"),
                        times[slot],
                        centerY
                );
                colors.pop();
                rowY += ROW_HEIGHT * rowAlpha;
            }
        }
        colors.pop();
    }

    private void drawCooldownRow(DrawEngine draw, MatrixStack matrices, ColorStack colors,
                                 ItemStack stack, String name, String time, float centerY) {
        draw.itemStack(matrices.peek().getPositionMatrix(), stack, this.x + 9.0f, centerY - 6.5f, 0.40625f, colors.alphaMultiplier());
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.rowFont, name,
                this.x + 27.0f,
                centerY + 0.5f - this.rowFont.getHeight(12.0f) / 2.0f,
                12.0f, 0.05f, colors.computeColor(SpectraHudStyle.TEXT)
        );
        float timeWidth = this.timeFont.getWidth(time, 10.0f) + 12.0f;
        float timeX = this.x + this.width - 9.0f - timeWidth;
        SpectraHudStyle.outlinedRect(
                draw, matrices, timeX, centerY - 8.0f, timeWidth, 16.0f, 6.0f,
                colors.computeColor(SpectraHudStyle.BORDER),
                SpectraHudStyle.panelColor(colors, SpectraHudStyle.PANEL)
        );
        draw.msdfFont(
                matrices.peek().getPositionMatrix(), this.timeFont, time,
                timeX + 6.0f,
                centerY + 0.5f - this.timeFont.getHeight(10.0f) / 2.0f,
                10.0f, 0.05f, colors.computeColor(SpectraHudStyle.MUTED)
        );
    }

    private List<CooldownEntry> visibleEntries() {
        return this.entries.values().stream()
                .filter(entry -> !entry.animation.isZero())
                .sorted((left, right) -> Float.compare(right.progress, left.progress))
                .limit(MAX_ROWS)
                .toList();
    }

    private List<CooldownRow> sensedRows() {
        ArrayList<CooldownRow> rows = new ArrayList<>();
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            Set<Item> seen = new HashSet<>();
            float tickDelta = Mc.INSTANCE.getMinecraft().getRenderTickCounter().getTickDelta(false);
            ItemCooldownManager cooldownManager = player.getItemCooldownManager();
            for (ItemStack stack : player.getInventory().main) {
                if (stack.isEmpty() || !seen.add(stack.getItem())) {
                    continue;
                }
                float progress = cooldownManager.getCooldownProgress(stack, tickDelta);
                if (progress > 0.0f) {
                    float remainingSeconds = remainingSeconds(cooldownManager, stack, tickDelta);
                    rows.add(new CooldownRow(
                            stack,
                            ClientLocalization.itemName(stack),
                            String.format(Locale.ROOT, "%.1fs", remainingSeconds),
                            progress,
                            remainingSeconds
                    ));
                }
            }
        }
        return rows;
    }

    @Override
    public void animate(WeightedEngine engine) {
        Set<Item> active = new HashSet<>();
        for (CooldownRow row : sensedRows()) {
            active.add(row.stack().getItem());
            CooldownEntry entry = this.entries.computeIfAbsent(row.stack().getItem(), ignored -> new CooldownEntry());
            entry.stack = row.stack();
            entry.name = row.name();
            entry.time = row.time();
            entry.progress = row.progress();
            entry.remainingSeconds = row.remainingSeconds();
            entry.animation.state(true).animate(engine);
        }
        for (Map.Entry<Item, CooldownEntry> mapped : this.entries.entrySet()) {
            if (!active.contains(mapped.getKey())) {
                mapped.getValue().animation.state(false).animate(engine);
            }
        }
        this.entries.entrySet().removeIf(mapped -> !active.contains(mapped.getKey()) && mapped.getValue().animation.isZero());
        boolean realRowsVisible = this.entries.values().stream().anyMatch(entry -> !entry.animation.isZero());
        boolean chatOpen = HudEditorScreen.isEditing();
        int previewCount = chatOpen && active.isEmpty() && !realRowsVisible ? previewRowCount() : 0;
        for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
            this.previewRowAnimations[slot].destination(slot < previewCount ? 1.0f : 0.0f).animate(engine);
        }
        boolean previewVisible = java.util.Arrays.stream(this.previewRowAnimations).anyMatch(animation -> !animation.isZero());
        this.visibilityAnimation.destination(realRowsVisible || previewVisible ? 1.0f : 0.0f).animate(engine);
        this.widthAnimation.destination(requiredWidth(realRowsVisible)).animate(engine);
        this.width = this.widthAnimation.animatedValue();
    }

    @Override
    protected boolean isContentVisible() {
        return this.visibilityAnimation.destination() > 0.5f;
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

    private static float normalizedProgress(ToggleAnimator animator) {
        return Math.min(1.0f, animator.smoothAnimation() / animator.maxValueScale);
    }

    private static final class CooldownEntry {
        private final ToggleAnimator animation = ToggleAnimator.times(2, 90);
        private ItemStack stack = ItemStack.EMPTY;
        private String name = "";
        private String time = "";
        private float progress;
        private float remainingSeconds;
    }

    private static float remainingSeconds(ItemCooldownManager manager, ItemStack stack, float tickDelta) {
        ItemCooldownManagerAccessor managerAccessor = (ItemCooldownManagerAccessor) manager;
        Identifier group = manager.getGroup(stack);
        Object rawEntry = managerAccessor.spectra$getEntries().get(group);
        if (!(rawEntry instanceof ItemCooldownEntryAccessor entry)) {
            return manager.getCooldownProgress(stack, tickDelta) * 10.0f;
        }
        return Math.max(0.0f, (entry.spectra$getEndTick() - (managerAccessor.spectra$getTick() + tickDelta)) / 20.0f);
    }

    private static float cooldownPulse(float remainingSeconds) {
        if (remainingSeconds <= 0.0f || remainingSeconds > 5.0f) {
            return 1.0f;
        }
        double phase = (System.currentTimeMillis() % 1400L) / 1400.0 * Math.PI * 2.0;
        return 0.72f + 0.28f * (float) ((Math.sin(phase) + 1.0) * 0.5);
    }

    private float requiredWidth(boolean realRowsVisible) {
        float required = MIN_WIDTH;
        if (realRowsVisible) {
            for (CooldownEntry entry : visibleEntries()) {
                required = Math.max(required, widthForRow(entry.name, entry.time));
            }
        } else {
            required = Math.max(required, widthForRow(
                    ClientLocalization.text("Example Cooldown", "Пример перезарядки"), "4.8s"));
        }
        return Math.min(MAX_WIDTH, required);
    }

    private float widthForRow(String name, String time) {
        float nameWidth = this.rowFont.getWidth(name, 12.0f);
        float timeWidth = this.timeFont.getWidth(time, 10.0f) + 12.0f;
        return 27.0f + nameWidth + 10.0f + timeWidth + 9.0f;
    }

    private static int previewRowCount() {
        int[] pattern = {3, 2, 1, 2, 3, 1, 1};
        return pattern[Math.floorMod((int) previewTick(), pattern.length)];
    }

    private static long previewTick() {
        return Math.floorDiv(System.currentTimeMillis() + 823L, 1000L);
    }

    private record CooldownRow(ItemStack stack, String name, String time, float progress, float remainingSeconds) {
    }
}
