package ru.spectra.client.cape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Owns per-player simulations and feeds them with the exact movement terms
 * used by the old client.
 */
public final class CapePhysicsManager {
    public static final int PART_COUNT = 16;
    private static final float GRAVITY = 25.0f;
    private static final float HEIGHT_MULTIPLIER = 5.0f;
    private static final float STRAFE_MULTIPLIER = 5.0f;
    private static final Map<Integer, Entry> SIMULATIONS = new HashMap<>();

    private CapePhysicsManager() {
    }

    public static void tick(AbstractClientPlayerEntity player) {
        if (MinecraftClient.getInstance().player != player) {
            return;
        }
        Entry entry = SIMULATIONS.get(player.getId());
        UUID uuid = player.getUuid();
        if (entry == null || !entry.uuid.equals(uuid)) {
            entry = new Entry(uuid, new CapeSimulation());
            SIMULATIONS.put(player.getId(), entry);
        }
        entry.underwater = player.isSubmergedInWater() && player.isTouchingWater();
        CapeSimulation simulation = entry.simulation;
        if (simulation.init(PART_COUNT)) {
            simulation.applyMovement(new CapeVector(1.0f, 1.0f, 0.0f));
            for (int i = 0; i < 5; i++) {
                simulate(player, simulation);
            }
        } else {
            simulate(player, simulation);
        }
    }

    public static CapeSimulation get(int entityId) {
        Entry entry = SIMULATIONS.get(entityId);
        return entry == null ? null : entry.simulation;
    }

    public static boolean isUnderwater(int entityId) {
        Entry entry = SIMULATIONS.get(entityId);
        return entry != null && entry.underwater;
    }

    private static void simulate(AbstractClientPlayerEntity player, CapeSimulation simulation) {
        if (simulation.empty()) {
            return;
        }
        double capeOffsetX = player.capeX - player.getX();
        double capeOffsetZ = player.capeZ - player.getZ();
        float yaw = player.bodyYaw;
        double sinYaw = MathHelper.sin(yaw * 0.017453292f);
        double negativeCosYaw = -MathHelper.cos(yaw * 0.017453292f);
        float heightMultiplier = HEIGHT_MULTIPLIER;
        boolean underwater = player.isSubmergedInWater() && player.isTouchingWater();
        if (underwater) {
            heightMultiplier *= 2.0f;
        }
        double fallImpulse = MathHelper.clamp((player.prevY - player.getY()) * 10.0, 0.0, 1.0);
        simulation.setGravity(underwater ? GRAVITY / 10.0f : GRAVITY);

        CapeVector gravity = new CapeVector(0.0f, -1.0f, 0.0f);
        float movementX = (float) (player.getX() - player.prevX);
        float movementZ = (float) (player.getZ() - player.prevZ);
        float strafeX = rotateX(movementX, movementZ, -player.getYaw());
        boolean sneaking = player.isInSneakingPose();
        double changeX = capeOffsetX * sinYaw + capeOffsetZ * negativeCosYaw + fallImpulse
                + (sneaking && !simulation.isSneaking() ? 3.0 : 0.0);
        double changeY = (player.getY() - player.prevY) * heightMultiplier
                + (sneaking && !simulation.isSneaking() ? 1.0 : 0.0);
        double changeZ = -strafeX * STRAFE_MULTIPLIER;
        simulation.setSneaking(sneaking);
        CapeVector change = new CapeVector((float) changeX, (float) changeY, (float) changeZ);
        if (player.isInSwimmingPose()) {
            float rotation = player.getPitch() + 90.0f;
            gravity.rotateDegrees(rotation);
            change.rotateDegrees(rotation);
        }
        simulation.setGravityDirection(gravity);
        simulation.applyMovement(change);
        simulation.simulate();
    }

    private static float rotateX(float x, float y, float degrees) {
        float radians = (float) Math.toRadians(degrees);
        return MathHelper.cos(radians) * x - MathHelper.sin(radians) * y;
    }

    private static final class Entry {
        private final UUID uuid;
        private final CapeSimulation simulation;
        private boolean underwater;

        private Entry(UUID uuid, CapeSimulation simulation) {
            this.uuid = uuid;
            this.simulation = simulation;
        }
    }
}
