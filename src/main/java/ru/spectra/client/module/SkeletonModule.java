package ru.spectra.client.module;

import ru.spectra.client.Lang;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.PlayerVisualFilter;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

/**
 * The model-part skeleton which previously existed in this 1.21.4 client.
 * It follows Minecraft's actual animated player model instead of estimating
 * limb positions. Players behind blocks are rejected before drawing.
 */
@Aliases(aliases = {"Skeleton", "Skelet ESP", "Skeleton ESP", "ESP"})
public final class SkeletonModule extends Module {
    public final ColorSetting colorSetting;
    public final BooleanSetting blendingSetting;
    private final Quaternionf quaternion = new Quaternionf();

    public SkeletonModule() {
        super(ModuleTab.RENDER, "Skeleton");
        this.colorSetting = new ColorSetting(Lang.SKELETON_COLOR);
        this.blendingSetting =
                new BooleanSetting(Lang.SKELETON_BLENDING).setValue(true);
        addSettings(this.colorSetting, this.blendingSetting);
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!Mc.INSTANCE.isWorldLoaded() || !isState()) {
            return;
        }

        AbstractClientPlayerEntity localPlayer = Mc.INSTANCE.getPlayer();
        MatrixStack matrices = event.matrixStack();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        float tickDelta = Mc.INSTANCE.getTickDelta();
        int color = this.colorSetting.getColor();
        try {
            RenderSystem.enableBlend();
            if (this.blendingSetting.isValue()) {
                RenderSystem.blendFunc(
                        GlStateManager.SrcFactor.SRC_ALPHA,
                        GlStateManager.DstFactor.ONE
                );
            } else {
                RenderSystem.defaultBlendFunc();
            }
            RenderSystem.disableCull();
            /*
             * The skeleton must be visible over the player's own skin. Full
             * wall visibility is prevented by canSee() below, rather than by
             * letting the model's polygons hide its internal bones.
             */
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(2.0f);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            for (AbstractClientPlayerEntity player : Mc.INSTANCE.getWorld().getPlayers()) {
                if (!shouldRender(player, localPlayer, event)) {
                    continue;
                }
                renderPlayer(player, localPlayer, matrices, tessellator, tickDelta, color);
            }
        } finally {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.lineWidth(1.0f);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private boolean shouldRender(AbstractClientPlayerEntity player,
                                 AbstractClientPlayerEntity localPlayer,
                                 WorldRenderEvent event) {
        if (player == null
                || !player.isAlive()
                || player.isSpectator()
                || !PlayerVisualFilter.shouldRender(player)
                || !event.frustum().isVisible(player.getBoundingBox())
                || !localPlayer.canSee(player)) {
            return false;
        }
        return Mc.INSTANCE.getGameOptions().getPerspective() != Perspective.FIRST_PERSON
                || player != localPlayer;
    }

    private void renderPlayer(AbstractClientPlayerEntity player,
                              AbstractClientPlayerEntity localPlayer,
                              MatrixStack matrices,
                              Tessellator tessellator,
                              float tickDelta,
                              int color) {
        matrices.push();
        try {
            Vec3d renderPosition = MathUtil.getEntityRenderPosition(player, tickDelta);
            PlayerEntityRenderer renderer =
                    (PlayerEntityRenderer) Mc.INSTANCE.getEntityRenderDispatcher().getRenderer(player);
            PlayerEntityRenderState renderState = renderer.createRenderState();
            renderer.updateRenderState(player, renderState, tickDelta);
            PlayerEntityModel model = renderer.getModel();
            model.setAngles(renderState);

            float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
            float pitch = player.getPitch(tickDelta);
            boolean swimming = player.isInSwimmingPose();
            boolean gliding = player.isGliding();
            boolean sneaking = player.isSneaking() && !player.getAbilities().flying;

            matrices.translate(renderPosition.x, renderPosition.y, renderPosition.z);
            if (swimming) {
                matrices.translate(0.0f, 0.35f, 0.0f);
            }
            matrices.multiply(this.quaternion.setAngleAxis(
                    Math.toRadians(bodyYaw + 180.0f), 0.0d, -1.0d, 0.0d));
            if (swimming || gliding) {
                matrices.multiply(this.quaternion.setAngleAxis(
                        Math.toRadians(90.0f + pitch), -1.0d, 0.0d, 0.0d));
            }
            if (swimming) {
                matrices.translate(0.0f, -0.95f, 0.0f);
            }

            BufferBuilder builder =
                    tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            Matrix4f base = matrices.peek().getPositionMatrix();
            drawBoneLine(builder, base,
                    0.0f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0.0f,
                    0.0f, sneaking ? 1.05f : 1.4f, 0.0f, color);
            drawBoneLine(builder, base,
                    -0.37f, sneaking ? 1.05f : 1.35f, 0.0f,
                    0.37f, sneaking ? 1.05f : 1.35f, 0.0f, color);
            drawBoneLine(builder, base,
                    -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0.0f,
                    0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0.0f,
                    color);

            drawPart(builder, matrices, model.head,
                    0.0f, sneaking ? 1.05f : 1.4f, 0.0f,
                    0.0f, 0.15f, 0.0f, color);
            drawPart(builder, matrices, model.rightLeg,
                    0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0.0f,
                    0.0f, -0.6f, 0.0f, color);
            drawPart(builder, matrices, model.leftLeg,
                    -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0.0f,
                    0.0f, -0.6f, 0.0f, color);
            drawPart(builder, matrices, model.rightArm,
                    0.37f, sneaking ? 1.05f : 1.35f, 0.0f,
                    0.0f, -0.55f, 0.0f, color);
            drawPart(builder, matrices, model.leftArm,
                    -0.37f, sneaking ? 1.05f : 1.35f, 0.0f,
                    0.0f, -0.55f, 0.0f, color);
            BufferRenderer.drawWithGlobalProgram(builder.end());
        } finally {
            matrices.pop();
        }
    }

    private void drawPart(BufferBuilder builder, MatrixStack matrices, ModelPart part,
                          float originX, float originY, float originZ,
                          float endX, float endY, float endZ, int color) {
        matrices.push();
        matrices.translate(originX, originY, originZ);
        applyPartRotation(matrices, part);
        drawBoneLine(builder, matrices.peek().getPositionMatrix(),
                0.0f, 0.0f, 0.0f, endX, endY, endZ, color);
        matrices.pop();
    }

    public void drawBoneLine(BufferBuilder builder, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = length == 0.0f ? 0.0f : dx / length;
        float ny = length == 0.0f ? 0.0f : dy / length;
        float nz = length == 0.0f ? 0.0f : dz / length;
        builder.vertex(matrix, x1, y1, z1).color(color).normal(nx, ny, nz);
        builder.vertex(matrix, x2, y2, z2).color(color).normal(nx, ny, nz);
    }

    public void applyPartRotation(MatrixStack matrices, ModelPart part) {
        if (part.roll != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation(part.roll));
        }
        if (part.yaw != 0.0f) {
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(part.yaw));
        }
        if (part.pitch != 0.0f) {
            matrices.multiply(RotationAxis.NEGATIVE_X.rotation(part.pitch));
        }
    }
}
