package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.math.DirectionalInput;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.model.HwTrackedItem;
import ru.spectra.client.Lang;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.ProjectionUtil;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.event.WorldRenderEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;

@Aliases(aliases = {"HolyWorld Helper", "HW Helper"})
public class HWHelperModule extends Module {



    public final BooleanSetting structureTimerSetting;
    public final BooleanSetting cooldownNotify = new BooleanSetting(
            Lang.FTHELPER_COOLDOWN_NOTIFY, Lang.FTHELPER_COOLDOWN_NOTIFY_DESC);
    private final ru.spectra.client.util.HelperCooldownNotifications cooldownNotifications =
            new ru.spectra.client.util.HelperCooldownNotifications(List.of(
                    Items.POPPED_CHORUS_FRUIT, Items.PRISMARINE_SHARD,
                    Items.NETHER_STAR, Items.JACK_O_LANTERN, Items.FIREWORK_STAR,
                    Items.FIRE_CHARGE, Items.WIND_CHARGE, Items.CROSSBOW));
    private net.minecraft.client.network.ClientPlayNetworkHandler observedConnection;

    public final BooleanSetting forceStopSetting;

    public final Mc mc;
    public final Stopwatch teleportTimer;
    public final List<HwTrackedItem> trackedItems;
    public final Pattern teleportPattern;
    public int teleportSeconds;

