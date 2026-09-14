package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.util.InventoryUtil;
import ru.spectra.client.type.Mc;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.model.PotionEntry;
import ru.spectra.client.model.PotionKey;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PotionListWidget extends Draggable {
    private static final float PANEL_WIDTH = 195.0f;
    private static final float ROW_START = 34.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float BOTTOM_PADDING = 4.0f;
    private static final int VISIBILITY_DURATION_MS = 220;
    private static final List<RegistryEntry<StatusEffect>> PREVIEW_EFFECTS = List.of(
            StatusEffects.SPEED,
            StatusEffects.STRENGTH,
            StatusEffects.HASTE,
            StatusEffects.REGENERATION,
            StatusEffects.RESISTANCE,
            StatusEffects.JUMP_BOOST
    );
    public final MsdfFont nameFont;
    public final MsdfFont durationFont;
    public final GlTexture starsTexture;
    public final MsdfFont menuIconFont;
    public final Map<PotionKey, PotionEntry> entries;
    public final AnimatedFloat openAnimation;
    private final AnimatedFloat[] previewRowAnimations;
    private final ArrayList<PotionEntry> frameEntries = new ArrayList<>();
    private final Comparator<PotionEntry> entryOrder;
    public final WidgetBounds headerBounds;
    public float width;
    public float height;
    public int frameCounter;

    public PotionListWidget(BooleanSupplier booleanSupplier) {
        super("PotionList", booleanSupplier);
        this.nameFont = Fonts.INTER_SEMIBOLD.get();
        this.durationFont = Fonts.INTER_EXTRA_BOLD.get();
        this.starsTexture = new GlTexture(new ClasspathResource("/textures/stars.png"));
        this.menuIconFont = Fonts.MENU_ICON.get();
        this.entries = new HashMap();
        this.openAnimation = new AnimatedFloat(VISIBILITY_DURATION_MS, Easings.EASE_IN_OUT_CUBIC);
        this.entryOrder = Comparator.<PotionEntry, Integer>comparing(entry -> entry.harmful ? 0 : 1)
                .thenComparingDouble(entry -> -this.nameFont.getWidth(entry.getDisplayName(), 12.0f));
        this.previewRowAnimations = new AnimatedFloat[] {
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC)
        };
        this.headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 19.0f);
        this.width = PANEL_WIDTH;
        this.height = 93.0f;
        this.x = 30.0f;
        this.y = 82.0f;
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

    @Override
    public boolean click(MouseButtonInput2 class807Var, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput class808Var, boolean z) {
        return z && !class808Var.intercepted();
    }

    public List<PotionEntry> getVisibleEntries() {
        refreshFrameEntries();
        return List.copyOf(this.frameEntries);
    }

    private void refreshFrameEntries() {
        this.frameEntries.clear();
        for (PotionEntry entry : this.entries.values()) {
            if (!entry.animator.isZero() && entry.effect != null) {
                this.frameEntries.add(entry);
            }
        }
        this.frameEntries.sort(this.entryOrder);
    }

    @Override
    public void layout(DragRenderContext class809Var) {
        if (isVisible()) {
            float animatedRows = 0.0f;
            refreshFrameEntries();
            List<PotionEntry> visibleEntries = this.frameEntries;
            if (visibleEntries.isEmpty()) {
                for (AnimatedFloat animation : this.previewRowAnimations) {
                    animatedRows += animation.animatedValue();
                }
            } else {
                for (PotionEntry entry : visibleEntries) {
                    animatedRows += normalizedProgress(entry.animator);
                }
            }
            this.width = PANEL_WIDTH;
            this.height = ROW_START + BOTTOM_PADDING + animatedRows * ROW_HEIGHT;
            this.headerBounds.withSize(this.width - 18.0f, 19.0f)
                    .withPosition(this.x + 9.0f, this.y);
        }
    }

    @Override
    public void render(DragRenderContext class809Var) {
        if (!isVisible() || this.openAnimation.isZero()) {
            return;
        }
        renderContents(class809Var, this.openAnimation.animatedValue());
    }

    private void renderContents(DragRenderContext class809Var, float alpha) {
        DrawEngine class154VarDrawEngine = class809Var.drawEngine();
        MatrixStack matrixStack = class809Var.matrixStack();
        ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
        class115VarColorStack.push();
        matrixStack.push();
        try {
            class115VarColorStack.alpha(alpha);
            SpectraHudStyle.panel(class154VarDrawEngine, matrixStack, this.x, this.y, this.width, this.height, 8.0f);
            SpectraHudStyle.header(
                    class154VarDrawEngine, matrixStack,
                    this.nameFont, "Active Effects", this.menuIconFont, "u",
                    this.x, this.y, this.width,
                    class115VarColorStack.computeColor(SpectraHudStyle.TEXT),
                    class115VarColorStack.computeColor(0xFFFFFFFF)
            );

            List<PotionEntry> visibleEntries = this.frameEntries;
            float rowY = this.y + ROW_START;
            if (visibleEntries.isEmpty()) {
                long tick = previewTick();
                int first = Math.floorMod((int) tick, PREVIEW_EFFECTS.size());
                String[] durations = {"1:24", "0:42", "0:18"};
                for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
                    float previewProgress = this.previewRowAnimations[slot].animatedValue();
                    if (previewProgress <= 0.01f) {
                        continue;
                    }
                    float animatedRowY = rowY + (1.0f - previewProgress) * 6.0f;
                    class115VarColorStack.push();
                    class115VarColorStack.alpha(previewProgress);
                    drawPreviewRow(
                            class154VarDrawEngine,
                            matrixStack,
                            animatedRowY,
                            PREVIEW_EFFECTS.get((first + slot) % PREVIEW_EFFECTS.size()),
                            ClientLocalization.text("Example Effect", "Пример эффекта"),
                            durations[slot],
                            false
                    );
                    class115VarColorStack.pop();
                    rowY += ROW_HEIGHT * previewProgress;
                }
            } else {
                for (PotionEntry entry : visibleEntries) {
                    float rowAlpha = normalizedProgress(entry.animator);
                    if (rowAlpha <= 0.01f) {
                        continue;
                    }
                    float animatedRowY = rowY + (1.0f - rowAlpha) * 6.0f;
                    class115VarColorStack.push();
                    class115VarColorStack.alpha(rowAlpha);
                    Sprite sprite = Mc.INSTANCE.getMinecraft().getStatusEffectSpriteManager().getSprite(entry.effect);
                    class154VarDrawEngine.texture(matrixStack.peek().getPositionMatrix(), this.x + 9.0f, animatedRowY + 3.5f, 13.0f, 13.0f, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), class154VarDrawEngine.bindTexture(Mc.INSTANCE.getTextureManager().getTexture(sprite.getAtlasId()).getGlId()), class115VarColorStack.white());
                    drawEffectText(class154VarDrawEngine, matrixStack, animatedRowY, englishEffectName(entry), entry.durationText, !entry.infinite && entry.duration > 0 && entry.duration <= 200);
                    class115VarColorStack.pop();
                    rowY += ROW_HEIGHT * rowAlpha;
                }
            }
        } finally {
            matrixStack.pop();
            class115VarColorStack.pop();
        }
    }

    @Override
    protected boolean isContentVisible() {
        return this.openAnimation.destination() > 0.5f;
    }

    private static float normalizedProgress(ru.spectra.client.render.ToggleAnimator animator) {
        return Math.min(1.0f, animator.smoothAnimation() / animator.maxValueScale);
    }

    private void drawPreviewRow(DrawEngine draw, MatrixStack matrices, float rowY, RegistryEntry<StatusEffect> effect, String name, String duration, boolean special) {
        Sprite sprite = Mc.INSTANCE.getMinecraft().getStatusEffectSpriteManager().getSprite(effect);
        draw.texture(matrices.peek().getPositionMatrix(), this.x + 9.0f, rowY + 3.5f, 13.0f, 13.0f, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), draw.bindTexture(Mc.INSTANCE.getTextureManager().getTexture(sprite.getAtlasId()).getGlId()), draw.colorStack().white());
        drawEffectText(draw, matrices, rowY, name, duration, special);
    }

    private void drawEffectText(DrawEngine draw, MatrixStack matrices, float rowY, String name, String duration, boolean special) {
        ColorStack colors = draw.colorStack();
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.nameFont, name, this.x + 26.0f, rowY + 10.0f - this.nameFont.getHeight(12.0f) / 2.0f, 12.0f, 0.05f, colors.computeColor(SpectraHudStyle.TEXT));
        float timerWidth = this.durationFont.getWidth(duration, 10.0f) + 12.0f;
        float timerX = this.x + this.width - 9.0f - timerWidth;
        int border = SpectraHudStyle.BORDER;
        int timerColor = special ? SpectraHudStyle.RED : SpectraHudStyle.MUTED;
        int fill = SpectraHudStyle.PANEL_DARK;
        SpectraHudStyle.outlinedRect(draw, matrices, timerX, rowY + 2.0f,
                timerWidth, 16.0f, 6.0f, colors.computeColor(border),
                SpectraHudStyle.panelColor(colors, fill));
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.durationFont, duration, timerX + 6.0f, rowY + 10.0f - this.durationFont.getHeight(10.0f) / 2.0f, 10.0f, 0.05f, colors.computeColor(timerColor));
    }

    private String englishEffectName(PotionEntry entry) {
        if (entry == null || entry.effect == null) {
            return ClientLocalization.text("Effect", "Эффект");
        }
        String translated = ClientLocalization.minecraft(
                entry.translationKey,
                I18n.translate(entry.translationKey, new Object[0]));
        StringBuilder name = new StringBuilder(translated);
        if (entry.amplifier > 0) {
            name.append(' ').append(toRoman(entry.amplifier + 1));
        }
        return name.toString();
    }

    private static String toRoman(int value) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return value > 0 && value < numerals.length ? numerals[value] : String.valueOf(value);
    }

    @Override
    public void animate(WeightedEngine class141Var) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        boolean chatOpen = HudEditorScreen.isEditing();
        if (player == null) {
            animatePreviewRows(class141Var, chatOpen);
            this.openAnimation.destination(chatOpen ? 1.0f : 0.0f).animate(class141Var);
            return;
        }
        this.frameCounter++;
        boolean hasActiveEffects = false;
        for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
            PotionEntry class648VarComputeIfAbsent = this.entries.computeIfAbsent(new PotionKey(Registries.STATUS_EFFECT.getId((StatusEffect) statusEffectInstance.getEffectType().value()), statusEffectInstance.getAmplifier()), class649Var -> {
                return new PotionEntry();
            });
            class648VarComputeIfAbsent.lastUpdateTick = this.frameCounter;
            boolean z = statusEffectInstance.isInfinite() || statusEffectInstance.getDuration() > 0;
            hasActiveEffects |= z;
            class648VarComputeIfAbsent.animator.state(z).animate(class141Var);
            if (z) {
                class648VarComputeIfAbsent.hasBeenActive = true;
            }
            class648VarComputeIfAbsent.effect = statusEffectInstance.getEffectType();
            class648VarComputeIfAbsent.harmful = ((StatusEffect) statusEffectInstance.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL;
            class648VarComputeIfAbsent.amplifier = statusEffectInstance.getAmplifier();
            class648VarComputeIfAbsent.translationKey = statusEffectInstance.getTranslationKey();
            class648VarComputeIfAbsent.infinite = statusEffectInstance.isInfinite();
            class648VarComputeIfAbsent.duration = statusEffectInstance.getDuration();
            class648VarComputeIfAbsent.durationText = InventoryUtil.INSTANCE.getPotionDuration(statusEffectInstance);
            if (!class648VarComputeIfAbsent.infinite && class648VarComputeIfAbsent.duration > class648VarComputeIfAbsent.previousDuration + 5) {
                class648VarComputeIfAbsent.expiryNotified = false;
            }
            if (!class648VarComputeIfAbsent.infinite && isBuffEffect(class648VarComputeIfAbsent.effect) && !class648VarComputeIfAbsent.expiryNotified && class648VarComputeIfAbsent.previousDuration > 20 && class648VarComputeIfAbsent.duration <= 20) {
                Spectra.INSTANCE.notificationRepository().post(NotificationType.INFO, buildExpiryMessage(class648VarComputeIfAbsent), 2500L);
                class648VarComputeIfAbsent.expiryNotified = true;
            }
            class648VarComputeIfAbsent.previousDuration = class648VarComputeIfAbsent.duration;
            if (class648VarComputeIfAbsent.infinite) {
                class648VarComputeIfAbsent.maxDuration = -1;
            } else if (class648VarComputeIfAbsent.maxDuration <= 0 || class648VarComputeIfAbsent.duration > class648VarComputeIfAbsent.maxDuration) {
                class648VarComputeIfAbsent.maxDuration = class648VarComputeIfAbsent.duration;
            }
        }
        boolean realRowsVisible = false;
        Iterator<Map.Entry<PotionKey, PotionEntry>> iterator = this.entries.entrySet().iterator();
        while (iterator.hasNext()) {
            PotionEntry entry = iterator.next().getValue();
            if (entry.lastUpdateTick != this.frameCounter) {
                entry.animator.state(false).animate(class141Var);
                entry.infinite = false;
            }
            if (this.frameCounter - entry.lastUpdateTick > 40 && entry.animator.isZero()) {
                iterator.remove();
                continue;
            }
            realRowsVisible |= !entry.animator.isZero();
        }
        animatePreviewRows(class141Var, chatOpen && !hasActiveEffects && !realRowsVisible);
        this.openAnimation.destination(hasActiveEffects || chatOpen ? 1.0f : 0.0f).animate(class141Var);
    }

    private void animatePreviewRows(WeightedEngine engine, boolean visible) {
        int previewCount = visible ? previewRowCount() : 0;
        for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
            this.previewRowAnimations[slot].destination(slot < previewCount ? 1.0f : 0.0f).animate(engine);
        }
    }

    private static int previewRowCount() {
        int[] pattern = {2, 1, 2, 3, 1, 3, 2};
        return pattern[Math.floorMod((int) previewTick(), pattern.length)];
    }

    private static long previewTick() {
        return Math.floorDiv(System.currentTimeMillis() + 487L, 1000L);
    }

    public boolean isBuffEffect(RegistryEntry<StatusEffect> registryEntry) {
        Identifier id;
        return (registryEntry == null || (id = Registries.STATUS_EFFECT.getId((StatusEffect) registryEntry.value())) == null || (!id.equals(Registries.STATUS_EFFECT.getId((StatusEffect) StatusEffects.STRENGTH.value())) && !id.equals(Registries.STATUS_EFFECT.getId((StatusEffect) StatusEffects.SPEED.value())))) ? false : true;
    }

    public Text buildExpiryMessage(PotionEntry class648Var) {
        String effectName = ClientLocalization.minecraft(
                class648Var.translationKey,
                I18n.translate(class648Var.translationKey, new Object[0]));
        return Text.literal(ClientLocalization.text("Effect ", "Эффект ")
                + String.valueOf(Formatting.RED)
                + effectName
                + (class648Var.amplifier > 0 ? " " + (class648Var.amplifier + 1) : "")
                + String.valueOf(Formatting.RESET)
                + ClientLocalization.text(" has expired!", " закончился!"));
    }
}
