package ru.spectra.client.module;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.TargetEspMode;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.ColorUtil;
import ru.spectra.client.util.PlayerVisualFilter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A visual target marker. It deliberately has no combat-module dependency:
 * the target is selected only for rendering, exactly like the old Spectra implementation.
 */
public final class TargetESPModule extends Module {
    private static final float FIXED_OPACITY = 255.0f;
    private static final float FIXED_FADE_SPEED = 0.5f;
    private static final float CRYSTAL_PULSE_SPEED = 0.5f;
    private static final float CRYSTAL_GLOW_STRENGTH = 1.0f;
    private static final float CROSS_PULSE_SPEED = 0.7f;
    private static final int CROSS_COUNT = 6;
    private static final float CROSS_SIZE = 0.3f;
    private static final float CROSS_THICKNESS = 0.1f;
    private static final float CROSS_DISTANCE = 0.8f;
    private static final float CROSS_HEIGHT = 0.0f;
    private static final float CROSS_ROTATION_SPEED = 120.0f;
    private static final GlTexture TARGET_ESP_BLOOM_TEXTURE =
            new GlTexture(new ClasspathResource("/textures/target_esp_bloom.png"));
    private static final Vec3d[] CRYSTAL_VERTICES = {
            new Vec3d(0.0, 1.5, 0.0), new Vec3d(0.0, -1.5, 0.0),
            new Vec3d(1.0, 0.0, 0.0), new Vec3d(-1.0, 0.0, 0.0),
            new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 0.0, -1.0)
    };
    private static final int[][] CRYSTAL_FACES = {
            {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
            {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}
    };
    private static final float[] CRYSTAL_BRIGHTNESS = {1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f};

    public final ModeSetting<TargetEspMode> mode = new ModeSetting<TargetEspMode>(Lang.TARGETESP_MODE)
            .values(TargetEspMode.class);
    public final BooleanSetting clientColor = new BooleanSetting(Translation.clearText("Client color")).setValue(false);
    public final ColorSetting color = new ColorSetting(Lang.TARGETESP_COLOR);
    public final NumberSetting opacity = number("Opacity", 180.0f, 50.0f, 255.0f, 5.0f)
            .visible(() -> !mode.isSelected(TargetEspMode.CRYSTALS));
    public final NumberSetting fadeSpeed = number("Fade speed", 0.5f, 0.1f, 2.0f, 0.1f)
            .visible(() -> !usesFixedPreset());
    public final NumberSetting pulseSpeed = number("Pulse speed", 1.0f, 0.1f, 3.0f, 0.1f)
            .visible(() -> !usesFixedPreset());
    public final NumberSetting soulCount = number("Soul count", 3.0f, 1.0f, 8.0f, 1.0f)
            .visible(() -> mode.isSelected(TargetEspMode.SOULS));
    public final NumberSetting soulThickness = number("Soul thickness", 0.4f, 0.1f, 2.0f, 0.05f)
            .visible(() -> mode.isSelected(TargetEspMode.SOULS));
    public final NumberSetting soulLength = number("Soul length", 20.0f, 5.0f, 100.0f, 1.0f)
            .visible(() -> mode.isSelected(TargetEspMode.SOULS));
    public final NumberSetting soulRotationSpeed = number("Soul rotation speed", 45.0f, 1.0f, 200.0f, 1.0f)
            .visible(() -> mode.isSelected(TargetEspMode.SOULS));
    public final NumberSetting crossCount = number("Cross count", CROSS_COUNT, 1.0f, 6.0f, 1.0f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting crossSize = number("Cross size", 0.3f, 0.15f, 0.6f, 0.05f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting crossThickness = number("Cross thickness", 0.1f, 0.05f, 0.2f, 0.01f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting crossDistance = number("Cross distance", CROSS_DISTANCE, 0.5f, 2.0f, 0.1f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting crossHeight = number("Cross height", 0.0f, -1.0f, 1.0f, 0.1f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting crossRotationSpeed = number("Cross rotation speed", CROSS_ROTATION_SPEED, 5.0f, 120.0f, 5.0f)
            .visible(() -> mode.isSelected(TargetEspMode.CROSSES));
    public final NumberSetting glowStrength = number("Glow strength", 1.0f, 0.0f, 2.0f, 0.1f)
            .visible(() -> !mode.isSelected(TargetEspMode.CRYSTALS));

    private LivingEntity selectedTarget;
    private LivingEntity renderedTarget;
    private float visibility;
    private float pulsePhase;
    private float soulAngle;
    private float crossAngle;
    private float crystalAngle;
    private long lastFrameNanos;

    public TargetESPModule() {
        super(ModuleTab.RENDER, "Target ESP");
        color.setColor(0xFF8C7CFF);
        addSettings(mode, clientColor, color, opacity, fadeSpeed, pulseSpeed,
                soulCount, soulThickness, soulLength, soulRotationSpeed,
                glowStrength);

        register(PlayerTickEvent.class, event -> {
            if (!event.isPre()) {
                return;
            }
            selectedTarget = isState() ? findVisualTarget() : null;
            if (selectedTarget != null) {
                renderedTarget = selectedTarget;
            }
        });

        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            resetVisualState();
            return;
        }
        updateAnimation();
        if (renderedTarget == null || visibility <= 0.01f || !renderedTarget.isAlive()) {
            return;
        }
        if (!PlayerVisualFilter.shouldRender(renderedTarget)) {
            resetVisualState();
            return;
        }
        float tickDelta = Mc.INSTANCE.getTickDelta();
        Vec3d targetPos = renderedTarget.getLerpedPos(tickDelta);
        Camera camera = Mc.INSTANCE.getCamera();
        Vec3d relative = targetPos.subtract(camera.getPos());
        int tint = animatedColor();
        MatrixStack matrices = event.matrixStack();
        switch (mode.currentValue()) {
            case SOULS -> renderSouls(matrices, camera, relative, tint);
            case CROSSES -> renderCrosses(matrices, relative, tint);
            case CRYSTALS -> renderCrystals(matrices, camera, relative, tint);
        }
    }

    private void updateAnimation() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 1.0f / 60.0f
                : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        float targetVisibility = isState() && selectedTarget != null ? 1.0f : 0.0f;
        float rate = effectiveFadeSpeed() * 3.0f * dt;
        visibility = MathHelper.clamp(visibility + Math.copySign(Math.min(Math.abs(targetVisibility - visibility), rate),
                targetVisibility - visibility), 0.0f, 1.0f);
        if (visibility <= 0.01f && targetVisibility == 0.0f) {
            renderedTarget = null;
        }
        pulsePhase = wrapDegrees(pulsePhase + effectivePulseSpeed() * 90.0f * dt);
        soulAngle = wrapDegrees(soulAngle + soulRotationSpeed.currentValue() * dt);
        crossAngle = wrapDegrees(crossAngle + CROSS_ROTATION_SPEED * dt);
        crystalAngle = wrapDegrees(crystalAngle + 60.0f * dt);
    }

    private void renderSouls(MatrixStack matrices, Camera camera, Vec3d relative, int tint) {
        float width = renderedTarget.getWidth() * 1.5f;
        int soulAlpha = MathHelper.clamp(ColorUtil.alpha(tint), 0, 255);
        int glowAlpha = MathHelper.clamp(Math.round(soulAlpha * 0.05f), 0, 255);
        int soulColor = ColorUtil.replAlpha(tint, soulAlpha);
        int glowColor = ColorUtil.replAlpha(tint, glowAlpha);
        int oldTexture = RenderSystem.getShaderTexture(0);
        boolean depthTest = isTargetVisibleThroughWorld(camera);

        matrices.push();
        try {
            matrices.translate(relative.x, relative.y, relative.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE,
                    GlStateManager.SrcFactor.ZERO,
                    GlStateManager.DstFactor.ONE
            );
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.setShaderTexture(0, TARGET_ESP_BLOOM_TEXTURE.textureWithSTB());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder builder = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int step = 2;
            int wormTick = 0;
            int wormCooldown = 0;
            for (int degrees = 0; degrees < 360; degrees += step) {
                float size = 0.13f + 0.005f * wormTick;
                float bigSize = 0.7f + 0.005f * wormTick;
                if (wormCooldown > 0) {
                    wormCooldown -= step;
                    continue;
                }
                wormTick += step;
                if (wormTick > 50) {
                    wormCooldown = 100;
                    wormTick = 0;
                    continue;
                }
                float radiusScale = Math.max(0.5f, 1.0f - 0.2f * visibility);
                float angle = (float) Math.toRadians(degrees + soulAngle);
                float x = MathHelper.sin(angle) * width * radiusScale;
                float z = MathHelper.cos(angle) * width * radiusScale;
                float y = renderedTarget.getHeight() * 0.58f
                        + renderedTarget.getHeight() * 0.16f
                        * MathHelper.sin((float) Math.toRadians(degrees / 2.0f + soulAngle / 5.0f));
                matrices.push();
                matrices.translate(x, y, z);
                matrices.multiply(camera.getRotation());
                emitBillboardQuad(matrices.peek().getPositionMatrix(), builder, bigSize / 2.0f, glowColor);
                emitBillboardQuad(matrices.peek().getPositionMatrix(), builder, size / 2.0f, soulColor);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(builder.end());
        } finally {
            matrices.pop();
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderCrosses(MatrixStack matrices, Vec3d relative, int tint) {
        int count = CROSS_COUNT;
        float radius = CROSS_DISTANCE;
        float y = renderedTarget.getHeight() * 0.5f + CROSS_HEIGHT;
        for (int i = 0; i < count; i++) {
            float angle = crossAngle + 360.0f * i / count;
            float radians = (float) Math.toRadians(angle);
            float x = MathHelper.cos(radians) * radius;
            float z = MathHelper.sin(radians) * radius;
            matrices.push();
            matrices.translate(relative.x + x, relative.y + y, relative.z + z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-angle + 90.0f));
            addCross(matrices.peek().getPositionMatrix(), CROSS_SIZE, CROSS_THICKNESS,
                    tint, effectiveGlowStrength());
            matrices.pop();
        }
    }

    private void addCross(Matrix4f matrix, float size, float thickness, int color, float glowStrength) {
        float s = size * 1.3f;
        float t = thickness / 2.0f;
        Box vertical = new Box(-t, -s, -t, t, s, t);
        float barY = s * 0.35f;
        Box horizontal = new Box(-size * 0.8f, barY - t, -t, size * 0.8f, barY + t, t);
        ShapeRenderer.INSTANCE.addBox(matrix, vertical, color, false);
        ShapeRenderer.INSTANCE.addBox(matrix, horizontal, color, false);
        addCrossGlow(matrix, vertical, color, glowStrength);
        addCrossGlow(matrix, horizontal, color, glowStrength);
        ShapeRenderer.INSTANCE.addOutline(matrix, vertical, color, 1.5f, false);
        ShapeRenderer.INSTANCE.addOutline(matrix, horizontal, color, 1.5f, false);
    }

    private void addCrossGlow(Matrix4f matrix, Box box, int color, float strength) {
        int alpha = ColorUtil.alpha(color);
        if (alpha <= 0 || strength <= 0.01f) {
            return;
        }
        addCrossGlowPass(matrix, box, color, alpha, strength, 7.0f, 0.08f);
        addCrossGlowPass(matrix, box, color, alpha, strength, 4.5f, 0.14f);
        addCrossGlowPass(matrix, box, color, alpha, strength, 2.75f, 0.22f);
    }

    private void addCrossGlowPass(Matrix4f matrix, Box box, int color, int alpha,
                                  float strength, float lineWidth, float alphaScale) {
        int glowAlpha = MathHelper.clamp(Math.round(alpha * alphaScale * strength), 0, 255);
        if (glowAlpha > 0) {
            ShapeRenderer.INSTANCE.addAdditiveOutline(matrix, box,
                    ColorUtil.replAlpha(color, glowAlpha), lineWidth, true, false);
        }
    }

    private void renderCrystals(MatrixStack matrices, Camera camera, Vec3d relative, int tint) {
        float width = renderedTarget.getWidth() * 1.5f;
        float radiusScale = 1.2f - 0.5f * visibility;
        boolean depthTest = isTargetVisibleThroughWorld(camera);
        int crystalAlpha = MathHelper.clamp(ColorUtil.alpha(tint), 0, 255);
        int crystalColor = ColorUtil.replAlpha(tint, crystalAlpha);
        float strength = effectiveGlowStrength();
        int bloomColor = ColorUtil.replAlpha(tint,
                MathHelper.clamp(Math.round(crystalAlpha * 0.22f * strength), 0, 255));
        float bloomSize = 0.5f;
        int oldTexture = RenderSystem.getShaderTexture(0);

        matrices.push();
        try {
            matrices.translate(relative.x, relative.y, relative.z);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE,
                    GlStateManager.SrcFactor.ZERO,
                    GlStateManager.DstFactor.ONE
            );
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder crystalBuilder = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int degrees = 0; degrees < 360; degrees += 20) {
                float angle = (float) Math.toRadians(degrees + crystalAngle);
                float x = MathHelper.sin(angle) * width * radiusScale;
                float z = MathHelper.cos(angle) * width * radiusScale;
                float height = 0.1f + renderedTarget.getHeight() * Math.abs((float) Math.sin(degrees));
                Vec3d directionToTarget =
                        new Vec3d(-x, renderedTarget.getHeight() * 0.5f - height, -z);
                matrices.push();
                matrices.translate(x, height, z);
                matrices.multiply(crystalRotation(directionToTarget));
                emitCrystal(matrices, crystalBuilder, 0.1f, crystalColor);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(crystalBuilder.end());

            RenderSystem.setShaderTexture(0, TARGET_ESP_BLOOM_TEXTURE.textureWithSTB());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bloomBuilder = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int degrees = 0; degrees < 360; degrees += 20) {
                float angle = (float) Math.toRadians(degrees + crystalAngle);
                float x = MathHelper.sin(angle) * width * radiusScale;
                float z = MathHelper.cos(angle) * width * radiusScale;
                float height = 0.1f + renderedTarget.getHeight() * Math.abs((float) Math.sin(degrees));
                matrices.push();
                matrices.translate(x, height, z);
                matrices.multiply(camera.getRotation());
                emitBillboardQuad(matrices.peek().getPositionMatrix(), bloomBuilder,
                        bloomSize, bloomColor);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(bloomBuilder.end());
        } finally {
            matrices.pop();
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private boolean isTargetVisibleThroughWorld(Camera camera) {
        return Mc.INSTANCE.getWorld().raycast(new RaycastContext(
                camera.getPos(), renderedTarget.getEyePos(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, Mc.INSTANCE.getPlayer()
        )).getType() == HitResult.Type.MISS;
    }

    private void emitBillboardQuad(Matrix4f matrix, BufferBuilder builder, float size, int color) {
        builder.vertex(matrix, -size, -size, 0.0f).texture(0.0f, 0.0f).color(color);
        builder.vertex(matrix, -size, size, 0.0f).texture(0.0f, 1.0f).color(color);
        builder.vertex(matrix, size, size, 0.0f).texture(1.0f, 1.0f).color(color);
        builder.vertex(matrix, size, -size, 0.0f).texture(1.0f, 0.0f).color(color);
    }

    private Quaternionf crystalRotation(Vec3d direction) {
        Vector3f targetDirection = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        if (targetDirection.lengthSquared() < 1.0E-6f) {
            return new Quaternionf();
        }
        targetDirection.normalize();
        Vector3f initialDirection = new Vector3f(0.0f, 1.0f, 0.0f);
        float dot = MathHelper.clamp(initialDirection.dot(targetDirection), -1.0f, 1.0f);
        if (dot > 0.9999f) {
            return new Quaternionf();
        }
        if (dot < -0.9999f) {
            return RotationAxis.POSITIVE_X.rotationDegrees(180.0f);
        }
        Vector3f axis = initialDirection.cross(targetDirection, new Vector3f()).normalize();
        return new Quaternionf().fromAxisAngleRad(axis, (float) Math.acos(dot));
    }

    private void emitCrystal(MatrixStack matrices, BufferBuilder builder, float size, int color) {
        matrices.push();
        matrices.scale(size, size, size);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < CRYSTAL_FACES.length; i++) {
            int[] face = CRYSTAL_FACES[i];
            int shaded = multiplyBrightness(color, CRYSTAL_BRIGHTNESS[i]);
            Vec3d first = CRYSTAL_VERTICES[face[0]];
            Vec3d second = CRYSTAL_VERTICES[face[1]];
            Vec3d third = CRYSTAL_VERTICES[face[2]];
            builder.vertex(matrix, (float) first.x, (float) first.y, (float) first.z).color(shaded);
            builder.vertex(matrix, (float) second.x, (float) second.y, (float) second.z).color(shaded);
            builder.vertex(matrix, (float) third.x, (float) third.y, (float) third.z).color(shaded);
        }
        matrices.pop();
    }

    private int animatedColor() {
        int base = clientColor.isValue() ? Spectra.INSTANCE.theme().palette().accent().argb() : color.getColor();
        int alpha = MathHelper.clamp(Math.round(effectiveOpacity() * visibility), 0, 255);
        int tinted = ColorUtil.replAlpha(base, alpha);
        return tinted;
    }

    private boolean usesFixedPreset() {
        return mode.isSelected(TargetEspMode.CROSSES)
                || mode.isSelected(TargetEspMode.CRYSTALS);
    }

    private float effectiveOpacity() {
        return mode.isSelected(TargetEspMode.CRYSTALS) ? FIXED_OPACITY : opacity.currentValue();
    }

    private float effectiveFadeSpeed() {
        return usesFixedPreset() ? FIXED_FADE_SPEED : fadeSpeed.currentValue();
    }

    private float effectivePulseSpeed() {
        if (mode.isSelected(TargetEspMode.CROSSES)) return CROSS_PULSE_SPEED;
        if (mode.isSelected(TargetEspMode.CRYSTALS)) return CRYSTAL_PULSE_SPEED;
        return pulseSpeed.currentValue();
    }

    private float effectiveGlowStrength() {
        if (mode.isSelected(TargetEspMode.CRYSTALS)) return CRYSTAL_GLOW_STRENGTH;
        return glowStrength.currentValue();
    }

    private LivingEntity findVisualTarget() {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            return null;
        }
        PlayerEntity player = Mc.INSTANCE.getPlayer();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d eye = player.getEyePos();
        LivingEntity aimed = null;
        LivingEntity closest = null;
        double bestDot = -1.0;
        double closestDistance = Double.MAX_VALUE;
        double angleThreshold = Math.cos(Math.toRadians(30.0));
        for (PlayerEntity candidate : Mc.INSTANCE.getWorld().getPlayers()) {
            if (candidate == player || !candidate.isAlive()
                    || !PlayerVisualFilter.shouldRender(candidate)) {
                continue;
            }
            double distance = player.squaredDistanceTo(candidate);
            if (distance > 25.0) {
                continue;
            }
            Vec3d direction = candidate.getPos().add(0.0, candidate.getHeight() * 0.5, 0.0).subtract(eye);
            if (direction.lengthSquared() > 1.0E-6) {
                double dot = look.dotProduct(direction.normalize());
                if (dot >= angleThreshold && dot > bestDot) {
                    bestDot = dot;
                    aimed = candidate;
                }
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return aimed != null ? aimed : closest;
    }

    private void resetVisualState() {
        selectedTarget = null;
        renderedTarget = null;
        visibility = 0.0f;
        lastFrameNanos = 0L;
    }

    private static NumberSetting number(String name, float value, float min, float max, float step) {
        return new NumberSetting(Translation.clearText(name)).range(min, max).currentValue(value).step(step);
    }

    private static float wrapDegrees(float value) {
        return value >= 360.0f ? value - 360.0f : value;
    }

    private static int multiplyBrightness(int color, float brightness) {
        return ColorUtil.argb(ColorUtil.alpha(color),
                MathHelper.clamp(Math.round(ColorUtil.red(color) * brightness), 0, 255),
                MathHelper.clamp(Math.round(ColorUtil.green(color) * brightness), 0, 255),
                MathHelper.clamp(Math.round(ColorUtil.blue(color) * brightness), 0, 255));
    }
}
