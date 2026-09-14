package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.WorldLoadEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.WorldEffectsRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.WorldEffectType;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@Aliases(aliases = {
        "WorldEffects", "Shader Effects", "World Shaders",
        "Эффекты мира", "Шейдерные эффекты"
})
public final class WorldEffectsModule extends Module {
    private static final double TNT_TRACKING_RADIUS = 160.0d;
    private static final double TNT_MATCH_DISTANCE_SQUARED = 36.0d;
    private static final long TNT_MEMORY_NANOS = 2_000_000_000L;

    public final MultiSelectSetting<WorldEffectType> effects =
            new MultiSelectSetting<WorldEffectType>(
                    Translation.clearText("World effects"),
                    Translation.clearText("Selects events that receive shader effects")
            ).values(WorldEffectType.class).select(
                    WorldEffectType.TNT_EXPLOSION,
                    WorldEffectType.MACE_SMASH,
                    WorldEffectType.ARROW_BLOOD
            );
    public final NumberSetting intensity = new NumberSetting(
            Translation.clearText("Effect intensity"),
            Translation.clearText("Controls refraction, flash and distortion strength")
    ).range(0.25f, 2.0f).currentValue(1.0f).step(0.05f);
    public final NumberSetting blurStrength = new NumberSetting(
            Translation.clearText("Blur strength"),
            Translation.clearText("Controls the liquid-glass blur radius")
    ).range(4.0f, 36.0f).currentValue(18.0f).step(1.0f);
    public final BooleanSetting screenShake = new BooleanSetting(
            Translation.clearText("Shader screen shake"),
            Translation.clearText("Shakes only the rendered world, leaving the HUD stable")
    ).setValue(true);

    private final Mc mc = Mc.INSTANCE;
    private final WorldEffectsRenderer renderer = new WorldEffectsRenderer();
    private final Map<Integer, TntTrace> recentTnt = new HashMap<>();
    private long lastMaceTriggerNanos;
    private Vec3d lastMacePosition = Vec3d.ZERO;

    public WorldEffectsModule() {
        super(ModuleTab.RENDER, "World Effects");
        addSettings(this.effects, this.intensity, this.blurStrength, this.screenShake);
        register(ClientTickEvent.class, ignored -> trackTnt());
        register(PacketReceiveEvent.class, this::onPacket);
        register(AttackEntityEvent.class, this::onAttack);
        register(WorldRenderEvent.class, this::onWorldRender);
        register(WorldLoadEvent.class, ignored -> clearRuntimeState());
    }

    private void trackTnt() {
        if (!isState() || !this.effects.isSelected(WorldEffectType.TNT_EXPLOSION)
                || !this.mc.isWorldLoaded()) {
            this.recentTnt.clear();
            return;
        }

        long now = System.nanoTime();
        this.recentTnt.entrySet().removeIf(entry ->
                now - entry.getValue().lastSeenNanos > TNT_MEMORY_NANOS);
        Box searchArea = this.mc.getPlayer().getBoundingBox().expand(TNT_TRACKING_RADIUS);
        for (TntEntity tnt : this.mc.getWorld().getEntitiesByClass(
                TntEntity.class, searchArea, entity -> !entity.isRemoved())) {
            this.recentTnt.put(tnt.getId(), new TntTrace(tnt.getPos(), now));
        }
    }

    private void onPacket(PacketReceiveEvent event) {
        if (!isState() || !this.mc.isWorldLoaded()) {
            return;
        }
        if (event.getPacket() instanceof ExplosionS2CPacket explosion
                && this.effects.isSelected(WorldEffectType.TNT_EXPLOSION)) {
            triggerTntExplosion(explosion.center());
            return;
        }
        if (event.getPacket() instanceof EntityDamageS2CPacket damage
                && (this.effects.isSelected(WorldEffectType.MACE_SMASH)
                || this.effects.isSelected(WorldEffectType.ARROW_BLOOD))) {
            triggerDamageEffects(damage);
        }
    }

