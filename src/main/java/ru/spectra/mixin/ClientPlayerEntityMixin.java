package ru.spectra.mixin;

import ru.spectra.client.math.DirectionalInput;
import ru.spectra.client.event.PlayerInitEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.type.TickStage;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.MovementUpdateEvent;
import ru.spectra.client.event.MovementUpdateSource;
import ru.spectra.client.module.ElytraHelperModule;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow
    protected int ticksLeftToDoubleTapSprint;

    @Shadow
    public Input input;

    @Shadow
    private boolean inSneakingPose;

    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    private int ticksToNextAutoJump;

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    private boolean falling;

    @Shadow
    private int underwaterVisibilityTicks;

    @Shadow
    private int field_3938;

    @Shadow
    private float mountJumpStrength;

    public ClientPlayerEntityMixin(ClientWorld clientWorld, GameProfile gameProfile) {
        super(clientWorld, gameProfile);
    }

    @Shadow
    protected abstract void tickNausea(boolean z);

    @Shadow
    protected abstract void pushOutOfBlocks(double d, double d2);

    @Shadow
    public abstract Portal.Effect getCurrentPortalEffect();

    @Shadow
    public abstract boolean isWalking();

    @Shadow
    public abstract boolean shouldSlowDown();

    @Shadow
    protected abstract boolean canStartSprinting();

    @Shadow
    public abstract boolean canSprint();

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    @Nullable
    public abstract JumpingMount getJumpingMount();

    @Shadow
    protected abstract boolean shouldStopSprinting();

    @Shadow
    public abstract float getMountJumpStrength();

    @Shadow
    protected abstract void startRidingJump();

    @Inject(at = {@At("TAIL")}, method = {"<init>"})
    private void onInit(MinecraftClient minecraftClient, ClientWorld clientWorld, ClientPlayNetworkHandler clientPlayNetworkHandler, StatHandler statHandler, ClientRecipeBook clientRecipeBook, boolean z, boolean z2, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new PlayerInitEvent());
    }

    @Inject(method = {"tick"}, at = {@At("HEAD")}, cancellable = true)
    private void tick(CallbackInfo callbackInfo) {
        PlayerTickEvent class130Var = new PlayerTickEvent(TickStage.PRE);
        Spectra.INSTANCE.eventDispatcher().dispatch(class130Var);
        if (class130Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"tick"}, at = {@At("RETURN")}, cancellable = true)
    private void postTickHook(CallbackInfo callbackInfo) {
        PlayerTickEvent class130Var = new PlayerTickEvent(TickStage.POST);
        Spectra.INSTANCE.eventDispatcher().dispatch(class130Var);
        if (class130Var.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"tickMovement"}, at = {@At("HEAD")}, cancellable = true)
    public void tickMovement(CallbackInfo callbackInfo) {
        if (this.ticksLeftToDoubleTapSprint > 0) {
            this.ticksLeftToDoubleTapSprint--;
        }
        if (!(this.client.currentScreen instanceof DownloadingTerrainScreen)) {
            tickNausea(getCurrentPortalEffect() == Portal.Effect.CONFUSION);
            tickPortalCooldown();
        }
        boolean z = this.input.playerInput.jump() && !((ElytraHelperModule) Spectra.INSTANCE.moduleRepository().get(ElytraHelperModule.class)).canStart();
        boolean zSneak = this.input.playerInput.sneak();
        boolean zMethod_20623 = isWalking();
        PlayerAbilities abilities = getAbilities();
        this.inSneakingPose = (abilities.flying || isSwimming() || hasVehicle() || !canChangeIntoPose(EntityPose.CROUCHING) || (!isSneaking() && (isSleeping() || canChangeIntoPose(EntityPose.STANDING)))) ? false : true;
        this.input.tick();
        this.client.getTutorialManager().onMovement(this.input);
        if (shouldStopSprinting()) {
            setSprinting(false);
        }
        if (isUsingItem() && !hasVehicle()) {
            this.input.movementSideways *= 0.2f;
            this.input.movementForward *= 0.2f;
            this.ticksLeftToDoubleTapSprint = 0;
        }
        if (shouldSlowDown()) {
            float attributeValue = (float) getAttributeValue(EntityAttributes.SNEAKING_SPEED);
            Input input = this.input;
            input.movementSideways *= attributeValue;
            input.movementForward *= attributeValue;
        }
        boolean z2 = false;
        if (this.ticksToNextAutoJump > 0) {
            this.ticksToNextAutoJump--;
            z2 = true;
            this.input.jump();
        }
        if (!this.noClip) {
            pushOutOfBlocks(getX() - (((double) getWidth()) * 0.35d), getZ() + (((double) getWidth()) * 0.35d));
            pushOutOfBlocks(getX() - (((double) getWidth()) * 0.35d), getZ() - (((double) getWidth()) * 0.35d));
            pushOutOfBlocks(getX() + (((double) getWidth()) * 0.35d), getZ() - (((double) getWidth()) * 0.35d));
            pushOutOfBlocks(getX() + (((double) getWidth()) * 0.35d), getZ() + (((double) getWidth()) * 0.35d));
        }
        if (zSneak) {
            this.ticksLeftToDoubleTapSprint = 0;
        }
        MovementUpdateEvent class308Var = new MovementUpdateEvent(DirectionalInput.fromInput(this.input.playerInput), this.client.options.sprintKey.isPressed(), MovementUpdateSource.MOVEMENT_TICK);
        Spectra.INSTANCE.eventDispatcher().dispatch(class308Var);
        boolean zMethod_48300 = canStartSprinting();
        boolean zIsOnGround = hasVehicle() ? getVehicle().isOnGround() : isOnGround();
        boolean z3 = (zSneak || zMethod_20623) ? false : true;
        if ((zIsOnGround || isSubmergedInWater()) && z3 && zMethod_48300) {
            if (this.ticksLeftToDoubleTapSprint > 0 || class308Var.isSprint()) {
                setSprinting(true);
            } else {
                this.ticksLeftToDoubleTapSprint = 7;
            }
        }
        if ((!isTouchingWater() || isSubmergedInWater()) && zMethod_48300 && class308Var.isSprint()) {
            setSprinting(true);
        }
        if (isSprinting()) {
            MovementUpdateEvent class308Var2 = new MovementUpdateEvent(DirectionalInput.fromInput(this.input.playerInput), canSprint(), MovementUpdateSource.MOVEMENT_TICK);
            Spectra.INSTANCE.eventDispatcher().dispatch(class308Var2);
            boolean z4 = (this.input.hasForwardMovement() && class308Var2.isSprint()) ? false : true;
            boolean z5 = z4 || (this.horizontalCollision && !this.collidedSoftly) || (isTouchingWater() && !isSubmergedInWater());
            if (isSwimming()) {
                if ((!isOnGround() && !this.input.playerInput.sneak() && z4) || !isTouchingWater()) {
                    setSprinting(false);
                }
            } else if (z5) {
                setSprinting(false);
            }
        }
        boolean z6 = false;
        if (abilities.allowFlying) {
            if (this.client.interactionManager.isFlyingLocked()) {
                if (!abilities.flying) {
                    abilities.flying = true;
                    z6 = true;
                    sendAbilitiesUpdate();
                }
            } else if (!z && this.input.playerInput.jump() && !z2) {
                if (this.abilityResyncCountdown == 0) {
                    this.abilityResyncCountdown = 7;
                } else if (!isSwimming()) {
                    abilities.flying = !abilities.flying;
                    if (abilities.flying && isOnGround()) {
                        jump();
                    }
                    z6 = true;
                    sendAbilitiesUpdate();
                    this.abilityResyncCountdown = 0;
                }
            }
        }
        if (this.input.playerInput.jump() && !z6 && !z && !isClimbing() && checkGliding()) {
            this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
        this.falling = isGliding();
        if (isTouchingWater() && this.input.playerInput.sneak() && shouldSwimInFluids()) {
            knockDownwards();
        }
        if (isSubmergedIn(FluidTags.WATER)) {
            this.underwaterVisibilityTicks = MathHelper.clamp(this.underwaterVisibilityTicks + (isSpectator() ? 10 : 1), 0, 600);
        } else if (this.underwaterVisibilityTicks > 0) {
            isSubmergedIn(FluidTags.WATER);
            this.underwaterVisibilityTicks = MathHelper.clamp(this.underwaterVisibilityTicks - 10, 0, 600);
        }
        if (abilities.flying && isCamera()) {
            int i = 0;
            if (this.input.playerInput.sneak()) {
                i = 0 - 1;
            }
            if (this.input.playerInput.jump()) {
                i++;
            }
            if (i != 0) {
                setVelocity(getVelocity().add(0.0d, i * abilities.getFlySpeed() * 3.0f, 0.0d));
            }
        }
        JumpingMount jumpingMountMethod_45773 = getJumpingMount();
        if (jumpingMountMethod_45773 == null || jumpingMountMethod_45773.getJumpCooldown() != 0) {
            this.mountJumpStrength = 0.0f;
        } else {
            if (this.field_3938 < 0) {
                this.field_3938++;
                if (this.field_3938 == 0) {
                    this.mountJumpStrength = 0.0f;
                }
            }
            if (z && !this.input.playerInput.jump()) {
                this.field_3938 = -10;
                jumpingMountMethod_45773.setJumpStrength(MathHelper.floor(getMountJumpStrength() * 100.0f));
                startRidingJump();
            } else if (!z && this.input.playerInput.jump()) {
                this.field_3938 = 0;
                this.mountJumpStrength = 0.0f;
            } else if (z) {
                this.field_3938++;
                if (this.field_3938 < 10) {
                    this.mountJumpStrength = this.field_3938 * 0.1f;
                } else {
                    this.mountJumpStrength = 0.8f + ((2.0f / (this.field_3938 - 9)) * 0.1f);
                }
            }
        }
        super.tickMovement();
        if (isOnGround() && abilities.flying && !this.client.interactionManager.isFlyingLocked()) {
            abilities.flying = false;
            sendAbilitiesUpdate();
        }
        callbackInfo.cancel();
    }



}
