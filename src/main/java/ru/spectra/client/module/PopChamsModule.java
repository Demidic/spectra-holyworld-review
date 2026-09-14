package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.render.AnimationStack2;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.util.ColorUtil;
import ru.spectra.client.math.DeltaTimeTracker;
import ru.spectra.client.Lang;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.model.PopChamsFakePlayer;
import ru.spectra.client.model.PopChamsModel;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.event.WorldRenderEvent;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Aliases(aliases = {"Pop Chams", "Totem Pop", "Chams"})
public class PopChamsModule extends Module {
    public final List<PopChamsModel> models;
    public final BooleanSetting blending;
    public final BooleanSetting textured;
    public final ColorSetting color;
    public final DeltaTimeTracker deltaTracker;
    public final AnimationStack2 animationStack;

    public PopChamsModule() {
        super(ModuleTab.RENDER, "Pop Chams");
        this.models = new ArrayList();
        this.blending = new BooleanSetting(Lang.POPCHAMS_BLENDING).setValue(true);
        this.textured = new BooleanSetting(Lang.POPCHAMS_TEXTURED).setValue(true);
        this.color = new ColorSetting(Lang.POPCHAMS_COLOR);
        this.deltaTracker = new DeltaTimeTracker();
        this.animationStack = new AnimationStack2();
        addSettings(this.blending, this.color);
        register(PlayerTickEvent.class, class130Var -> {
            if (Mc.INSTANCE.isWorldLoaded() && class130Var.isPre()) {
                this.models.removeIf((v0) -> {
                    return v0.tick();
                });
            }
        });
        register(WorldRenderEvent.class, class016Var -> {
            if (Mc.INSTANCE.isWorldLoaded() && !this.models.isEmpty()) {
                MatrixStack matrixStack = class016Var.matrixStack();
                WeightedEngine class141Var = new WeightedEngine(this.deltaTracker.elapsedUnit(), this.animationStack);
                this.animationStack.begin();
                this.models.forEach(class563Var -> {
                    class563Var.animate(class141Var);
                });
                this.animationStack.end();
                RenderSystem.enableBlend();
                if (this.blending.isValue()) {
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                } else {
                    RenderSystem.defaultBlendFunc();
                }
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(MinecraftClient.isFancyGraphicsOrBetter());
                RenderSystem.enableCull();
                this.models.forEach(class563Var2 -> {
                    renderModel(matrixStack, class563Var2);
                });
                RenderSystem.disableCull();
                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }
        });
        register(PacketReceiveEvent.class, class051Var -> {
            ClientWorld world;
            Entity entity;
            if (isState()) {
                if ((class051Var.getPacket()) instanceof EntityStatusS2CPacket packet ) {
                    EntityStatusS2CPacket entityStatusS2CPacket = packet;
                    if (entityStatusS2CPacket.getStatus() != 35 || (world = Mc.INSTANCE.getWorld()) == null || (entity = entityStatusS2CPacket.getEntity(world)) == null || !(entity instanceof OtherClientPlayerEntity)) {
                        return;
                    }
                    this.models.add(createModel((OtherClientPlayerEntity) entity, this.color.getColor(), true));
                }
            }
        });
    }

    public PopChamsModel createModel(OtherClientPlayerEntity otherClientPlayerEntity, int i, boolean z) {
        Mc class815Var = Mc.INSTANCE;
        PopChamsFakePlayer class562Var = new PopChamsFakePlayer(this, class815Var.getWorld(), BlockPos.ORIGIN, otherClientPlayerEntity.bodyYaw, new GameProfile(otherClientPlayerEntity.getUuid(), otherClientPlayerEntity.getName().getString()));
        class562Var.copyPositionAndRotation(otherClientPlayerEntity);
        ((PlayerEntity) class562Var).bodyYaw = otherClientPlayerEntity.bodyYaw;
        ((PlayerEntity) class562Var).headYaw = otherClientPlayerEntity.headYaw;
        ((PlayerEntity) class562Var).handSwingProgress = otherClientPlayerEntity.handSwingProgress;
        ((PlayerEntity) class562Var).handSwingTicks = otherClientPlayerEntity.handSwingTicks;
        class562Var.setSneaking(otherClientPlayerEntity.isSneaking());
        ((PlayerEntity) class562Var).limbAnimator.setSpeed(otherClientPlayerEntity.limbAnimator.getSpeed());
        ((PlayerEntity) class562Var).limbAnimator.pos = otherClientPlayerEntity.limbAnimator.getPos();
        ((PlayerEntity) class562Var).limbAnimator.prevSpeed = otherClientPlayerEntity.limbAnimator.prevSpeed;
        return new PopChamsModel(class562Var, otherClientPlayerEntity.getSkinTextures().texture(), i, z);
    }

    public void renderModel(MatrixStack matrixStack, PopChamsModel class563Var) {
        renderModel(matrixStack, class563Var,
                class563Var.getAnimation().smoothAnimation(), 0.0, class563Var.getColor());
    }

    public void renderModel(MatrixStack matrixStack, PopChamsModel class563Var,
                            float opacity, double yOffset, int tint) {
        BufferBuilder bufferBuilderBegin;
        Mc class815Var = Mc.INSTANCE;
        Vec3d pos = class815Var.getEntityRenderDispatcher().camera.getPos();
        Tessellator tesselator = class815Var.getTesselator();
        PlayerEntityModel model = class563Var.getModel();
        PlayerEntity player = class563Var.getPlayer();
        Identifier texture = class563Var.getTexture();
        int iApplyOpacity = ColorUtil.replAlpha(tint,
                Math.round(ColorUtil.alpha(tint) * MathUtil.clamp(opacity, 0.0f, 1.0f)));
        double x = player.getX() - pos.getX();
        double y = player.getY() - pos.getY();
        double z = player.getZ() - pos.getZ();
        class815Var.getTickDelta();
        float pos2 = player.limbAnimator.getPos();
        float speed = player.limbAnimator.getSpeed();
        PlayerEntityRenderState playerEntityRenderState = new PlayerEntityRenderState();
        playerEntityRenderState.bodyYaw = player.getBodyYaw();
        playerEntityRenderState.yawDegrees = player.headYaw - player.bodyYaw;
        playerEntityRenderState.pitch = player.getPitch();
        playerEntityRenderState.deathTime = player.deathTime;
        playerEntityRenderState.limbFrequency = pos2;
        playerEntityRenderState.limbAmplitudeMultiplier = speed;
        model.setAngles(playerEntityRenderState);
        if (!class563Var.isTextured() || texture == null) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION);
            bufferBuilderBegin = tesselator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        } else {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            bufferBuilderBegin = tesselator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        }
        RenderSystem.setShaderColor(ColorUtil.red(iApplyOpacity) / 255.0f, ColorUtil.green(iApplyOpacity) / 255.0f, ColorUtil.blue(iApplyOpacity) / 255.0f, ColorUtil.alpha(iApplyOpacity) / 255.0f);
        matrixStack.push();
        matrixStack.translate((float) x, (float) (y + yOffset), (float) z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtil.rad(180.0f - player.bodyYaw)));
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        model.render(matrixStack, bufferBuilderBegin, 10, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilderBegin.end());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrixStack.pop();
    }
}
