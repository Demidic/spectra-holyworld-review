package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.internal.DirectionSwitchMap;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.IteratorUtil;
import ru.spectra.client.Lang;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.util.PlayerSnapshotManager;
import ru.spectra.client.math.ProjectileTrajectory;
import ru.spectra.client.util.RaycastUtil;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.util.RotationManager;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.ShapeType;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.model.TrajectoryPoint;
import ru.spectra.client.event.WorldRenderEvent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.util.Arm;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Pair;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

@Aliases(aliases = {"Pearl Prediction", "Projectile Prediction", "Arrow Prediction"})
public class ProjectilePredictionModule extends Module {
    public final Mc mc;
    public final ColorSetting color;
    public final BooleanSetting handPrediction;
    public final BooleanSetting areaPrediction;

    public ProjectilePredictionModule() {
        super(ModuleTab.RENDER, "Projectile Prediction");
        this.mc = Mc.INSTANCE;
        this.color = new ColorSetting(Lang.PROJECTILE_PREDICTION_COLOR);
        this.handPrediction = new BooleanSetting(Lang.PROJECTILE_PREDICTION_HAND, Lang.PROJECTILE_PREDICTION_HAND_DESC).setValue(true);
        this.areaPrediction = new BooleanSetting(Lang.PROJECTILE_PREDICTION_AREA, Lang.PROJECTILE_PREDICTION_AREA_DESC).setValue(true);
        addSettings(this.handPrediction, this.areaPrediction, this.color);
        register(WorldRenderEvent.class, class016Var -> {
            if (isState() && this.mc.isWorldLoaded()) {
                MatrixStack matrixStack = class016Var.matrixStack();
                ColorStack class115VarColorStack = Spectra.INSTANCE.drawEngine().colorStack();
                Rotation interpolatedRotation = RotationManager.INSTANCE.getInterpolatedRotation();
                if (this.handPrediction.isValue()
                        && this.mc.getGameOptions().getPerspective() == Perspective.FIRST_PERSON) {
                    drawPredictionItem(matrixStack, this.mc.getPlayer().getMainHandStack(),
                            interpolatedRotation, Hand.MAIN_HAND);
                    drawPredictionItem(matrixStack, this.mc.getPlayer().getOffHandStack(),
                            interpolatedRotation, Hand.OFF_HAND);
                }
                for (ProjectileTrajectory class371Var : PlayerSnapshotManager.INSTANCE.getProjectiles()) {
                    List listSteps = class371Var.steps();
                    if (listSteps != null && listSteps.size() >= 2) {
                        ItemStack itemStackStack = class371Var.stack();
                        for (int i = 1; i < listSteps.size(); i++) {
                            TrajectoryPoint class372Var = (TrajectoryPoint) listSteps.get(i - 1);
                            TrajectoryPoint class372Var2 = (TrajectoryPoint) listSteps.get(i);
                            ShapeRenderer.INSTANCE.addLine(matrixStack.peek().getPositionMatrix(), class372Var.pos(), class372Var2.pos(), class115VarColorStack.computeColor(this.color.getColor(), MathUtil.clamp((class372Var2.tick() - this.mc.getPlayer().age) / 25.0f, 0.0f, 1.0f)), 3.0f);
                        }
                        Vec3d vec3dPos = ((TrajectoryPoint) listSteps.getLast()).pos();
                        if (this.areaPrediction.isValue()) {
                            int iComputeColor = class115VarColorStack.computeColor(this.color.getColor(), 0.5f);
                            if ((itemStackStack.getItem() instanceof LingeringPotionItem) || (itemStackStack.getItem() instanceof SplashPotionItem)) {
                                drawAreaCircle(matrixStack, vec3dPos, 3.0f, iComputeColor);
                            } else if (isSpecialSnowball(itemStackStack)) {
                                drawAreaCircle(matrixStack, vec3dPos, 7.0f, iComputeColor);
                            }
                        }
                    }
                }
            }
        });
    }

    public void drawPredictionItem(MatrixStack matrixStack, ItemStack itemStack, Rotation class007Var) {
        drawPredictionItem(matrixStack, itemStack, class007Var, Hand.MAIN_HAND);
    }

