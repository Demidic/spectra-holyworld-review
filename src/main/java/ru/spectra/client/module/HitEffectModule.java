package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.event.WorldLoadEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.PopChamsModel;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.ColorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Aliases(aliases = {"Hit Effect", "HitEffect", "Critical Effect", "CriticalEffect"})
public final class HitEffectModule extends Module {
    private static final float WAVE_WIDTH = 2.5f;
    private static final int MAX_BLOCKS_PER_FRAME = 400;
    private static final int MAX_ACTIVE_WAVES = 6;
    private static final int MAX_ACTIVE_SKINS = 8;
    private static final long MIN_WAVE_INTERVAL_MS = 100L;

    public final MultiSelectSetting<EffectType> effects =
            new MultiSelectSetting<EffectType>(
                    Translation.clearText("Effects"),
                    Translation.clearText("Allows Wave, Rising skin, or both at once")
            ).values(EffectType.class).select(EffectType.WAVE);
    public final ModeSetting<AttackMode> attackMode = new ModeSetting<AttackMode>(
            Translation.clearText("Hit type"),
            Translation.clearText("Chooses which attacks create the effect")
    ).values(AttackMode.class).currentValue(AttackMode.CRITICAL);
    public final NumberSetting waveRadius = setting("Wave radius", 12.0f, 3.0f, 25.0f, 1.0f)
            .visible(() -> effects.isSelected(EffectType.WAVE));
    public final NumberSetting waveSpeed = setting("Wave speed", 8.0f, 3.0f, 50.0f, 1.0f)
            .visible(() -> effects.isSelected(EffectType.WAVE));
    public final NumberSetting waveOpacity = setting("Wave opacity", 0.8f, 0.3f, 1.0f, 0.1f)
            .visible(() -> effects.isSelected(EffectType.WAVE));
    public final ColorSetting waveColor = new ColorSetting(
            Translation.clearText("Wave color"),
            Translation.clearText("Changes the color of the hit wave")
    ).visible(() -> effects.isSelected(EffectType.WAVE));
    public final NumberSetting skinLifetime = setting("Skin lifetime", 1.35f, 0.4f, 3.0f, 0.05f)
            .visible(() -> effects.isSelected(EffectType.RISING_SKIN));
    public final NumberSetting skinRise = setting("Skin rise height", 2.1f, 0.5f, 4.0f, 0.1f)
            .visible(() -> effects.isSelected(EffectType.RISING_SKIN));
    public final NumberSetting skinOpacity = setting("Skin opacity", 0.72f, 0.1f, 1.0f, 0.05f)
            .visible(() -> effects.isSelected(EffectType.RISING_SKIN));
    public final ColorSetting skinColor = new ColorSetting(
            Translation.clearText("Skin color"),
            Translation.clearText("Tints the rising player skin")
    ).visible(() -> effects.isSelected(EffectType.RISING_SKIN));

    private final Deque<Wave> waves = new ArrayDeque<>();
    private final List<RisingSkin> risingSkins = new ArrayList<>();
    private long lastWaveAt;

    public HitEffectModule() {
        super(ModuleTab.RENDER, "Hit Effect");
        this.waveColor.setColor(0xFF8C7CFF);
        this.skinColor.setColor(0xFF8C7CFF);
        addSettings(
                this.effects,
                this.attackMode,
                this.waveRadius,
                this.waveSpeed,
                this.waveOpacity,
                this.waveColor,
                this.skinLifetime,
                this.skinRise,
                this.skinOpacity,
                this.skinColor
        );
        register(AttackEntityEvent.class, this::onAttack);
        register(WorldRenderEvent.class, this::render);
        register(WorldLoadEvent.class, ignored -> clearWaves());
    }

