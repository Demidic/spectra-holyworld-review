package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.internal.AxisSwitchMap;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.render.ClientParticle;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.event.WorldRenderEvent;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Aliases(aliases = {"Particles", "Stars"})
public class ParticlesModule extends Module {
    public final BooleanSetting worldParticles;
    public final BooleanSetting hitParticles;
    public final NumberSetting spawnRate;
    public final NumberSetting radius;
    public final NumberSetting hitAmount;
    public final NumberSetting lifetime;
    public final NumberSetting size;

    public final NumberSetting maxParticles;
    public final ModeSetting<ParticleTexture> worldTexture;
    public final ModeSetting<ParticleTexture> hitTexture;
    public final ColorSetting worldColor;
    public final ColorSetting hitColor;
    public final List<ClientParticle> particles;
    public final Quaternionf cameraRotation;
    public long startTime;

    public final Vector3f relativePos;
    public final Vector3f rightVector;
    public final Vector3f upVector;
    public final Vector3f cornerTopLeft;

    public final Vector3f cornerBottomLeft;
    public final Vector3f cornerBottomRight;
    public final Vector3f cornerTopRight;
    public static final double bounceFactor = 0.55d;
    public static final double groundFriction = 0.75d;
    public static final double wallFriction = 0.9d;
    public static final double epsilon = 1.0E-4d;
    public float tickDelta;