    public void drawPredictionItem(MatrixStack matrixStack, ItemStack itemStack,
                                   Rotation rotation, Hand hand) {
        Vec3d origin = firstPersonHandOrigin(rotation, hand);
        HandTrajectory trajectory = handTrajectory(itemStack, rotation, origin);
        if (trajectory == null || trajectory.points().size() < 2) {
            return;
        }
        List<Vec3d> points = trajectory.points();
        int baseColor = this.color.getColor();
        for (int index = 1; index < points.size(); index++) {
            float progress = index / (float) (points.size() - 1);
            int lineColor = Spectra.INSTANCE.drawEngine().colorStack().computeColor(
                    baseColor, 1.0f - progress * 0.55f);
            ShapeRenderer.INSTANCE.addLine(matrixStack.peek().getPositionMatrix(),
                    points.get(index - 1), points.get(index), lineColor,
                    index < 4 ? 3.4f : 2.6f);
        }
        if (trajectory.hit() != null) {
            drawImpactMarker(matrixStack.peek().getPositionMatrix(), trajectory.hit(), baseColor);
        }
    }

    private Vec3d firstPersonHandOrigin(Rotation rotation, Hand hand) {
        ClientPlayerEntity player = this.mc.getPlayer();
        Camera camera = this.mc.getCamera();
        Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).normalize();
        Vec3d right = new Vec3d(0.0, 1.0, 0.0).crossProduct(forward);
        if (right.lengthSquared() > 1.0E-6) {
            right = right.normalize();
        }
        Arm arm = hand == Hand.MAIN_HAND
                ? player.getMainArm()
                : (player.getMainArm() == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT);
        double side = arm == Arm.RIGHT ? -0.22 : 0.22;
        Vec3d up = forward.crossProduct(right).normalize();
        return camera.getPos()
                .add(forward.multiply(0.42))
                .add(right.multiply(side))
                .subtract(up.multiply(0.24));
    }

    private HandTrajectory handTrajectory(ItemStack stack, Rotation rotation, Vec3d origin) {
        Item item = stack.getItem();
        Item active = this.mc.getPlayer().getActiveItem().getItem();
        ProjectileEntity projectile;
        Vec3d direction = rotation.getDirectionVector();
        double speed;
        boolean thrown = true;
        if (item instanceof TridentItem) {
            if (item != active || this.mc.getPlayer().getItemUseTime() < 10) return null;
            projectile = new TridentEntity(this.mc.getWorld(), this.mc.getPlayer(), stack);
            speed = 2.5;
        } else if (item instanceof SnowballItem) {
            projectile = new SnowballEntity(this.mc.getWorld(), this.mc.getPlayer(), stack);
            speed = 1.5;
        } else if (item instanceof EggItem) {
            projectile = new EggEntity(this.mc.getWorld(), this.mc.getPlayer(), stack);
            speed = 1.5;
        } else if (item instanceof EnderPearlItem) {
            projectile = new EnderPearlEntity(this.mc.getWorld(), this.mc.getPlayer(), stack);
            speed = 1.5;
        } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            projectile = new PotionEntity(this.mc.getWorld(), this.mc.getPlayer(), stack);
            direction = new Vec3d(direction.x,
                    -MathHelper.sin((rotation.getPitch() - 20.0f) * 0.017453292f),
                    direction.z);
            speed = 0.5;
        } else if (item instanceof BowItem) {
            if (item != active || !this.mc.getPlayer().isUsingItem()) return null;
            projectile = new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), stack, stack);
            speed = 3.0f * MathUtil.clamp(
                    (this.mc.getPlayer().getItemUseTime() + this.mc.getTickDelta()) / 20.0f,
                    0.0f, 1.0f);
            thrown = false;
        } else if (item instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            projectile = new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), stack, stack);
            ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            speed = charged != null && !charged.isEmpty()
                    && charged.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET)
                    ? 1.6 : 3.15;
            thrown = false;
        } else {
            return null;
        }
        Vec3d velocity = direction.multiply(speed /
                Math.max(1.0E-6, MathHelper.sqrt(direction.toVector3f().lengthSquared())))
                .add(getMotion(projectile));
        return traceHandTrajectory(origin, velocity, projectile, thrown, 300);
    }

    private HandTrajectory traceHandTrajectory(Vec3d start, Vec3d velocity,
                                             ProjectileEntity projectile,
                                             boolean thrown, int maxSteps) {
        List<Box> hitboxes = IteratorUtil.toList(this.mc.getWorld().getEntities().iterator())
                .stream().filter(entity -> entity.canBeHitByProjectile()
                        && entity != projectile.getOwner())
                .map(entity -> entity.getBoundingBox().expand(0.30000001192092896d))
                .toList();
        ArrayList<Vec3d> points = new ArrayList<>();
        Vec3d position = start;
        points.add(position);
        for (int step = 0; step < maxSteps; step++) {
            if (thrown) {
                velocity = calculateMotion(projectile, position, velocity)
                        .add(0.0, -projectile.getFinalGravity(), 0.0);
            }
            Vec3d next = position.add(velocity);
            BlockHitResult hit = raycastStep(projectile, hitboxes, position, next);
            if (hit != null) {
                points.add(hit.getPos());
                return new HandTrajectory(points, hit);
            }
            points.add(next);
            if (!thrown) {
                velocity = calculateMotion(projectile, position,
                        velocity.add(0.0, -projectile.getFinalGravity(), 0.0));
            }
            position = next;
            if (position.y < -128.0) break;
        }
        return new HandTrajectory(points, null);
    }

    private void drawImpactMarker(Matrix4f matrix, BlockHitResult hit, int color) {
        Vec3d normal = Vec3d.of(hit.getSide().getVector());
        Vec3d center = hit.getPos().add(normal.multiply(0.012));
        Vec3d axisA;
        Vec3d axisB;
        if (hit.getSide().getAxis() == Direction.Axis.Y) {
            axisA = new Vec3d(1.0, 0.0, 0.0);
            axisB = new Vec3d(0.0, 0.0, 1.0);
        } else if (hit.getSide().getAxis() == Direction.Axis.Z) {
            axisA = new Vec3d(1.0, 0.0, 0.0);
            axisB = new Vec3d(0.0, 1.0, 0.0);
        } else {
            axisA = new Vec3d(0.0, 0.0, 1.0);
            axisB = new Vec3d(0.0, 1.0, 0.0);
        }
        int markerColor = Spectra.INSTANCE.drawEngine().colorStack().computeColor(color, 0.95f);
        Vec3d previous = null;
        int segments = 32;
        double radius = 0.13;
        for (int index = 0; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            Vec3d point = center.add(axisA.multiply(Math.cos(angle) * radius))
                    .add(axisB.multiply(Math.sin(angle) * radius));
            if (previous != null) {
                ShapeRenderer.INSTANCE.addLine(matrix, previous, point, markerColor, 2.4f);
            }
            previous = point;
        }
    }

    private record HandTrajectory(List<Vec3d> points, BlockHitResult hit) {
    }

    private void drawLegacyPredictionItem(MatrixStack matrixStack, ItemStack itemStack, Rotation class007Var) {
        if ((itemStack.getItem() instanceof SplashPotionItem) || (itemStack.getItem() instanceof LingeringPotionItem)) {
            drawPotionPrediction(matrixStack, itemStack, class007Var);
            return;
        }
        List<BlockHitResult> trajectoryResult = getTrajectoryResult(itemStack, class007Var);
        if (trajectoryResult == null || trajectoryResult.isEmpty()) {
            return;
        }
        renderProjectileResults(matrixStack, trajectoryResult, itemStack);
    }

    public void drawPotionPrediction(MatrixStack matrixStack, ItemStack itemStack, Rotation class007Var) {
        ClientPlayerEntity player = this.mc.getPlayer();
        PotionEntity potionEntity = new PotionEntity(this.mc.getWorld(), player, itemStack);
        Vec3d directionVector = class007Var.getDirectionVector();
        Vec3d vec3d = new Vec3d(directionVector.x, -MathHelper.sin((class007Var.getPitch() - 20.0f) * 0.017453292f), directionVector.z);
        Vec3d vec3dAdd = player.getEyePos().add(MathUtil.interpolate(player).subtract(player.getPos()));
        Vec3d vec3dAdd2 = vec3d.multiply(0.5d / ((double) MathHelper.sqrt(vec3d.toVector3f().lengthSquared()))).add(getMotion(potionEntity));
        List<Box> list = IteratorUtil.toList(this.mc.getWorld().getEntities().iterator()).stream().filter(entity -> {
            return entity.canBeHitByProjectile() && entity != potionEntity.getOwner();
        }).map(entity2 -> {
            return entity2.getBoundingBox().expand(0.30000001192092896d);
        }).toList();
        ArrayList arrayList = new ArrayList();
        arrayList.add(vec3dAdd);
        Vec3d vec3d2 = vec3dAdd;
        BlockHitResult blockHitResult = null;
        for (int i = 0; i < 300; i++) {
            Vec3d vec3dAdd3 = calculateMotion(potionEntity, vec3d2, vec3dAdd2).add(0.0d, -potionEntity.getFinalGravity(), 0.0d);
            vec3dAdd2 = vec3dAdd3;
            Vec3d vec3dAdd4 = vec3d2.add(vec3dAdd3);
            BlockHitResult blockHitResultMethod004 = raycastStep(potionEntity, list, vec3d2, vec3dAdd4);
            if (blockHitResultMethod004 != null) {
                arrayList.add(blockHitResultMethod004.getPos());
                blockHitResult = blockHitResultMethod004;
                break;
            } else {
                arrayList.add(vec3dAdd4);
                vec3d2 = vec3dAdd4;
            }
        }
        if (blockHitResult == null) {
            blockHitResult = new BlockHitResult(BlockPos.ORIGIN.toCenterPos(), Direction.DOWN, BlockPos.ORIGIN.down(999), true);
        }
        matrixStack.peek().getPositionMatrix();
        this.color.getColor();
        Vec3d pos = blockHitResult.getPos();
        int iArgb = blockHitResult.getBlockPos().equals(BlockPos.ORIGIN) ? ThemePalette.darkRed.argb() : this.color.getColor();
        drawLandingMarker(matrixStack, pos, blockHitResult.getSide(), iArgb);
        if (this.areaPrediction.isValue()) {
            drawAreaCircle(matrixStack, pos, itemStack.getItem() instanceof LingeringPotionItem ? 4.0f : 3.0f, iArgb);
        }
    }

    public List<BlockHitResult> getTrajectoryResult(ItemStack itemStack, Rotation class007Var) {
        Item item = this.mc.getPlayer().getActiveItem().getItem();
        Item item2 = itemStack.getItem();
        Objects.requireNonNull(item2);
        if (item2 instanceof TridentItem) {
            if (((TridentItem) item2).equals(item) && this.mc.getPlayer().getItemUseTime() >= 10) {
                return checkTrajectory(new TridentEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack), 2.5d, class007Var);
            }
            return new ArrayList();
        }
        if (item2 instanceof SnowballItem) {
            return checkTrajectory(new SnowballEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack), 1.5d, class007Var);
        }
        if (item2 instanceof EggItem) {
            return checkTrajectory(new EggEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack), 1.5d, class007Var);
        }
        if (item2 instanceof EnderPearlItem) {
            return checkTrajectory(new EnderPearlEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack), 1.5d, class007Var);
        }
        if (item2 instanceof BowItem) {
            if (((BowItem) item2).equals(item) && this.mc.getPlayer().isUsingItem()) {
                return checkTrajectory(new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack, itemStack), 3.0f * MathUtil.clamp((this.mc.getPlayer().getItemUseTime() + this.mc.getTickDelta()) / 20.0f, 0.0f, 1.0f), class007Var);
            }
            return new ArrayList();
        }
        if (item2 instanceof SplashPotionItem) {
            return getPotionTrajectory(itemStack, class007Var);
        }
        if (item2 instanceof LingeringPotionItem) {
            return getPotionTrajectory(itemStack, class007Var);
        }
        if (item2 instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(itemStack)) {
                ChargedProjectilesComponent chargedProjectilesComponent = (ChargedProjectilesComponent) itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                ArrayList arrayList = new ArrayList();
                if (chargedProjectilesComponent != null && !chargedProjectilesComponent.isEmpty()) {
                    float f = ((ItemStack) chargedProjectilesComponent.getProjectiles().getFirst()).isOf(Items.FIREWORK_ROCKET) ? 1.6f : 3.15f;
                    arrayList.add(checkTrajectory(class007Var.getDirectionVector(), new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack, itemStack), f, false));
                    if (chargedProjectilesComponent.getProjectiles().size() > 1) {
                        arrayList.add(checkTrajectory(class007Var.add(-10.0f, 0.0f).getDirectionVector(), new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack, itemStack), f, false));
                        arrayList.add(checkTrajectory(class007Var.add(10.0f, 0.0f).getDirectionVector(), new ArrowEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack, itemStack), f, false));
                    }
                }
                return arrayList;
            }
            return new ArrayList();
        }
        return new ArrayList();
    }

    public void renderProjectileResults(MatrixStack matrixStack, List<BlockHitResult> list, ItemStack itemStack) {
        for (BlockHitResult blockHitResult : list) {
            Vec3d pos = blockHitResult.getPos();
            int iArgb = blockHitResult.getBlockPos().equals(BlockPos.ORIGIN) ? ThemePalette.darkRed.argb() : this.color.getColor();
            if (this.areaPrediction.isValue()) {
                if ((itemStack.getItem() instanceof SplashPotionItem) || (itemStack.getItem() instanceof LingeringPotionItem)) {
                    drawAreaCircle(matrixStack, pos, 3.0f, iArgb);
                } else if (isSpecialSnowball(itemStack)) {
                    drawAreaCircle(matrixStack, pos, 7.0f, iArgb);
                }
            }
            drawLandingMarker(matrixStack, pos, blockHitResult.getSide(), iArgb);
        }
    }

    public void drawLandingMarker(MatrixStack matrixStack, Vec3d vec3d, Direction direction, int i) {
        Vec3d vec3dAdd = vec3d.subtract(this.mc.getEntityRenderDispatcher().camera.getPos()).add(Vec3d.of(direction.getVector()).multiply(0.001d));
        matrixStack.push();
        matrixStack.translate(vec3dAdd.x, vec3dAdd.y, vec3dAdd.z);
        switch (DirectionSwitchMap.directionOrdinals[direction.ordinal()]) {
            case 1:
            case 2:
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0f));
                break;
            case 3:
            case 4:
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                break;
        }
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        for (int i2 = 0; i2 < 48; i2++) {
            float f = (float) (((((double) i2) * 3.141592653589793d) * 2.0d) / ((double) 48));
            float f2 = (float) (((((double) (i2 + 1)) * 3.141592653589793d) * 2.0d) / ((double) 48));
            float fCos = (float) Math.cos(f);
            float fSin = (float) Math.sin(f);
            float fCos2 = (float) Math.cos(f2);
            float fSin2 = (float) Math.sin(f2);
            float f3 = 0.35f - 0.02f;
            float f4 = 0.35f + 0.02f;
            ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3d(fCos * f3, 0.0d, fSin * f3), new Vec3d(fCos * f4, 0.0d, fSin * f4), new Vec3d(fCos2 * f4, 0.0d, fSin2 * f4), new Vec3d(fCos2 * f3, 0.0d, fSin2 * f3), i);
        }
        float f5 = 0.35f * 0.6f;
        ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3d(-f5, 0.0d, -0.02f), new Vec3d(-f5, 0.0d, 0.02f), new Vec3d(f5, 0.0d, 0.02f), new Vec3d(f5, 0.0d, -0.02f), i);
        ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3d(-0.02f, 0.0d, -f5), new Vec3d(-0.02f, 0.0d, f5), new Vec3d(0.02f, 0.0d, f5), new Vec3d(0.02f, 0.0d, -f5), i);
        matrixStack.pop();
    }

    public void drawAreaCircle(MatrixStack matrixStack, Vec3d vec3d, float f, int i) {
        Vec3d vec3dSubtract = vec3d.subtract(this.mc.getEntityRenderDispatcher().camera.getPos());
        matrixStack.push();
        matrixStack.translate(vec3dSubtract.x, vec3dSubtract.y + 0.01d, vec3dSubtract.z);
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        for (int i2 = 0; i2 < 64; i2++) {
            float f2 = (float) (((((double) i2) * 3.141592653589793d) * 2.0d) / ((double) 64));
            float f3 = (float) (((((double) (i2 + 1)) * 3.141592653589793d) * 2.0d) / ((double) 64));
            float fCos = (float) Math.cos(f2);
            float fSin = (float) Math.sin(f2);
            float fCos2 = (float) Math.cos(f3);
            float fSin2 = (float) Math.sin(f3);
            float f4 = f - 0.03f;
            float f5 = f + 0.03f;
            ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3d(fCos * f4, 0.0d, fSin * f4), new Vec3d(fCos * f5, 0.0d, fSin * f5), new Vec3d(fCos2 * f5, 0.0d, fSin2 * f5), new Vec3d(fCos2 * f4, 0.0d, fSin2 * f4), i);
        }
        matrixStack.pop();
    }

    public List<BlockHitResult> getPotionTrajectory(ItemStack itemStack, Rotation class007Var) {
        Vec3d directionVector = class007Var.getDirectionVector();
        return List.of(checkTrajectory(new Vec3d(directionVector.x, -MathHelper.sin((class007Var.getPitch() - 20.0f) * 0.017453292f), directionVector.z), new PotionEntity(this.mc.getWorld(), this.mc.getPlayer(), itemStack), 0.5d, true));
    }

    public List<BlockHitResult> checkTrajectory(ProjectileEntity projectileEntity, double d, Rotation class007Var) {
        return List.of(checkTrajectory(class007Var.getDirectionVector(), projectileEntity, d, true));
    }

    public BlockHitResult checkTrajectory(Vec3d vec3d, ProjectileEntity projectileEntity, double d, boolean z) {
        return checkTrajectory(this.mc.getPlayer().getEyePos(), vec3d, projectileEntity, d, z);
    }

    public BlockHitResult checkTrajectory(Vec3d vec3d, Vec3d vec3d2, ProjectileEntity projectileEntity, double d, boolean z) {
        ClientPlayerEntity player = this.mc.getPlayer();
        this.mc.getTickDelta();
        return traceTrajectory(vec3d.add(MathUtil.interpolate(player).subtract(player.getPos())), vec3d2.multiply(d / ((double) MathHelper.sqrt(vec3d2.toVector3f().lengthSquared()))).add(getMotion(projectileEntity)), projectileEntity, 300);
    }

    public BlockHitResult traceTrajectory(Vec3d vec3d, Vec3d vec3d2, ProjectileEntity projectileEntity, int i) {
        return traceTrajectory(vec3d, vec3d2, projectileEntity, i, IteratorUtil.toList(this.mc.getWorld().getEntities().iterator()).stream().filter(entity -> {
            return entity.canBeHitByProjectile() && entity != projectileEntity.getOwner();
        }).map(entity2 -> {
            return entity2.getBoundingBox().expand(0.30000001192092896d);
        }).toList());
    }

    public BlockHitResult traceTrajectory(Vec3d vec3d, Vec3d vec3d2, ProjectileEntity projectileEntity, int i, List<Box> list) {
        Objects.requireNonNull(projectileEntity);
        if (projectileEntity instanceof ThrownItemEntity) {
            ThrownItemEntity thrownItemEntity = (ThrownItemEntity) projectileEntity;
            for (int i2 = 0; i2 < i; i2++) {
                Vec3d vec3dAdd = calculateMotion(thrownItemEntity, vec3d, vec3d2).add(0.0d, -thrownItemEntity.getFinalGravity(), 0.0d);
                vec3d2 = vec3dAdd;
                Vec3d vec3dAdd2 = vec3d.add(vec3dAdd);
                BlockHitResult blockHitResultMethod004 = raycastStep(thrownItemEntity, list, vec3d, vec3dAdd2);
                if (blockHitResultMethod004 != null) {
                    return blockHitResultMethod004;
                }
                vec3d = vec3dAdd2;
            }
        } else {
            for (int i3 = 0; i3 < i; i3++) {
                Vec3d vec3dAdd3 = vec3d.add(vec3d2);
                BlockHitResult blockHitResultMethod005 = raycastStep(projectileEntity, list, vec3d, vec3dAdd3);
                if (blockHitResultMethod005 != null) {
                    return blockHitResultMethod005;
                }
                vec3d2 = calculateMotion(projectileEntity, vec3d, vec3d2.add(0.0d, -projectileEntity.getFinalGravity(), 0.0d));
                vec3d = vec3dAdd3;
            }
        }
        return new BlockHitResult(BlockPos.ORIGIN.toCenterPos(), Direction.DOWN, BlockPos.ORIGIN.down(999), true);
    }

    public BlockHitResult raycastStep(Entity entity, List<Box> list, Vec3d vec3d, Vec3d vec3d2) {
        BlockHitResult blockHitResultRaycast = RaycastUtil.raycast(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, entity);
        BlockHitResult blockHitResultRaycast2 = Box.raycast(list, vec3d, blockHitResultRaycast.getPos(), BlockPos.ORIGIN);
        if (blockHitResultRaycast2 != null && blockHitResultRaycast2.getType() != HitResult.Type.MISS) {
            return blockHitResultRaycast2;
        }
        if (blockHitResultRaycast.getType() != HitResult.Type.MISS || vec3d2.y < -128.0d) {
            return blockHitResultRaycast;
        }
        return null;
    }

    public Pair<List<ChunkPos>, List<TrajectoryPoint>> predictEntity(Entity entity, Vec3d vec3d, Vec3d vec3d2, boolean z) {
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        for (int i = 0; i < 1000; i++) {
            if (z) {
                vec3d = calculateMotion(entity, vec3d2, vec3d.add(0.0d, -entity.getFinalGravity(), 0.0d));
            }
            Vec3d vec3dAdd = vec3d2.add(vec3d);
            BlockHitResult blockHitResultRaycast = RaycastUtil.raycast(vec3d2, vec3dAdd, RaycastContext.ShapeType.COLLIDER, entity);
            arrayList.add(new TrajectoryPoint(this.mc.getPlayer().age + i, vec3d, vec3d2, blockHitResultRaycast.getPos()));
            hashSet.add(new ChunkPos(BlockPos.ofFloored(vec3d2)));
            if (blockHitResultRaycast.getType() != HitResult.Type.MISS || vec3dAdd.y < -128.0d) {
                break;
            }
            if (!z) {
                vec3d = calculateMotion(entity, vec3d2, vec3d).add(0.0d, -entity.getFinalGravity(), 0.0d);
            }
            vec3d2 = vec3dAdd;
        }
        return new Pair<>(new ArrayList(hashSet), arrayList);
    }

    public Vec3d calculateMotion(Entity entity, Vec3d vec3d, Vec3d vec3d2) {
        double d;
        boolean zIsIn = this.mc.getWorld().getBlockState(BlockPos.ofFloored(vec3d)).getFluidState().isIn(FluidTags.WATER);
        Objects.requireNonNull(entity);
        if (entity instanceof TridentEntity) {
            d = 0.99d;
        } else if ((entity instanceof PersistentProjectileEntity) && zIsIn) {
            d = 0.6d;
        } else {
            d = !zIsIn ? 0.99d : 0.8d;
        }
        return vec3d2.multiply(d);
    }

    public Vec3d getMotion(ProjectileEntity projectileEntity) {
        int protocolVersion = ServerUtil.getProtocolVersion();
        if (((projectileEntity instanceof ArrowEntity) && ((ArrowEntity) projectileEntity).getItemStack().isOf(Items.CROSSBOW)) || ((projectileEntity instanceof ThrownItemEntity) && protocolVersion > 754 && protocolVersion < 767 && !ServerUtil.isConnectedToServer("holyworld"))) {
            return Vec3d.ZERO;
        }
        Vec3d[] vec3dArr = (Vec3d[]) PlayerSnapshotManager.INSTANCE.getSnapshots(this.mc.getPlayer(), 2).map(class373Var -> {
            return class373Var.pos;
        }).toList().toArray(i -> {
            return new Vec3d[i];
        });
        return vec3dArr.length < 3 ? Vec3d.ZERO : MathUtil.interpolate(vec3dArr[1].subtract(vec3dArr[2]), vec3dArr[0].subtract(vec3dArr[1]));
    }

    public boolean isSpecialSnowball(ItemStack itemStack) {
        return (itemStack.getItem() instanceof SnowballItem) && ServerUtil.isConnectedToAllFuntimeServers() && ((NbtComponent) itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).copyNbt().contains("don-item");
    }
}