    private void triggerTntExplosion(Vec3d center) {
        long now = System.nanoTime();
        Integer matchedId = null;
        double bestDistance = TNT_MATCH_DISTANCE_SQUARED;
        for (Map.Entry<Integer, TntTrace> entry : this.recentTnt.entrySet()) {
            TntTrace trace = entry.getValue();
            if (now - trace.lastSeenNanos > TNT_MEMORY_NANOS) {
                continue;
            }
            double distance = trace.position.squaredDistanceTo(center);
            if (distance <= bestDistance) {
                matchedId = entry.getKey();
                bestDistance = distance;
            }
        }
        if (matchedId == null) {
            return;
        }
        this.recentTnt.remove(matchedId);
        double playerDistance = this.mc.getPlayer().getPos().distanceTo(center);
        float proximity = 1.0f - (float) Math.min(1.0d, playerDistance / 72.0d);
        this.renderer.trigger(WorldEffectType.TNT_EXPLOSION, center, 0.28f + proximity * 0.92f);
    }

    private void triggerDamageEffects(EntityDamageS2CPacket packet) {
        Entity target = this.mc.getWorld().getEntityById(packet.entityId());
        if (!(target instanceof LivingEntity living) || target == this.mc.getPlayer()) {
            return;
        }
        DamageSource damageSource = packet.createDamageSource(this.mc.getWorld());
        if (this.effects.isSelected(WorldEffectType.MACE_SMASH)
                && damageSource.isOf(DamageTypes.MACE_SMASH)
                && damageSource.getAttacker() == this.mc.getPlayer()) {
            triggerMaceEffect(living);
        }
        if (!this.effects.isSelected(WorldEffectType.ARROW_BLOOD)) {
            return;
        }
        if (!(damageSource.getSource() instanceof ArrowEntity arrow)
                || arrow.getOwner() != this.mc.getPlayer()) {
            return;
        }
        Vec3d impact = living.getPos().add(0.0d, living.getHeight() * 0.58d, 0.0d);
        Vec3d direction = arrow.getVelocity();
        if (direction.lengthSquared() < 0.000001d) {
            direction = impact.subtract(arrow.getPos());
        }
        this.renderer.trigger(
                WorldEffectType.ARROW_BLOOD,
                impact,
                1.0f,
                direction,
                findBloodGround(impact)
        );
    }

    private void onAttack(AttackEntityEvent event) {
        if (!isState() || !this.effects.isSelected(WorldEffectType.MACE_SMASH)
                || !this.mc.isWorldLoaded()
                || !(event.attacker() instanceof LivingEntity target)
                || !this.mc.getPlayer().getMainHandStack().isOf(Items.MACE)
                || !MaceItem.shouldDealAdditionalDamage(this.mc.getPlayer())) {
            return;
        }
        triggerMaceEffect(target);
    }

    private void triggerMaceEffect(LivingEntity target) {
        long now = System.nanoTime();
        Vec3d position = new Vec3d(target.getX(), target.getY() + 0.08d, target.getZ());
        if (now - this.lastMaceTriggerNanos < 750_000_000L
                && position.squaredDistanceTo(this.lastMacePosition) < 4.0d) {
            return;
        }
        this.lastMaceTriggerNanos = now;
        this.lastMacePosition = position;
        this.renderer.trigger(WorldEffectType.MACE_SMASH, position, 1.2f);
    }

    private Vec3d findBloodGround(Vec3d impact) {
        BlockHitResult result = this.mc.getWorld().raycast(new RaycastContext(
                impact.add(0.0d, 0.15d, 0.0d),
                impact.add(0.0d, -12.0d, 0.0d),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this.mc.getPlayer()
        ));
        return result.getType() == HitResult.Type.MISS
                ? impact.add(0.0d, -Math.min(1.6d, this.mc.getPlayer().getHeight()), 0.0d)
                : result.getPos().add(0.0d, 0.018d, 0.0d);
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!isState() || !this.mc.isWorldLoaded()
                || !this.renderer.hasActiveEffects()) {
            return;
        }
        try {
            this.renderer.render(
                    event,
                    this.intensity.currentValue(),
                    Math.round(this.blurStrength.currentValue()),
                    this.screenShake.isValue()
            );
        } catch (RuntimeException error) {
            this.renderer.release();
            Spectra.LOGGER.error(
                    "World Effects framebuffer pass failed; disabling the effect safely",
                    error
            );
            setState(false);
        }
    }

    private void clearRuntimeState() {
        this.recentTnt.clear();
        this.lastMaceTriggerNanos = 0L;
        this.lastMacePosition = Vec3d.ZERO;
        this.renderer.clear();
    }

    @Override
    public void deactivate() {
        this.recentTnt.clear();
        this.lastMaceTriggerNanos = 0L;
        this.lastMacePosition = Vec3d.ZERO;
        this.renderer.release();
        super.deactivate();
    }

    private record TntTrace(Vec3d position, long lastSeenNanos) {
    }
}
