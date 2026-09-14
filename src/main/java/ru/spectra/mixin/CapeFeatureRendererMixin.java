package ru.spectra.mixin;

import ru.spectra.client.cape.CapePhysicsManager;
import ru.spectra.client.cape.CapeSimulation;
import ru.spectra.client.type.Mc;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the rigid vanilla cape for Spectra's local cape with the segmented
 * WaveyCapes mesh and the original 1.16.5 stick simulation.
 */
@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
    @Unique
    private static final float CAPE_WIDTH = 0.3f;
    @Unique
    private static final float CAPE_HEIGHT = 0.96f;
    @Unique
    private static final float CAPE_DEPTH = 0.06f;

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void spectra$renderPhysicalCape(MatrixStack matrices, VertexConsumerProvider providers, int light,
                                            PlayerEntityRenderState state, float limbAngle, float limbDistance,
                                            CallbackInfo ci) {
        if (state.name == null || !state.name.equals(Mc.INSTANCE.getSession().getUsername())) {
            return;
        }
        if (state.invisible || !state.capeVisible || state.skinTextures == null
                || state.skinTextures.capeTexture() == null || state.equippedChestStack.isOf(Items.ELYTRA)) {
            return;
        }
        CapeSimulation simulation = CapePhysicsManager.get(state.id);
        if (simulation == null || simulation.points().size() != CapePhysicsManager.PART_COUNT) {
            return;
        }

        VertexConsumer vertices = providers.getBuffer(RenderLayer.getEntitySolid(state.skinTextures.capeTexture()));
        renderSmoothCape(matrices, vertices, light, state, simulation,
                Math.min(1.0f, Math.max(0.0f, Mc.INSTANCE.getTickDelta())));
        ci.cancel();
    }

    @Unique
    private static void renderSmoothCape(MatrixStack matrices, VertexConsumer vertices, int light,
                                         PlayerEntityRenderState state, CapeSimulation simulation, float delta) {
        Matrix4f previous = null;
        for (int part = 0; part < CapePhysicsManager.PART_COUNT; part++) {
            matrices.push();
            transformPart(matrices, state, simulation, delta, part);
            Matrix4f current = new Matrix4f(matrices.peek().getPositionMatrix());
            if (previous == null) {
                previous = new Matrix4f(current);
            }

            if (part == 0) {
                emitTop(vertices, current, previous, CAPE_WIDTH, 0.0f, 0.0f,
                        -CAPE_WIDTH, 0.0f, -CAPE_DEPTH, light);
            }
            if (part == CapePhysicsManager.PART_COUNT - 1) {
                float bottom = (part + 1) * (CAPE_HEIGHT / CapePhysicsManager.PART_COUNT);
                emitBottom(vertices, current, current, CAPE_WIDTH, bottom, 0.0f,
                        -CAPE_WIDTH, bottom, -CAPE_DEPTH, part, light);
            }

            float top = part * (CAPE_HEIGHT / CapePhysicsManager.PART_COUNT);
            float bottom = (part + 1) * (CAPE_HEIGHT / CapePhysicsManager.PART_COUNT);
            emitLeft(vertices, current, previous, -CAPE_WIDTH, bottom, 0.0f,
                    -CAPE_WIDTH, top, -CAPE_DEPTH, part, light);
            emitRight(vertices, current, previous, CAPE_WIDTH, bottom, 0.0f,
                    CAPE_WIDTH, top, -CAPE_DEPTH, part, light);
            emitBack(vertices, current, previous, CAPE_WIDTH, bottom, -CAPE_DEPTH,
                    -CAPE_WIDTH, top, -CAPE_DEPTH, part, light);
            emitFront(vertices, previous, current, CAPE_WIDTH, bottom, 0.0f,
                    -CAPE_WIDTH, top, 0.0f, part, light);

            previous = current;
            matrices.pop();
        }
    }

    @Unique
    private static void transformPart(MatrixStack matrices, PlayerEntityRenderState state,
                                      CapeSimulation simulation, float delta, int part) {
        matrices.translate(0.0f, 0.0f, state.equippedChestStack.isEmpty() ? 0.125f : 0.15f);
        CapeSimulation.Point base = simulation.points().getFirst();
        CapeSimulation.Point point = simulation.points().get(part);
        float x = point.lerpedX(delta) - base.lerpedX(delta);
        if (x > 0.0f) {
            x = 0.0f;
        }
        float y = base.lerpedY(delta) - part - point.lerpedY(delta);
        float z = base.lerpedZ(delta) - point.lerpedZ(delta);
        float crouch = 0.0f;
        if (state.sneaking) {
            crouch = 25.0f;
            matrices.translate(0.0f, 0.15f, 0.0f);
        }
        float wind = naturalWind(part, CapePhysicsManager.isUnderwater(state.id));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + crouch + wind));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        matrices.translate(-z / CapePhysicsManager.PART_COUNT,
                y / CapePhysicsManager.PART_COUNT,
                x / CapePhysicsManager.PART_COUNT);
        matrices.translate(0.0f, 0.03f, -0.03f);
        matrices.translate(0.0f, part / (float) CapePhysicsManager.PART_COUNT, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rotation(delta, part, simulation)));
        matrices.translate(0.0f, -part / (float) CapePhysicsManager.PART_COUNT, 0.0f);
        matrices.translate(0.0f, -0.03f, 0.03f);
    }

    @Unique
    private static float rotation(float delta, int part, CapeSimulation simulation) {
        if (part == CapePhysicsManager.PART_COUNT - 1) {
            return rotation(delta, part - 1, simulation);
        }
        CapeSimulation.Point first = simulation.points().get(part);
        CapeSimulation.Point second = simulation.points().get(part + 1);
        float angleX = second.lerpedX(delta) - first.lerpedX(delta);
        float angleY = second.lerpedY(delta) - first.lerpedY(delta);
        return (float) (Math.toDegrees(Math.atan2(angleX, angleY)) + 180.0);
    }

    @Unique
    private static float naturalWind(int part, boolean underwater) {
        long highlightedPart = System.currentTimeMillis() / (underwater ? 9L : 3L) % 360L;
        float relativePart = (part + 1) / (float) CapePhysicsManager.PART_COUNT;
        return (float) (Math.sin(Math.toRadians(relativePart * 360.0f - highlightedPart)) * 3.0);
    }

    @Unique
    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int light, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalX, normalY, normalZ);
    }

    @Unique
    private static float minV(int part) {
        return 0.03125f + 0.5f / CapePhysicsManager.PART_COUNT * part;
    }

    @Unique
    private static float maxV(int part) {
        return 0.03125f + 0.5f / CapePhysicsManager.PART_COUNT * (part + 1);
    }

    @Unique
    private static void emitBack(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 int part, int light) {
        vertex(out, oldMatrix, x1, y2, z1, 0.171875f, minV(part), light, 0, 0, -1);
        vertex(out, oldMatrix, x2, y2, z1, 0.015625f, minV(part), light, 0, 0, -1);
        vertex(out, matrix, x2, y1, z2, 0.015625f, maxV(part), light, 0, 0, -1);
        vertex(out, matrix, x1, y1, z2, 0.171875f, maxV(part), light, 0, 0, -1);
    }

    @Unique
    private static void emitFront(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  int part, int light) {
        vertex(out, oldMatrix, x1, y1, z1, 0.34375f, maxV(part), light, 0, 0, 1);
        vertex(out, oldMatrix, x2, y1, z1, 0.1875f, maxV(part), light, 0, 0, 1);
        vertex(out, matrix, x2, y2, z2, 0.1875f, minV(part), light, 0, 0, 1);
        vertex(out, matrix, x1, y2, z2, 0.34375f, minV(part), light, 0, 0, 1);
    }

    @Unique
    private static void emitLeft(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 int part, int light) {
        vertex(out, matrix, x2, y1, z1, 0.015625f, maxV(part), light, -1, 0, 0);
        vertex(out, matrix, x2, y1, z2, 0.0f, maxV(part), light, -1, 0, 0);
        vertex(out, oldMatrix, x2, y2, z2, 0.0f, minV(part), light, -1, 0, 0);
        vertex(out, oldMatrix, x2, y2, z1, 0.015625f, minV(part), light, -1, 0, 0);
    }

    @Unique
    private static void emitRight(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  int part, int light) {
        vertex(out, matrix, x2, y1, z2, 0.171875f, maxV(part), light, 1, 0, 0);
        vertex(out, matrix, x2, y1, z1, 0.1875f, maxV(part), light, 1, 0, 0);
        vertex(out, oldMatrix, x2, y2, z1, 0.1875f, minV(part), light, 1, 0, 0);
        vertex(out, oldMatrix, x2, y2, z2, 0.171875f, minV(part), light, 1, 0, 0);
    }

    @Unique
    private static void emitBottom(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   int part, int light) {
        vertex(out, oldMatrix, x1, y2, z2, 0.328125f, 0.0f, light, 0, 1, 0);
        vertex(out, oldMatrix, x2, y2, z2, 0.171875f, 0.0f, light, 0, 1, 0);
        vertex(out, matrix, x2, y1, z1, 0.171875f, 0.03125f, light, 0, 1, 0);
        vertex(out, matrix, x1, y1, z1, 0.328125f, 0.03125f, light, 0, 1, 0);
    }

    @Unique
    private static void emitTop(VertexConsumer out, Matrix4f matrix, Matrix4f oldMatrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                int light) {
        vertex(out, oldMatrix, x1, y2, z1, 0.171875f, 0.03125f, light, 0, -1, 0);
        vertex(out, oldMatrix, x2, y2, z1, 0.015625f, 0.03125f, light, 0, -1, 0);
        vertex(out, matrix, x2, y1, z2, 0.015625f, 0.0f, light, 0, -1, 0);
        vertex(out, matrix, x1, y1, z2, 0.171875f, 0.0f, light, 0, -1, 0);
    }
}
