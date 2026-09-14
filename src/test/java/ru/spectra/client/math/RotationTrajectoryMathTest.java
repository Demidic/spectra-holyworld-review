package ru.spectra.client.math;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import net.minecraft.util.math.Vec3d;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class RotationTrajectoryMathTest {
    @Test
    void directionConversionPreservesProjectileGeometryAcrossYawAndPitch() {
        for (int yaw = -360; yaw <= 360; yaw += 15) {
            for (int pitch = -90; pitch <= 90; pitch += 10) {
                Vec3d direction = new Rotation(yaw, pitch).getDirectionVector();
                double yawRadians = Math.toRadians(-yaw);
                double pitchRadians = Math.toRadians(pitch);
                assertEquals(Math.sin(yawRadians) * Math.cos(pitchRadians), direction.x, 0.0002);
                assertEquals(-Math.sin(pitchRadians), direction.y, 0.0002);
                assertEquals(Math.cos(yawRadians) * Math.cos(pitchRadians), direction.z, 0.0002);
                assertEquals(1.0, direction.length(), 0.0002);
            }
        }
    }

    @Test
    void multishotOffsetsDoNotMutateTheOriginalProjectileAngles() {
        Rotation original = new Rotation(128.5f, 23.75f);
        Rotation left = original.add(-10.0f, 0.0f);
        Rotation right = original.add(10.0f, 0.0f);
        assertEquals(118.5f, left.getYaw());
        assertEquals(138.5f, right.getYaw());
        assertEquals(23.75f, left.getPitch());
        assertEquals(23.75f, right.getPitch());
        assertEquals(128.5f, original.getYaw());
        assertEquals(23.75f, original.getPitch());
        assertEquals(90.0f, original.add(0.0f, 100.0f).getPitch());
        assertEquals(-90.0f, original.add(0.0f, -200.0f).getPitch());
    }

    @Test
    void compiledMathNoLongerContainsUnusedAimAndMouseStepMethods() throws Exception {
        Set<String> rotationMethods = methods("ru/spectra/client/math/Rotation");
        for (String removed : Set.of("random", "addYaw", "lookingAt", "withFixedYaw",
                "angleDifference", "angleTo", "rotationDeltaTo", "normalize")) {
            assertFalse(rotationMethods.contains(removed), removed);
        }
        assertTrue(rotationMethods.containsAll(Set.of("playerRotation", "add", "getDirectionVector")));
        assertFalse(methods("ru/spectra/client/math/RotationMath").contains("fromVec3d"));
        assertFalse(methods("ru/spectra/client/math/RotationMath").contains("calculateRotationDifference"));
        Set<String> mathMethods = methods("ru/spectra/client/util/MathUtil");
        assertFalse(mathMethods.contains("computeGcd"));
        assertFalse(mathMethods.contains("quadraticBezier"));
        assertTrue(mathMethods.containsAll(Set.of("clamp", "interpolate", "lerp", "rad")));
    }

    @Test
    void unusedRotationDriversAndTargetPointResolverAreNotCompiled() {
        for (String removed : Set.of("math/RotationDelta", "math/RotationVector",
                "event/RotationUpdateEvent", "event/RotationVectorEvent",
                "event/MovementYawEvent", "util/HitPointResolver")) {
            assertNull(getClass().getClassLoader().getResource("ru/spectra/client/" + removed + ".class"), removed);
        }
    }

    private static Set<String> methods(String name) throws Exception {
        try (InputStream input = RotationTrajectoryMathTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node.methods.stream().map(method -> method.name).collect(Collectors.toSet());
        }
    }
}
