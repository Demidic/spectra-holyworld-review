package ru.spectra.client.util;
import ru.spectra.client.event.ChunkLoadEvent;
import ru.spectra.client.model.DummyLivingEntity;
import ru.spectra.client.type.EntityLifecycleAction;
import ru.spectra.client.event.EntityLifecycleEvent;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Mc;
import ru.spectra.client.event.PlayerInitEvent;
import ru.spectra.client.model.PlayerSnapshot;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.module.ProjectilePredictionModule;
import ru.spectra.client.math.ProjectileTrajectory;
import ru.spectra.client.model.TrajectoryPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;

public class PlayerSnapshotManager {
    public static final PlayerSnapshotManager INSTANCE = new PlayerSnapshotManager();
    public final List<ProjectileTrajectory> projectiles = new ArrayList();
    public final Mc wrapper = Mc.INSTANCE;
    public final int maxTicks = 40;
    public final Map<Integer, ArrayDeque<PlayerSnapshot>> historyById = new HashMap();
    private LivingEntity historyPlayer;
    private ClientWorld trackingWorld;
    private boolean trackingProjectiles;

    public PlayerSnapshotManager() {
        Spectra.INSTANCE.eventDispatcher().register(PlayerInitEvent.class, class125Var -> {
            this.historyById.clear();
            this.historyPlayer = null;
            this.projectiles.clear();
            this.trackingWorld = null;
            this.trackingProjectiles = false;
        });
        Spectra.INSTANCE.eventDispatcher().register(ChunkLoadEvent.class, class334Var -> {
            if (!ensureProjectileTracking()) return;
            ChunkPos chunkPos = class334Var.chunkPos();
            this.projectiles.stream().filter(class371Var -> {
                return class371Var.chunks.contains(chunkPos);
            }).forEach(class371Var2 -> {
                ProjectilePredictionModule class564Var = (ProjectilePredictionModule) Spectra.INSTANCE.moduleRepository().get(ProjectilePredictionModule.class);
                if (class371Var2.steps.isEmpty()) return;
                TrajectoryPoint class372Var = (TrajectoryPoint) class371Var2.steps.getFirst();
                Entity entity = class371Var2.entity;
                Pair<List<ChunkPos>, List<TrajectoryPoint>> pairPredictEntity = class564Var.predictEntity(entity, class372Var.velocity, class372Var.position, entity instanceof ThrownItemEntity);
                class371Var2.chunks.clear();
                class371Var2.steps.clear();
                class371Var2.chunks.addAll((Collection) pairPredictEntity.getLeft());
                class371Var2.steps.addAll((Collection) pairPredictEntity.getRight());
            });
        });
        Spectra.INSTANCE.eventDispatcher().register(EntityLifecycleEvent.class, class331Var -> {
            Entity thrownItemEntityEntity = class331Var.entity();
            if (thrownItemEntityEntity == Mc.INSTANCE.getPlayer() || Objects.requireNonNull(class331Var.type()) != EntityLifecycleAction.ADD) {
                return;
            }
            if (!ensureProjectileTracking()) return;
            Objects.requireNonNull(thrownItemEntityEntity);
            trackProjectile(thrownItemEntityEntity);
        });
        Spectra.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, class130Var -> {
            ClientWorld world;
            if (class130Var.isPre() && (world = this.wrapper.getWorld()) != null) {
                if (ensureProjectileTracking()) this.projectiles.removeIf(ProjectileTrajectory::tick);
                // The sole history consumer is getMotion(localPlayer) in projectile
                // prediction. Recording every remote player/mob adds O(entities)
                // work and allocations to every tick without serving a feature.
                LivingEntity player = this.wrapper.getPlayer();
                if (player != this.historyPlayer) {
                    this.historyById.clear();
                    this.historyPlayer = player;
                }
                if (player != null) {
                    ArrayDeque<PlayerSnapshot> history = this.historyById.computeIfAbsent(
                            player.getId(), id -> new ArrayDeque<>(41));
                    history.addFirst(PlayerSnapshot.from(player));
                    while (history.size() > 41) history.removeLast();
                }
            }
        }, EventPriority.HIGHEST);
    }

    private boolean ensureProjectileTracking() {
        ClientWorld world = this.wrapper.getWorld();
        boolean enabled = world != null && this.wrapper.getPlayer() != null && Spectra.INSTANCE.moduleRepository()
                .get(ProjectilePredictionModule.class).isState();
        if (!enabled || world != this.trackingWorld) {
            this.projectiles.clear();
            this.trackingProjectiles = false;
            this.trackingWorld = world;
        }
        if (enabled && !this.trackingProjectiles) {
            this.trackingProjectiles = true;
            // Enabling the visual also discovers projectiles already in flight.
            // This scan happens on activation/world change, never every frame.
            for (Entity entity : world.getEntities()) trackProjectile(entity);
        }
        return enabled;
    }

    private void trackProjectile(Entity entity) {
        if (entity.isRemoved()) return;
        if (!(entity instanceof ThrownItemEntity) && !(entity instanceof PersistentProjectileEntity)) return;
        for (ProjectileTrajectory trajectory : this.projectiles) {
            if (trajectory.entity == entity) return;
        }
        ProjectilePredictionModule module = Spectra.INSTANCE.moduleRepository().get(ProjectilePredictionModule.class);
        if (entity instanceof ThrownItemEntity thrown) {
            registerProjectile(thrown, thrown.getStack(), module.predictEntity(thrown,
                    thrown.getVelocity(), thrown.getPos(), true));
        } else if (entity instanceof PersistentProjectileEntity projectile) {
            registerProjectile(projectile, projectile.getItemStack(), module.predictEntity(projectile,
                    projectile.getVelocity(), projectile.getPos(), false));
        }
    }

    public LivingEntity createEntity(EntityType<?> entityType) {
        return new DummyLivingEntity(this, entityType, this.wrapper.getWorld());
    }

    public Optional<PlayerSnapshot> getSnapshot(LivingEntity livingEntity, int i) {
        ArrayDeque<PlayerSnapshot> arrayDeque = this.historyById.get(Integer.valueOf(livingEntity.getId()));
        if (arrayDeque == null) {
            return Optional.empty();
        }
        if (i < 0 || i > 40) {
            return Optional.empty();
        }
        int i2 = 0;
        for (PlayerSnapshot class373Var : arrayDeque) {
            int i3 = i2;
            i2++;
            if (i3 == i) {
                return Optional.of(class373Var);
            }
        }
        return Optional.empty();
    }

    public void registerProjectile(Entity entity, ItemStack itemStack, Pair<List<ChunkPos>, List<TrajectoryPoint>> pair) {
        this.projectiles.removeIf(class371Var -> {
            return class371Var.entity.getUuid().equals(entity.getUuid());
        });
        this.projectiles.add(new ProjectileTrajectory(entity, itemStack, (List) pair.getLeft(), (List) pair.getRight()));
    }

    public PlayerSnapshot getSnapshotOrDefault(LivingEntity livingEntity, int i) {
        return getSnapshot(livingEntity, i).orElseGet(() -> PlayerSnapshot.from(livingEntity));
    }

    public Stream<PlayerSnapshot> getSnapshots(LivingEntity livingEntity, int i) {
        ArrayDeque<PlayerSnapshot> arrayDeque = this.historyById.get(Integer.valueOf(livingEntity.getId()));
        if (arrayDeque == null) {
            return Stream.empty();
        }
        return arrayDeque.stream().limit(Math.min(Math.max(i, 0), 40) + 1);
    }

    public List<ProjectileTrajectory> getProjectiles() {
        ensureProjectileTracking();
        return this.projectiles;
    }

    public Mc getWrapper() {
        return this.wrapper;
    }

    public int getMAX_TICKS() {
        Objects.requireNonNull(this);
        return 40;
    }

    public Map<Integer, ArrayDeque<PlayerSnapshot>> getHistoryById() {
        return this.historyById;
    }
}