    private static NumberSetting setting(String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name))
                .currentValue(value)
                .range(min, max)
                .step(step);
    }

    private void onAttack(AttackEntityEvent event) {
        if (!isState() || !(event.attacker() instanceof LivingEntity target)) {
            return;
        }
        if (this.attackMode.isSelected(AttackMode.CRITICAL) && !isVanillaCriticalHit()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (this.effects.isSelected(EffectType.WAVE)
                && now - this.lastWaveAt >= MIN_WAVE_INTERVAL_MS) {
            while (this.waves.size() >= MAX_ACTIVE_WAVES) {
                this.waves.removeFirst();
            }
            this.waves.addLast(new Wave(
                    BlockPos.ofFloored(target.getX(), target.getY() - 0.1d, target.getZ()),
                    now
            ));
            this.lastWaveAt = now;
        }
        if (this.effects.isSelected(EffectType.RISING_SKIN)
                && target instanceof OtherClientPlayerEntity player) {
            while (this.risingSkins.size() >= MAX_ACTIVE_SKINS) {
                dispose(this.risingSkins.removeFirst().model);
            }
            PopChamsModule renderer = Spectra.INSTANCE.moduleRepository().get(PopChamsModule.class);
            PopChamsModel snapshot = renderer.createModel(player, this.skinColor.getColor(), true);
            this.risingSkins.add(new RisingSkin(
                    snapshot,
                    now,
                    Math.round(this.skinLifetime.currentValue() * 1000.0f)
            ));
        }
    }

    private boolean isVanillaCriticalHit() {
        var player = Mc.INSTANCE.getPlayer();
        return player != null
                && player.getAttackCooldownProgress(0.5f) > 0.9f
                && player.fallDistance > 0.0f
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.isSubmergedIn(FluidTags.WATER)
                && !player.isInLava()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle()
                && !player.isGliding()
                && !player.getAbilities().flying
                && !player.isSprinting();
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }

        long now = System.currentTimeMillis();
        renderRisingSkins(event, now);
        if (this.waves.isEmpty()) {
            return;
        }
        float maxRadius = this.waveRadius.currentValue();
        float speed = this.waveSpeed.currentValue();
        Matrix4f matrix = event.matrixStack().peek().getPositionMatrix();
        int baseColor = this.waveColor.getColor();

        this.waves.removeIf(wave -> {
            float elapsedSeconds = (now - wave.startedAt) / 1000.0f;
            float currentRadius = elapsedSeconds * speed;
            if (currentRadius > maxRadius + WAVE_WIDTH) {
                return true;
            }

            float normalizedProgress = Math.min(1.0f, currentRadius / Math.max(0.001f, maxRadius));
            float globalAlpha = (float) Math.pow(1.0f - normalizedProgress, 0.6d);
            float innerRadius = Math.max(0.0f, currentRadius - WAVE_WIDTH);
            float minRadiusSquared = innerRadius * innerRadius;
            float maxRadiusSquared = (currentRadius + 0.5f) * (currentRadius + 0.5f);
            int scanRadius = Math.max(1, (int) Math.ceil(currentRadius + 0.5f));
            int renderedBlocks = 0;

            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    if (renderedBlocks >= MAX_BLOCKS_PER_FRAME) {
                        return false;
                    }

                    float distanceSquared = x * x + z * z;
                    if (distanceSquared < minRadiusSquared || distanceSquared > maxRadiusSquared) {
                        continue;
                    }

                    BlockPos surface = findSurface(wave.center.add(x, 0, z));
                    if (surface == null) {
                        continue;
                    }
                    BlockState state = Mc.INSTANCE.getWorld().getBlockState(surface);
                    VoxelShape shape = state.getOutlineShape(Mc.INSTANCE.getWorld(), surface);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    float distance = (float) Math.sqrt(distanceSquared);
                    float localAlpha = 1.0f
                            - Math.abs(distance - currentRadius) / WAVE_WIDTH;
                    localAlpha = Math.max(0.0f, Math.min(1.0f, localAlpha)) * globalAlpha;
                    if (localAlpha <= 0.05f) {
                        continue;
                    }

                    renderedBlocks++;
                    int alpha = Math.round(255.0f
                            * this.waveOpacity.currentValue()
                            * localAlpha);
                    float lineWidth = 1.0f + localAlpha * 2.5f;
                    renderSurfaceShape(matrix, surface, shape, baseColor, alpha, lineWidth);
                }
            }
            return false;
        });
    }

    private void renderSurfaceShape(Matrix4f matrix, BlockPos pos, VoxelShape shape,
                                    int baseColor, int alpha, float lineWidth) {
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Box box = new Box(
                    pos.getX() + minX,
                    pos.getY() + minY,
                    pos.getZ() + minZ,
                    pos.getX() + maxX,
                    pos.getY() + maxY,
                    pos.getZ() + maxZ
            ).expand(0.004d);
            // The vertical faces are deliberately preserved. They connect the
            // ring when adjacent terrain columns have different heights.
            ShapeRenderer.INSTANCE.addBox(
                    matrix,
                    box,
                    ColorUtil.replAlpha(baseColor, Math.round(alpha * 0.55f)),
                    true,
                    true
            );
            ShapeRenderer.INSTANCE.addOutline(
                    matrix,
                    box,
                    ColorUtil.replAlpha(baseColor, alpha),
                    lineWidth,
                    true,
                    true
            );
        });
    }

    private BlockPos findSurface(BlockPos column) {
        for (int y = 2; y >= -4; y--) {
            BlockPos pos = column.up(y);
            BlockState state = Mc.INSTANCE.getWorld().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape supportShape = state.getCollisionShape(Mc.INSTANCE.getWorld(), pos);
            if (supportShape.isEmpty()) {
                // Grass and other non-colliding decorations must not hide the
                // supporting block below them.
                continue;
            }
            Box bounds = supportShape.getBoundingBox();
            double footprint = (bounds.maxX - bounds.minX) * (bounds.maxZ - bounds.minZ);
            double height = bounds.maxY - bounds.minY;
            if (footprint < 0.45d || height < 0.1d) {
                // Torches, flowers, lanterns and similarly narrow decorations
                // or thin covers are not terrain surfaces. Continue down to
                // their base block.
                continue;
            }
            return pos;
        }
        return null;
    }

    private void renderRisingSkins(WorldRenderEvent event, long now) {
        if (this.risingSkins.isEmpty()) {
            return;
        }
        this.risingSkins.removeIf(skin -> {
            if (now - skin.startedAt < skin.lifetimeMs) {
                return false;
            }
            dispose(skin.model);
            return true;
        });
        if (this.risingSkins.isEmpty()) {
            return;
        }

        PopChamsModule renderer = Spectra.INSTANCE.moduleRepository().get(PopChamsModule.class);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            for (RisingSkin skin : this.risingSkins) {
                float progress = Math.min(1.0f,
                        (now - skin.startedAt) / (float) Math.max(1L, skin.lifetimeMs));
                float fadeIn = Math.min(1.0f, progress / 0.12f);
                float fadeOut = Math.min(1.0f, (1.0f - progress) / 0.32f);
                float opacity = this.skinOpacity.currentValue()
                        * Math.min(smoothStep(fadeIn), fadeOut);
                float rise = this.skinRise.currentValue() * smoothStep(progress);
                renderer.renderModel(
                        event.matrixStack(),
                        skin.model,
                        opacity,
                        rise,
                        this.skinColor.getColor()
                );
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static void dispose(PopChamsModel model) {
        model.getPlayer().remove(Entity.RemovalReason.DISCARDED);
        model.getPlayer().onRemoved();
    }

    private void clearWaves() {
        this.waves.clear();
        this.risingSkins.forEach(skin -> dispose(skin.model));
        this.risingSkins.clear();
        this.lastWaveAt = 0L;
    }

    @Override
    public void deactivate() {
        clearWaves();
        super.deactivate();
    }

    public enum AttackMode implements DisplayNamed {
        CRITICAL("Critical hits only"),
        ALL("All hits");

        private final Translation displayName;

        AttackMode(String displayName) {
            this.displayName = Translation.clearText(displayName);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum EffectType implements DisplayNamed {
        WAVE("Wave"),
        RISING_SKIN("Rising skin");

        private final Translation displayName;

        EffectType(String displayName) {
            this.displayName = Translation.clearText(displayName);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    private record Wave(BlockPos center, long startedAt) {
    }

    private record RisingSkin(PopChamsModel model, long startedAt, long lifetimeMs) {
    }
}