    public ParticlesModule() {
        super(ModuleTab.RENDER, "Particles");
        this.worldParticles = new BooleanSetting(Lang.PARTICLES_WORLD);
        this.hitParticles = new BooleanSetting(Lang.PARTICLES_HIT);
        NumberSetting class613VarUnit = new NumberSetting(Lang.PARTICLES_SPAWN_RATE).range(0.0f, 5.0f).currentValue(1.0f).step(1.0f).unit(SettingUnit.TICKS);
        BooleanSetting class665Var = this.worldParticles;
        Objects.requireNonNull(class665Var);
        this.spawnRate = class613VarUnit.visible(class665Var::isValue);
        NumberSetting class613VarUnit2 = new NumberSetting(Lang.PARTICLES_RADIUS).range(10.0f, 40.0f).currentValue(40.0f).step(1.0f).unit(SettingUnit.BLOCKS);
        BooleanSetting class665Var2 = this.worldParticles;
        Objects.requireNonNull(class665Var2);
        this.radius = class613VarUnit2.visible(class665Var2::isValue);
        NumberSetting class613VarUnit3 = new NumberSetting(Lang.PARTICLES_HIT_AMOUNT).range(1.0f, 50.0f).currentValue(15.0f).step(1.0f).unit(SettingUnit.UNITS);
        BooleanSetting class665Var3 = this.hitParticles;
        Objects.requireNonNull(class665Var3);
        this.hitAmount = class613VarUnit3.visible(class665Var3::isValue);
        this.lifetime = new NumberSetting(Lang.PARTICLES_LIFETIME).range(10.0f, 200.0f).currentValue(60.0f).step(1.0f).unit(SettingUnit.TICKS).visible(() -> {
            return Boolean.valueOf(this.worldParticles.isValue() || this.hitParticles.isValue());
        });
        this.size = new NumberSetting(Lang.PARTICLES_SIZE).range(0.05f, 0.5f).currentValue(0.15f).unit(SettingUnit.UNITS).visible(() -> {
            return Boolean.valueOf(this.worldParticles.isValue() || this.hitParticles.isValue());
        });
        NumberSetting class613VarStep = new NumberSetting(Lang.PARTICLES_MAX).range(10.0f, 200.0f).currentValue(100.0f).unit(SettingUnit.UNITS).step(1.0f);
        BooleanSetting class665Var4 = this.worldParticles;
        Objects.requireNonNull(class665Var4);
        this.maxParticles = class613VarStep.visible(class665Var4::isValue);
        this.worldTexture = new ModeSetting<ParticleTexture>(
                Lang.PARTICLES_WORLD_TEXTURE,
                Lang.PARTICLES_WORLD_TEXTURE_DESC
        ).values(ParticleTexture.class)
                .options(ParticleTexture.selectableValues())
                .select(ParticleTexture.STAR)
                .visible(this.worldParticles::isValue);
        this.hitTexture = new ModeSetting<ParticleTexture>(
                Lang.PARTICLES_HIT_TEXTURE,
                Lang.PARTICLES_HIT_TEXTURE_DESC
        ).values(ParticleTexture.class)
                .options(ParticleTexture.selectableValues())
                .select(ParticleTexture.GLOW)
                .visible(this.hitParticles::isValue);
        this.worldColor = new ColorSetting(
                Lang.PARTICLES_WORLD_COLOR,
                Lang.PARTICLES_WORLD_COLOR_DESC
        ).value(0xFF8C7CFF).visible(this.worldParticles::isValue);
        this.hitColor = new ColorSetting(
                Lang.PARTICLES_HIT_COLOR,
                Lang.PARTICLES_HIT_COLOR_DESC
        ).value(0xFF8C7CFF).visible(this.hitParticles::isValue);
        this.particles = new ArrayList();
        this.cameraRotation = new Quaternionf();
        this.startTime = System.nanoTime();
        this.relativePos = new Vector3f();
        this.rightVector = new Vector3f();
        this.upVector = new Vector3f();
        this.cornerTopLeft = new Vector3f();
        this.cornerBottomLeft = new Vector3f();
        this.cornerBottomRight = new Vector3f();
        this.cornerTopRight = new Vector3f();
        addSettings(
                this.worldParticles,
                this.hitParticles,
                this.worldTexture,
                this.worldColor,
                this.hitTexture,
                this.hitColor,
                this.spawnRate,
                this.radius,
                this.hitAmount,
                this.lifetime,
                this.size,
                this.maxParticles
        );
        register(ClientTickEvent.class, class181Var -> {
            if (Mc.INSTANCE.isWorldLoaded()) {
                if (this.worldParticles.isValue() && isState()) {
                    spawnParticles();
                }
                if (this.particles.isEmpty()) {
                    return;
                }
                updateParticles(1.0f);
            }
        });
        register(AttackEntityEvent.class, class144Var -> {
            Entity entityAttacker;
            int iMin;
            if (!isState() || !this.hitParticles.isValue() || (entityAttacker = class144Var.attacker()) == null || (iMin = Math.min((int) this.hitAmount.currentValue(), 450 - this.particles.size())) <= 0) {
                return;
            }
            Vec3d pos = entityAttacker.getPos();
            for (int i = 0; i < iMin; i++) {
                this.particles.add(new ClientParticle(
                        pos.x + ((ThreadLocalRandom.current().nextDouble() - 0.5d) * ((double) entityAttacker.getWidth())),
                        pos.y + (ThreadLocalRandom.current().nextDouble() * ((double) entityAttacker.getHeight())),
                        pos.z + ((ThreadLocalRandom.current().nextDouble() - 0.5d) * ((double) entityAttacker.getWidth())),
                        (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d,
                        (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d,
                        (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d,
                        (int) this.lifetime.currentValue(),
                        true
                ));
            }
        });
        register(WorldRenderEvent.class, class016Var -> {
            if (this.particles.isEmpty()) {
                return;
            }
            this.tickDelta = Mc.INSTANCE.getRenderTickCounter().getTickDelta(false);
            renderParticles(class016Var.matrixStack(), class016Var.frustum());
        });
    }

    public void spawnParticles() {
        Vec3d pos = Mc.INSTANCE.getPlayer().getPos();
        for (int i = 0; i < 10 && this.particles.size() < this.maxParticles.currentValue(); i++) {
            double dNextDouble = ThreadLocalRandom.current().nextDouble() * 3.141592653589793d * 2.0d;
            double dNextDouble2 = ThreadLocalRandom.current().nextDouble() * ((double) this.radius.currentValue());
            Vec3d vec3dAdd = pos.add(Math.cos(dNextDouble) * dNextDouble2, (ThreadLocalRandom.current().nextDouble() - 0.5d) * dNextDouble2, Math.sin(dNextDouble) * dNextDouble2);
            Vec3d vec3d = new Vec3d((ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d, (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d, (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.2d);
            this.particles.add(new ClientParticle(
                    vec3dAdd.x, vec3dAdd.y, vec3dAdd.z,
                    vec3d.x, vec3d.y, vec3d.z,
                    (int) this.lifetime.currentValue(),
                    false
            ));
        }
    }

    public void updateParticles(float f) throws MatchException {
        Mc class815Var = Mc.INSTANCE;
        Iterator<ClientParticle> it = this.particles.iterator();
        float fCurrentValue = this.size.currentValue();
        double d = 0.003d * ((double) f);
        while (it.hasNext()) {
            ClientParticle next = it.next();
            next.size += f;
            if (next.size >= next.lifetime) {
                it.remove();
            } else {
                next.x = next.prevX;
                next.y = next.prevY;
                next.z = next.prevZ;
                next.vx += (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.002d * ((double) f);
                next.vz += (ThreadLocalRandom.current().nextDouble() - 0.5d) * 0.002d * ((double) f);
                double dPow = Math.pow(0.96d, f);
                next.vx *= dPow;
                next.vy = (next.vy * dPow) + d;
                next.vz *= dPow;
                double d2 = next.vx * ((double) f);
                double d3 = next.vy * ((double) f);
                double d4 = next.vz * ((double) f);
                Box box = new Box(next.prevX - ((double) fCurrentValue), next.prevY - ((double) fCurrentValue), next.prevZ - ((double) fCurrentValue), next.prevX + ((double) fCurrentValue), next.prevY + ((double) fCurrentValue), next.prevZ + ((double) fCurrentValue));
                double dMethod004 = collideAxis(box, d3, Direction.Axis.Y);
                if (dMethod004 != d3) {
                    next.vy = (-next.vy) * bounceFactor;
                    if (d3 < 0.0d) {
                        next.vx *= groundFriction;
                        next.vz *= groundFriction;
                    }
                    if (Math.abs(dMethod004) < epsilon) {
                        dMethod004 = d3 < 0.0d ? epsilon : -1.0E-4d;
                    }
                }
                next.prevY += dMethod004;
                Box boxOffset = box.offset(0.0d, dMethod004, 0.0d);
                double dMethod005 = collideAxis(boxOffset, d2, Direction.Axis.X);
                if (dMethod005 != d2) {
                    next.vx = (-next.vx) * bounceFactor;
                    next.vy *= wallFriction;
                    next.vz *= wallFriction;
                    if (Math.abs(dMethod005) < epsilon) {
                        dMethod005 = d2 < 0.0d ? epsilon : -1.0E-4d;
                    }
                }
                next.prevX += dMethod005;
                double dMethod006 = collideAxis(boxOffset.offset(dMethod005, 0.0d, 0.0d), d4, Direction.Axis.Z);
                if (dMethod006 != d4) {
                    next.vz = (-next.vz) * bounceFactor;
                    next.vy *= wallFriction;
                    next.vx *= wallFriction;
                    if (Math.abs(dMethod006) < epsilon) {
                        dMethod006 = d4 < 0.0d ? epsilon : -1.0E-4d;
                    }
                }
                next.prevZ += dMethod006;
                if (Math.abs(next.vx) < 1.0E-5d) {
                    next.vx = 0.0d;
                }
                if (Math.abs(next.vy) < 1.0E-5d) {
                    next.vy = 0.0d;
                }
                if (Math.abs(next.vz) < 1.0E-5d) {
                    next.vz = 0.0d;
                }
            }
        }
    }

    public double collideAxis(Box box, double d, Direction.Axis axis) throws MatchException {
        Box boxStretch;
        if (d == 0.0d) {
            return 0.0d;
        }
        double d2 = box.minX;
        double d3 = box.minY;
        double d4 = box.minZ;
        double d5 = box.maxX;
        double d6 = box.maxY;
        double d7 = box.maxZ;
        switch (AxisSwitchMap.switchMap[axis.ordinal()]) {
            case 1:
                boxStretch = box.stretch(d, 0.0d, 0.0d);
                break;
            case 2:
                boxStretch = box.stretch(0.0d, d, 0.0d);
                break;
            case 3:
                boxStretch = box.stretch(0.0d, 0.0d, d);
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        Box box2 = boxStretch;
        ClientWorld world = Mc.INSTANCE.getWorld();
        double[] dArr = {d};
        switch (AxisSwitchMap.switchMap[axis.ordinal()]) {
            case 1:
                Iterator it = world.getBlockCollisions((Entity) null, box2).iterator();
                while (it.hasNext()) {
                    ((VoxelShape) it.next()).forEachBox((d8, d9, d10, d11, d12, d13) -> {
                        if (d12 <= d3 || d9 >= d6 || d13 <= d4 || d10 >= d7) {
                            return;
                        }
                        if (d > 0.0d) {
                            double da8 = d8 - d5;
                            if (da8 < 0.0d || da8 >= dArr[0]) {
                                return;
                            }
                            dArr[0] = da8;
                            return;
                        }
                        double da9 = d11 - d2;
                        if (da9 > 0.0d || da9 <= dArr[0]) {
                            return;
                        }
                        dArr[0] = da9;
                    });
                    if (dArr[0] == 0.0d) {
                        break;
                    }
                }
                break;
            case 2:
                Iterator it2 = world.getBlockCollisions((Entity) null, box2).iterator();
                while (it2.hasNext()) {
                    ((VoxelShape) it2.next()).forEachBox((d14, d15, d16, d17, d18, d19) -> {
                        if (d17 <= d2 || d14 >= d5 || d19 <= d4 || d16 >= d7) {
                            return;
                        }
                        if (d > 0.0d) {
                            double da14 = d15 - d6;
                            if (da14 < 0.0d || da14 >= dArr[0]) {
                                return;
                            }
                            dArr[0] = da14;
                            return;
                        }
                        double da15 = d18 - d3;
                        if (da15 > 0.0d || da15 <= dArr[0]) {
                            return;
                        }
                        dArr[0] = da15;
                    });
                    if (dArr[0] == 0.0d) {
                        break;
                    }
                }
                break;
            case 3:
                Iterator it3 = world.getBlockCollisions((Entity) null, box2).iterator();
                while (it3.hasNext()) {
                    ((VoxelShape) it3.next()).forEachBox((d20, d21, d22, d23, d24, d25) -> {
                        if (d23 <= d2 || d20 >= d5 || d24 <= d3 || d21 >= d6) {
                            return;
                        }
                        if (d > 0.0d) {
                            double da20 = d22 - d7;
                            if (da20 < 0.0d || da20 >= dArr[0]) {
                                return;
                            }
                            dArr[0] = da20;
                            return;
                        }
                        double da21 = d25 - d4;
                        if (da21 > 0.0d || da21 <= dArr[0]) {
                            return;
                        }
                        dArr[0] = da21;
                    });
                    if (dArr[0] == 0.0d) {
                        break;
                    }
                }
                break;
        }
        return dArr[0];
    }

    public void renderParticles(MatrixStack matrixStack, Frustum frustum) {
        if (this.particles.isEmpty()) {
            return;
        }
        Camera camera = Mc.INSTANCE.getCamera();
        Vec3d pos = camera.getPos();
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        this.cameraRotation.set(camera.getRotation());
        this.upVector.set(0.0f, 1.0f, 0.0f).rotate(this.cameraRotation);
        this.rightVector.set(1.0f, 0.0f, 0.0f).rotate(this.cameraRotation);
        renderParticleBatch(positionMatrix, frustum, pos, false,
                this.worldTexture.currentValue(), this.worldColor);
        renderParticleBatch(positionMatrix, frustum, pos, true,
                this.hitTexture.currentValue(), this.hitColor);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderParticleBatch(Matrix4f positionMatrix, Frustum frustum, Vec3d cameraPos,
                                     boolean hitGroup, ParticleTexture texture, ColorSetting color) {
        float particleSize = this.size.currentValue();
        if (!hasVisibleParticle(frustum, hitGroup, particleSize)) {
            return;
        }

        RenderSystem.setShaderTexture(0, texture.texture().textureWithSTB());
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;
        float alpha = color.getAlpha();
        for (ClientParticle particle : this.particles) {
            if (particle.hitParticle != hitGroup) {
                continue;
            }
            float particleAlpha = particle.fadeAlpha(particle.size) * alpha;
            if (particleAlpha <= 0.0f) {
                continue;
            }
            double renderX = particle.x + ((particle.prevX - particle.x) * this.tickDelta);
            double renderY = particle.y + ((particle.prevY - particle.y) * this.tickDelta);
            double renderZ = particle.z + ((particle.prevZ - particle.z) * this.tickDelta);
            if (!frustum.isVisible(particleBounds(renderX, renderY, renderZ, particleSize))) {
                continue;
            }

            this.relativePos.set(
                    (float) (renderX - cameraPos.x),
                    (float) (renderY - cameraPos.y),
                    (float) (renderZ - cameraPos.z)
            );
            this.cornerTopLeft.set(this.relativePos)
                    .fma(-particleSize, this.rightVector).fma(particleSize, this.upVector);
            this.cornerBottomLeft.set(this.relativePos)
                    .fma(-particleSize, this.rightVector).fma(-particleSize, this.upVector);
            this.cornerBottomRight.set(this.relativePos)
                    .fma(particleSize, this.rightVector).fma(-particleSize, this.upVector);
            this.cornerTopRight.set(this.relativePos)
                    .fma(particleSize, this.rightVector).fma(particleSize, this.upVector);
            buffer.vertex(positionMatrix, this.cornerTopLeft.x, this.cornerTopLeft.y, this.cornerTopLeft.z)
                    .texture(0.0f, 0.0f).color(red, green, blue, particleAlpha);
            buffer.vertex(positionMatrix, this.cornerBottomLeft.x, this.cornerBottomLeft.y, this.cornerBottomLeft.z)
                    .texture(0.0f, 1.0f).color(red, green, blue, particleAlpha);
            buffer.vertex(positionMatrix, this.cornerBottomRight.x, this.cornerBottomRight.y, this.cornerBottomRight.z)
                    .texture(1.0f, 1.0f).color(red, green, blue, particleAlpha);
            buffer.vertex(positionMatrix, this.cornerTopRight.x, this.cornerTopRight.y, this.cornerTopRight.z)
                    .texture(1.0f, 0.0f).color(red, green, blue, particleAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private boolean hasVisibleParticle(Frustum frustum, boolean hitGroup, float particleSize) {
        for (ClientParticle particle : this.particles) {
            if (particle.hitParticle != hitGroup || particle.fadeAlpha(particle.size) <= 0.0f) {
                continue;
            }
            double renderX = particle.x + ((particle.prevX - particle.x) * this.tickDelta);
            double renderY = particle.y + ((particle.prevY - particle.y) * this.tickDelta);
            double renderZ = particle.z + ((particle.prevZ - particle.z) * this.tickDelta);
            if (frustum.isVisible(particleBounds(renderX, renderY, renderZ, particleSize))) {
                return true;
            }
        }
        return false;
    }

    private static Box particleBounds(double x, double y, double z, float size) {
        return new Box(x - size, y - size, z - size, x + size, y + size, z + size);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        for (ClientParticle class560Var : this.particles) {
            class560Var.lifetime = (int) Math.min(class560Var.lifetime, class560Var.size + 20.0f);
        }
    }

    public enum ParticleTexture implements DisplayNamed {
        STAR("Stars", "star.png"),
        HEART("Hearts", "heart.png"),
        SNOWFLAKE("Snowflakes", "snowflake.png"),
        GLOW("Glow", "glow.png"),
        DOLLAR("Dollars", "dollar.png"),
        CROSS("Crosses", "cross.png"),
        CIRCLE("Circles", "circle.png"),
        TRIANGLE("Triangles", "triangle.png"),
        BUBBLE("Bubbles", "bubble.png");

        private final Translation displayName;
        private final GlTexture texture;

        ParticleTexture(String displayName, String textureName) {
            this.displayName = Translation.clearText(displayName);
            this.texture = new GlTexture(new ClasspathResource("/textures/particles/" + textureName));
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }

        public GlTexture texture() {
            return this.texture;
        }

        public static ParticleTexture[] selectableValues() {
            return new ParticleTexture[]{STAR, SNOWFLAKE, GLOW, DOLLAR, CROSS};
        }
    }
}