    public HWHelperModule() {
        super(ModuleTab.MISC, "HW Helper");
        this.structureTimerSetting = new BooleanSetting(Lang.FTHELPER_STRUCTURE_TIMER, Lang.FTHELPER_STRUCTURE_TIMER_DESC);
        this.forceStopSetting = new BooleanSetting(Lang.HWHELPER_FORCE_STOP);
        this.mc = Mc.INSTANCE;
        this.teleportTimer = new Stopwatch();
        this.trackedItems = new ArrayList();
        this.teleportPattern = Pattern.compile("через\\s+(\\d+)\\s+секунд.*не\\s+двигайтесь");
        this.teleportSeconds = 0;
        addSettings(this.forceStopSetting, this.structureTimerSetting, this.cooldownNotify);
        register(WorldRenderEvent.class, class016Var -> {
            if (isState() && this.structureTimerSetting.isValue() && Mc.INSTANCE.isWorldLoaded()) {
                this.trackedItems.forEach(class500Var -> {
                    if (class500Var.boxSize != 0.0f && class500Var.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(class500Var.world)) {
                        ShapeRenderer.INSTANCE.addBox(class016Var.matrixStack().peek().getPositionMatrix(), Box.from(class500Var.vec.add(-0.5d)).expand(class500Var.boxSize), Spectra.INSTANCE.drawEngine().colorStack().computeColor(Spectra.INSTANCE.theme().palette().accent().argb(), 0.5f));
                    }
                });
            }
        });
        register(Render2DEvent.class, class311Var -> {
            if (isState() && this.structureTimerSetting.isValue()
                    && Mc.INSTANCE.isWorldLoaded() && class311Var.isPre()) {
                MatrixStack matrixStack = class311Var.matrixStack();
                Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
                DrawEngine class154VarDrawEngine = Spectra.INSTANCE.drawEngine();
                ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
                ThemePalette class764VarPalette = Spectra.INSTANCE.theme().palette();
                class154VarDrawEngine.begin();
                class115VarColorStack.push();
                this.trackedItems.forEach(class500Var -> {
                    double dCurrentTimeMillis = (class500Var.time - System.currentTimeMillis()) / 1000.0d;
                    Optional<Vector2f> optionalWorldToScreen = ProjectionUtil.worldToScreen(class500Var.vec);
                    if (optionalWorldToScreen.isPresent()) {
                        Vector2f vector2f = optionalWorldToScreen.get();
                        String str = MathUtil.round(dCurrentTimeMillis, 0.10000000149011612d) + "с";
                        float width = Fonts.INTER_BOLD.get().getWidth(str, 14.0f);
                        float height = Fonts.INTER_BOLD.get().getHeight(14.0f);
                        int iComputeColor = class115VarColorStack.computeColor(class764VarPalette.text().tone(400).argb());
                        float f = 6.0f + 16.0f + 4.0f + width + 6.0f + 2.0f;
                        float fMax = Math.max(16.0f, height) + (6.0f * 2.0f);
                        float f2 = vector2f.x - (f / 2.0f);
                        float f3 = vector2f.y - (fMax / 2.0f);
                        if (class500Var.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(class500Var.world)) {
                            class154VarDrawEngine.roundedRectangle(positionMatrix, f2, f3, f, fMax, 7.0f, 2.5f, class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(700).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(801).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(801).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(900).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(900).argb()));
                            float f4 = f2 + 6.0f;
                            class154VarDrawEngine.itemStack(matrixStack.peek().getPositionMatrix(), class500Var.item.getDefaultStack(), f4, f3 + ((fMax - 16.0f) / 2.0f), 0.5f, 1.0f);
                            class154VarDrawEngine.msdfFont(matrixStack.peek().getPositionMatrix(), Fonts.INTER_BOLD.get(), str, f4 + 16.0f + 4.0f, f3 + ((fMax - height) / 2.0f), 14.0f, 0.0f, iComputeColor);
                        }
                    }
                });
                class115VarColorStack.pop();
                class154VarDrawEngine.end();
            }
        });
        register(PacketReceiveEvent.class, class051Var -> {
            if (isState() && this.mc.isWorldLoaded()) {
                net.minecraft.network.packet.Packet<?> packet = class051Var.getPacket();
                Objects.requireNonNull(packet);
                if (packet instanceof GameMessageS2CPacket) {
                    String lowerCase = (String) (((GameMessageS2CPacket) packet).content().getString().toLowerCase());
                    Matcher matcher = this.teleportPattern.matcher(lowerCase);
                    if (matcher.find()) {
                        this.teleportSeconds = Integer.parseInt(matcher.group(1));
                        this.teleportTimer.reset();
                    }
                    if (lowerCase.equalsIgnoreCase("Телепортирование начинается...") || lowerCase.equalsIgnoreCase("Запрос на телепортацию отменен.")) {
                        this.teleportSeconds = 0;
                    }
                } else if (packet instanceof PlaySoundS2CPacket) {
                    PlaySoundS2CPacket playSoundS2CPacket = (PlaySoundS2CPacket) packet;
                    if (playSoundS2CPacket.getCategory().equals(SoundCategory.MASTER) && this.structureTimerSetting.isValue()) {
                        Vec3d centerPos = BlockPos.ofFloored(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ()).toCenterPos();
                        switch (playSoundS2CPacket.getSound().getIdAsString().replace("minecraft:", "")) {
                            case "block.beacon.deactivate":
                                trackItem(Items.NETHER_STAR, centerPos, 15000L, 14.5f);
                                break;
                            case "entity.generic.explode":
                                trackItem(Items.PRISMARINE_SHARD, centerPos.add(0.0d, -1.0d, 0.0d), 11000L, 0.0f);
                                break;
                        }
                    }
                }
            }
        });
        register(MovementInputEvent.class, class040Var -> {
            if (isState() && this.mc.isWorldLoaded()) {
                if (shouldBlockMovement()) {
                    class040Var.setInput(DirectionalInput.NONE);
                    class040Var.setJumping(false);
                    class040Var.setSprinting(false);
                }
                if (this.teleportSeconds <= 0 || !this.teleportTimer.hasElapsed(this.teleportSeconds + 1, TimeUnit.SECONDS)) {
                    return;
                }
                this.teleportSeconds = 0;
            }
        });
        register(PlayerTickEvent.class, class130Var -> {
            if (!class130Var.isPost() && isState() && this.mc.isWorldLoaded()) {
                if (this.observedConnection != this.mc.getNetworkHandler()) {
                    this.observedConnection = this.mc.getNetworkHandler();
                    this.trackedItems.clear();
                    this.teleportSeconds = 0;
                    this.cooldownNotifications.clear();
                }
                this.cooldownNotifications.update(this.mc.getPlayer(), this.cooldownNotify.isValue());
                this.trackedItems.removeIf(class500Var -> {
                    return class500Var.time - ((double) System.currentTimeMillis()) <= 0.0d;
                });
            }
        });
    }

    public boolean shouldBlockMovement() {
        return this.forceStopSetting.isValue() && this.teleportSeconds > 0 && !this.teleportTimer.hasElapsed((long) (this.teleportSeconds + 1), TimeUnit.SECONDS);
    }

    @Override
    public void deactivate() {
        this.trackedItems.clear();
        this.teleportSeconds = 0;
        this.cooldownNotifications.clear();
        super.deactivate();
    }

    public List<HwTrackedItem> getStructures() {
        return Collections.unmodifiableList(this.trackedItems);
    }

    public void trackItem(Item item, Vec3d vec3d, long j, float f) {
        if (this.trackedItems.stream().noneMatch(class500Var -> {
            return class500Var.vec().equals(vec3d);
        })) {
            this.trackedItems.add(new HwTrackedItem(item, vec3d, ServerUtil.getWorldType(), ServerUtil.getAnarchy(), System.currentTimeMillis() + j, f));
        }
    }
}
