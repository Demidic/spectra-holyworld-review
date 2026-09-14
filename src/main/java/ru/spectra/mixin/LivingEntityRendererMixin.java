package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.util.ColorUtil;
import ru.spectra.client.module.ChamsModule;
import ru.spectra.client.ui.TargetHudWidget;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> {

    @Unique
    BufferBuilder currentBuilder;

    @Unique
    private boolean spectra$previousDepthTest;

    @Unique
    private int spectra$previousDepthFunction;

    @Unique
    private int spectra$currentChamsColor;

    @Unique
    private boolean spectra$currentChamsAdditiveBlend;



    @Redirect(method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    public VertexConsumer redirectGetBuffer(VertexConsumerProvider vertexConsumerProvider, RenderLayer renderLayer) {
        if (TargetHudWidget.isRenderingModelPortrait()) {
            return vertexConsumerProvider.getBuffer(renderLayer);
        }
        ChamsModule class540Var = (ChamsModule) Spectra.INSTANCE.moduleRepository().get(ChamsModule.class);
        if (!class540Var.isState() || !class540Var.shouldRender(class540Var.currentEntity())) {
            return vertexConsumerProvider.getBuffer(renderLayer);
        }
        int color = class540Var.colorSetting().getColor();
        boolean zIsValue = class540Var.blendingSetting().isValue();
        this.spectra$currentChamsColor = color;
        this.spectra$currentChamsAdditiveBlend = zIsValue;
        RenderSystem.enableBlend();
        if (zIsValue) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        RenderSystem.setShaderColor(ColorUtil.red(color) / 255.0f, ColorUtil.green(color) / 255.0f, ColorUtil.blue(color) / 255.0f, ColorUtil.alpha(color) / 255.0f);
        BufferBuilder bufferBuilderBegin = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        this.currentBuilder = bufferBuilderBegin;
        this.spectra$previousDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.spectra$previousDepthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        return bufferBuilderBegin;
    }

    @Inject(method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V", shift = At.Shift.AFTER)})
    private void postRender(S s, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo callbackInfo) {
        BufferBuilder builder = this.currentBuilder;
        if (builder != null) {
            // Clear the renderer field before drawing because inventory
            // previews may perform nested entity renders from the same thread.
            this.currentBuilder = null;
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            try {
                // Some GUI/compatibility models intentionally emit no geometry.
                // end() throws for those; endNullable() keeps the render pass valid.
                var builtBuffer = builder.endNullable();
                if (builtBuffer != null) {
                    ChamsModule chams = Spectra.INSTANCE.moduleRepository().get(ChamsModule.class);
                    if (chams.shouldDeferColorForBlurredFog()) {
                        chams.drawDepthAndDeferColor(
                                builtBuffer,
                                this.spectra$currentChamsColor,
                                this.spectra$currentChamsAdditiveBlend
                        );
                    } else {
                        BufferRenderer.drawWithGlobalProgram(builtBuffer);
                    }
                }
            } finally {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
                RenderSystem.depthFunc(this.spectra$previousDepthFunction);
                if (this.spectra$previousDepthTest) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
            }
        }
    }
}
