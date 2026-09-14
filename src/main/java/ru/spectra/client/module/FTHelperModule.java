package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.util.BlockUtil;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.type.ChatMessageType;
import ru.spectra.client.event.ChatReceiveEvent;
import ru.spectra.client.util.ChatUtil;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.event.Event;
import ru.spectra.client.type.EventPriority;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.model.FtTrackedBoss;
import ru.spectra.client.model.FtTrackedStructure;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.Lang;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.PacketSendEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.ProjectionUtil;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.ui.WorldMarkerRenderer;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.util.WavSoundPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;

@Aliases(aliases = {"Funtime Helper", "FT Helper"})
public class FTHelperModule extends Module {
    private static final Set<Integer> RESTRICTED_ANARCHIES =
            Set.of(106, 107, 212, 213, 214, 308, 309);
    private static final long RESTRICTED_WARNING_VISIBLE_MS = 5000L;
    private static final long RESTRICTED_WARNING_FADE_MS = 420L;

    public Map<BlockPos, BlockState> blockUpdates;

    public final BooleanSetting autoPoint;
    public final BooleanSetting structureTimer;
    public final BooleanSetting cooldownNotify;
    public final BooleanSetting restrictedAnarchyWarning;

    private final ru.spectra.client.util.HelperCooldownNotifications cooldownNotifications =
            new ru.spectra.client.util.HelperCooldownNotifications(List.of(
                    Items.ENDER_EYE, Items.NETHERITE_SCRAP, Items.SLIME_BALL,
                    Items.GUNPOWDER, Items.BLAZE_POWDER, Items.GLOWSTONE_DUST,
                    Items.SNOWBALL, Items.WIND_CHARGE, Items.ENDER_PEARL, Items.CHORUS_FRUIT));

    public final Mc mc;
    public final List<FtTrackedBoss> trackedBosses;
    public final List<FtTrackedStructure> structures;
    public final Map<String, GlTexture> iconCache;
    public final Map<String, String> iconPaths;
    public final List<Runnable> pendingTasks;
    private final List<RecentStructureSound> recentStructureSounds;
    private PendingPlastPlacement pendingPlastPlacement;
    private ClientPlayNetworkHandler warningConnection;
    private int lastObservedAnarchy = Integer.MIN_VALUE;
    private int activeWarningAnarchy = -1;
    private long restrictedWarningStartedAt = -1L;

    public FTHelperModule() {
        super(ModuleTab.MISC, "FT Helper");
        this.blockUpdates = new HashMap();
        this.autoPoint = new BooleanSetting(Lang.FTHELPER_AUTO_POINT, Lang.FTHELPER_AUTO_POINT_DESC);
        this.structureTimer = new BooleanSetting(Lang.FTHELPER_STRUCTURE_TIMER, Lang.FTHELPER_STRUCTURE_TIMER_DESC);
        this.cooldownNotify = new BooleanSetting(Lang.FTHELPER_COOLDOWN_NOTIFY, Lang.FTHELPER_COOLDOWN_NOTIFY_DESC);
        this.restrictedAnarchyWarning = new BooleanSetting(
                Lang.FTHELPER_RESTRICTED_ANARCHY_WARNING,
                Lang.FTHELPER_RESTRICTED_ANARCHY_WARNING_DESC
        ).setValue(true);
        this.mc = Mc.INSTANCE;
        this.trackedBosses = new ArrayList();
        this.structures = new ArrayList();
        this.iconCache = new HashMap();
        this.iconPaths = Map.ofEntries(Map.entry("Мистический сундук", "/icons/fthelper/mystic.png"), Map.entry("Вулкан", "/icons/fthelper/volcano.png"), Map.entry("Метеоритный дождь", "/icons/fthelper/shower.png"), Map.entry("Маяк убийца", "/icons/fthelper/lighthouse.png"), Map.entry("Мистический Алтарь", "/icons/fthelper/altar.png"), Map.entry("Загадочный маяк", "/icons/fthelper/lighthouse.png"), Map.entry("Сундук смерти", "/icons/fthelper/deadchest.png"), Map.entry("Адская резня", "/icons/fthelper/infernal.png"));
        this.pendingTasks = new ArrayList();
        this.recentStructureSounds = new ArrayList<>();
        addSettings(this.autoPoint, this.structureTimer,
                this.cooldownNotify, this.restrictedAnarchyWarning);
        register(Render2DEvent.class, class311Var -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && class311Var.isPre()) {
                MatrixStack matrixStack = class311Var.matrixStack();
                Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
                DrawEngine class154VarDrawEngine = Spectra.INSTANCE.drawEngine();
                ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
                ThemePalette class764VarPalette = Spectra.INSTANCE.theme().palette();
                class154VarDrawEngine.begin();
                class115VarColorStack.push();
                renderRestrictedAnarchyWarning(matrixStack, class154VarDrawEngine, class115VarColorStack);
                Collections.<FtTrackedStructure>emptyList().forEach(class493Var -> {
                    double dCurrentTimeMillis = (class493Var.time - System.currentTimeMillis()) / 1000.0d;
                    Optional<Vector2f> optionalWorldToScreen = ProjectionUtil.worldToScreen(class493Var.vec);
                    if (optionalWorldToScreen.isPresent()) {
                        Vector2f vector2f = optionalWorldToScreen.get();
                        String str = MathUtil.round(dCurrentTimeMillis, 0.10000000149011612d) + "с";
                        float width = Fonts.INTER_BOLD.get().getWidth(str, 14.0f);
                        float height = Fonts.INTER_BOLD.get().getHeight(14.0f);
                        float f = 6.0f + 16.0f + 4.0f + width + 6.0f + 2.0f;
                        float fMax = Math.max(16.0f, height) + (6.0f * 2.0f);
                        float f2 = vector2f.x - (f / 2.0f);
                        float f3 = vector2f.y - (fMax / 2.0f);
                        if (class493Var.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(class493Var.world)) {
                            class154VarDrawEngine.roundedRectangle(positionMatrix, f2, f3, f, fMax, 7.0f, 2.5f, class115VarColorStack.computeColor(class764VarPalette.surfaceOutline().tone(700).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(801).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(801).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(900).argb()), class115VarColorStack.computeColor(class764VarPalette.surfaceBackground().tone(900).argb()));
                            float f4 = f2 + 6.0f;
                            class154VarDrawEngine.itemStack(matrixStack.peek().getPositionMatrix(), class493Var.item.getDefaultStack(), f4, f3 + ((fMax - 16.0f) / 2.0f), 0.5f, 1.0f);
                            class154VarDrawEngine.msdfFont(matrixStack.peek().getPositionMatrix(), Fonts.INTER_BOLD.get(), str, f4 + 16.0f + 4.0f, f3 + ((fMax - height) / 2.0f), 14.0f, 0.0f, class115VarColorStack.computeColor(class764VarPalette.text().tone(400).argb()));
                        }
                    }
                });
                this.trackedBosses.forEach(class496Var -> {
                    String strReplace;
                    Optional<Vector2f> optionalWorldToScreen = ProjectionUtil.worldToScreen(class496Var.vec);
                    if (optionalWorldToScreen.isPresent()) {
                        Vector2f vector2f = optionalWorldToScreen.get();
                        double dCurrentTimeMillis = (class496Var.timeOpen - System.currentTimeMillis()) / 1000.0d;
                        double dCurrentTimeMillis2 = (class496Var.timeEnd - System.currentTimeMillis()) / 1000.0d;
                        if (dCurrentTimeMillis > 0.0d) {
                            strReplace = ("До начала: " + MathUtil.round(dCurrentTimeMillis, dCurrentTimeMillis < 30.0d ? 0.10000000149011612d : 1.0d) + "с").replace(".0", "");
                        } else if (dCurrentTimeMillis2 > 0.0d) {
                            strReplace = ("До конца: " + MathUtil.round(dCurrentTimeMillis2, dCurrentTimeMillis2 < 30.0d ? 0.10000000149011612d : 1.0d) + "с").replace(".0", "");
                        } else {
                            strReplace = "Конец ивента!";
                        }
                        String str = strReplace;
                        if (class496Var.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(class496Var.world)) {
                            String iconPath = this.iconPaths.get(class496Var.name);
                            GlTexture icon = iconPath == null ? null : this.iconCache.computeIfAbsent(
                                    iconPath, path -> new GlTexture(new ClasspathResource(path))
                            );
                            WorldMarkerRenderer.renderEvent(
                                    class154VarDrawEngine, matrixStack, vector2f,
                                    class496Var.name, class496Var.vec, icon, str
                            );
                        }
                    }
                });
                class115VarColorStack.pop();
                class154VarDrawEngine.end();
            }
        });
        register(ChatReceiveEvent.class, class066Var -> {
            String strSubstringBetween;
            if (isState() && Mc.INSTANCE.isWorldLoaded() && class066Var.type() == ChatMessageType.GAME_MESSAGE && class066Var.message().contains("Координаты:") && (strSubstringBetween = StringUtils.substringBetween(class066Var.textData().getString(), "Координаты: [", "]")) != null) {
                class066Var.cancel();
                ChatUtil.addMessage(Mc.INSTANCE.getInGameHud().getChatHud(), class066Var.textData().copy().append(Text.literal(" ")).append(Text.literal("[Поставить WayPoint]").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ".way add Event " + strSubstringBetween)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите чтобы добавить вейпоинт"))))), null);
            }
        }, EventPriority.HIGHEST);
        register(PlayerTickEvent.class, class130Var -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && class130Var.isPre()) {
                updateRestrictedAnarchyWarning();
                if (!this.pendingTasks.isEmpty()) {
                    ArrayList<Runnable> arrayList = new ArrayList<Runnable>(this.pendingTasks);
                    this.pendingTasks.clear();
                    arrayList.forEach((v0) -> {
                        v0.run();
                    });
                }
                analyzePlastBlockUpdates();
                updatePendingPlastPlacement();
                this.blockUpdates.clear();
                this.structures.removeIf(class493Var -> {
                    return class493Var.time - ((double) System.currentTimeMillis()) <= 0.0d;
                });
                this.trackedBosses.removeIf(class496Var -> {
                    return (class496Var.timeEnd + 90000.0d) - ((double) System.currentTimeMillis()) <= 0.0d;
                });
                this.cooldownNotifications.update(this.mc.getPlayer(), this.cooldownNotify.isValue());
            }
        });
        register(PacketSendEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                handlePlastInteraction(event.getPacket());
            }
        });
        register(PacketReceiveEvent.class, class051Var -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                net.minecraft.network.packet.Packet<?> packet = class051Var.getPacket();
                Objects.requireNonNull(packet);
                if (packet instanceof PlaySoundS2CPacket soundPacket) {
                    if (this.structureTimer.isValue()) {
                        rememberStructureSound(soundPacket);
                        handlePendingPlastSound(soundPacket);
                        handleStructureSound(soundPacket);
                    }
                } else if (packet instanceof ChunkDeltaUpdateS2CPacket) {
                    ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket = (ChunkDeltaUpdateS2CPacket) packet;
                    if (this.structureTimer.isValue()) {
                        chunkDeltaUpdateS2CPacket.visitUpdates((blockPos, blockState) -> {
                            this.blockUpdates.put(blockPos.toImmutable(), blockState);
                        });
                        this.pendingTasks.add(() -> {
                            chunkDeltaUpdateS2CPacket.visitUpdates((blockPos2, blockState2) -> {
                                Vec3d centerPos = blockPos2.toImmutable().toCenterPos();
                                if (this.blockUpdates.size() <= 50 || this.blockUpdates.size() >= 600) {
                                    return;
                                }
                                if (isSmallStructure(blockPos2.up(2))) {
                                    addTrackedStructure(Items.NETHERITE_SCRAP, centerPos, System.currentTimeMillis() + 15000);
                                } else if (isLargeStructure(blockPos2.up(3))) {
                                    addTrackedStructure(Items.NETHERITE_SCRAP, centerPos, System.currentTimeMillis() + 30000);
                                }
                            });
                        });
                    }
                } else if (packet instanceof GameMessageS2CPacket) {
                    GameMessageS2CPacket gameMessageS2CPacket = (GameMessageS2CPacket) packet;
                    if (this.autoPoint.isValue()) {
                        Text textContent = gameMessageS2CPacket.content();
                        String string = textContent.toString();
                        String string2 = textContent.getString();
                        String strSubstringBetween = StringUtils.substringBetween(string2, "|||   [", "]   ");
                        if (strSubstringBetween != null) {
                            String strSubstringBetween2 = StringUtils.substringBetween(string, "value='/gps ", "'");
                            String strSubstringBetween3 = StringUtils.substringBetween(string2, "Уровень лута: ", "\n ║");
                            String strSubstringBetween4 = StringUtils.substringBetween(string2, "Призван игроком: ", "\n ║");
                            if (strSubstringBetween2 == null) {
                                switch (strSubstringBetween) {
                                    case "Сундук смерти":
                                        addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, BlockPos.ofFloored(-155.0d, 64.0d, 205.0d).toCenterPos(), "lobby", 300, 0);
                                        break;
                                    case "Адская резня":
                                        addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, BlockPos.ofFloored(48.0d, 87.0d, 73.0d).toCenterPos(), "lobby", 180, 120);
                                        break;
                                }
                                return;
                            }
                            String[] strArrSplit = strSubstringBetween2.split(" ");
                            Vec3d centerPos = BlockPos.ofFloored(Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1]), Integer.parseInt(strArrSplit[2])).toCenterPos();
                            switch (strSubstringBetween) {
                                case "Мистический сундук":
                                    addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, centerPos, "overworld", 300, 0);
                                    break;
                                case "Вулкан":
                                    addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, centerPos, "overworld", 300, 120);
                                    break;
                                case "Метеоритный дождь":
                                case "Маяк убийца":
                                case "Мистический Алтарь":
                                    addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, centerPos, "overworld", 360, 0);
                                    break;
                                case "Загадочный маяк":
                                    addTrackedBoss(strSubstringBetween, strSubstringBetween3, strSubstringBetween4, centerPos, "overworld", 60, 180);
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    private void updateRestrictedAnarchyWarning() {
        ClientPlayNetworkHandler currentConnection = this.mc.getNetworkHandler();
        if (currentConnection != this.warningConnection) {
            this.structures.clear();
            this.trackedBosses.clear();
            this.blockUpdates.clear();
            this.pendingTasks.clear();
            this.recentStructureSounds.clear();
            this.pendingPlastPlacement = null;
            this.cooldownNotifications.clear();
            this.warningConnection = currentConnection;
            this.lastObservedAnarchy = Integer.MIN_VALUE;
            this.activeWarningAnarchy = -1;
            this.restrictedWarningStartedAt = -1L;
        }
        if (currentConnection == null) {
            this.lastObservedAnarchy = Integer.MIN_VALUE;
            this.activeWarningAnarchy = -1;
            this.restrictedWarningStartedAt = -1L;
            return;
        }
        if (!"FunTime".equalsIgnoreCase(ServerUtil.server)) {
            this.lastObservedAnarchy = Integer.MIN_VALUE;
            this.activeWarningAnarchy = -1;
            this.restrictedWarningStartedAt = -1L;
            return;
        }
        int anarchy = ServerUtil.getAnarchy();
        if (anarchy <= 0) {
            this.lastObservedAnarchy = 0;
            this.activeWarningAnarchy = -1;
            this.restrictedWarningStartedAt = -1L;
            return;
        }
        if (anarchy == this.lastObservedAnarchy) {
            if (!this.restrictedAnarchyWarning.isValue()) {
                this.activeWarningAnarchy = -1;
                this.restrictedWarningStartedAt = -1L;
            }
            return;
        }
        this.lastObservedAnarchy = anarchy;
        this.activeWarningAnarchy = -1;
        this.restrictedWarningStartedAt = -1L;
        if (this.restrictedAnarchyWarning.isValue() && RESTRICTED_ANARCHIES.contains(anarchy)) {
            this.activeWarningAnarchy = anarchy;
            this.restrictedWarningStartedAt = System.currentTimeMillis();
        }
    }

    private void renderRestrictedAnarchyWarning(MatrixStack matrices, DrawEngine draw,
                                                ColorStack colors) {
        if (!this.restrictedAnarchyWarning.isValue()
                || this.activeWarningAnarchy <= 0
                || this.restrictedWarningStartedAt < 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.restrictedWarningStartedAt;
        long totalDuration = RESTRICTED_WARNING_VISIBLE_MS + RESTRICTED_WARNING_FADE_MS;
        if (elapsed >= totalDuration) {
            this.activeWarningAnarchy = -1;
            this.restrictedWarningStartedAt = -1L;
            return;
        }
        float alpha = elapsed <= RESTRICTED_WARNING_VISIBLE_MS
                ? 1.0f
                : 1.0f - (elapsed - RESTRICTED_WARNING_VISIBLE_MS)
                / (float) RESTRICTED_WARNING_FADE_MS;

        String title = Lang.FTHELPER_RESTRICTED_ANARCHY_TITLE.effective();
        String line1 = Lang.FTHELPER_RESTRICTED_ANARCHY_LINE_1.effective()
                .replace("{anarchy}", Integer.toString(this.activeWarningAnarchy));
        String line2 = Lang.FTHELPER_RESTRICTED_ANARCHY_LINE_2.effective();
        MsdfFont titleFont = Fonts.INTER_BOLD.get();
        MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
        float titleSize = 14.0f;
        float descriptionSize = 11.0f;
        float textWidth = Math.max(
                titleFont.getWidth(title, titleSize),
                Math.max(descriptionFont.getWidth(line1, descriptionSize),
                        descriptionFont.getWidth(line2, descriptionSize))
        );
        float iconSize = 22.0f;
        float panelWidth = Math.max(430.0f, 18.0f + iconSize + 13.0f + textWidth + 20.0f);
        float panelHeight = 72.0f;
        ScreenResolution resolution = ScreenResolution.resolution();
        panelWidth = Math.min(panelWidth, resolution.screenWidth() - 24.0f);
        float panelX = (resolution.screenWidth() - panelWidth) / 2.0f;
        float panelY = Math.max(18.0f,
                (resolution.screenHeight() - panelHeight) / 2.0f - 120.0f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        draw.roundedRectangle(matrix, panelX, panelY, panelWidth, panelHeight, 10.0f,
                colors.computeColor(0xA66D72, 0.50f * alpha));
        draw.roundedRectangle(matrix, panelX + 1.0f, panelY + 1.0f,
                panelWidth - 2.0f, panelHeight - 2.0f, 9.0f,
                colors.computeColor(0x4B252A, 0.76f * alpha));

        float iconX = panelX + 17.0f;
        float iconY = panelY + (panelHeight - iconSize) / 2.0f;
        GlTexture warningIcon = NotificationType.WARNING.getTexture();
        draw.texture(matrix, iconX, iconY, iconSize, iconSize,
                draw.bindTexture(warningIcon.textureWithSTB()),
                colors.computeColor(0xFFD166, alpha));

        float textX = iconX + iconSize + 13.0f;
        draw.msdfFont(matrix, titleFont, title, textX, panelY + 12.0f,
                titleSize, 0.04f, colors.computeColor(0xFFF4F4, alpha));
        draw.msdfFont(matrix, descriptionFont, line1, textX, panelY + 36.0f,
                descriptionSize, 0.04f, colors.computeColor(0xE9D2D4, 0.94f * alpha));
        draw.msdfFont(matrix, descriptionFont, line2, textX, panelY + 51.0f,
                descriptionSize, 0.04f, colors.computeColor(0xD7B9BC, 0.86f * alpha));
    }

    public void drawTooltip(MatrixStack matrixStack, DrawEngine class154Var, List<String> list, Vector2f vector2f) {
        ColorStack class115VarColorStack = class154Var.colorStack();
        float f = 0.0f;
        for (String str : list) {
            MsdfFont class161Var = Fonts.INTER_MEDIUM.get();
            class154Var.msdfFont(matrixStack.peek().getPositionMatrix(), class161Var, str, (int) (vector2f.x - (class161Var.getWidth(str, 10.0f) / 2.0f)), (int) (vector2f.y + 17.0f + f), 10.0f, 0.0f, class115VarColorStack.computeColor(13948641));
            f += 10.0f;
        }
    }

    public void addTrackedBoss(String str, String str2, String str3, Vec3d vec3d, String str4, int i, int i2) {
        if (this.trackedBosses.stream().noneMatch(class496Var -> {
            return class496Var.vec.equals(vec3d);
        })) {
            long jCurrentTimeMillis = System.currentTimeMillis() + (((long) i) * 1000);
            this.trackedBosses.add(new FtTrackedBoss(str, str2, str3, vec3d, str4, ServerUtil.getAnarchy(), jCurrentTimeMillis, jCurrentTimeMillis + (((long) i2) * 1000)));
            Spectra.INSTANCE.notificationRepository().post(NotificationType.EVENT, (Text) Text.literal("Начался ивент: " + str), 10L, TimeUnit.SECONDS);
            WavSoundPlayer.INSTANCE.playSound("apple_pay", 80.0f, false);
        }
    }

    public void addTrackedStructure(Item item, Vec3d vec3d, double d) {
        if (this.structures.stream().noneMatch(class493Var -> {
            return class493Var.item == item
                    && class493Var.vec.squaredDistanceTo(vec3d) <= 1.0d
                    && class493Var.time > System.currentTimeMillis();
        })) {
            this.structures.add(new FtTrackedStructure(item, vec3d, ServerUtil.getWorldType(), ServerUtil.getAnarchy(), d));
        }
    }

    private void handlePlastInteraction(Packet<?> packet) {
        if (!this.structureTimer.isValue() || this.mc.getPlayer() == null) {
            return;
        }
        Hand hand;
        float pitch = this.mc.getPlayer().getPitch();
        if (packet instanceof PlayerInteractItemC2SPacket interactItem) {
            hand = interactItem.getHand();
            pitch = interactItem.getPitch();
        } else if (packet instanceof PlayerInteractBlockC2SPacket interactBlock) {
            hand = interactBlock.getHand();
        } else {
            return;
        }
        ItemStack stack = this.mc.getPlayer().getStackInHand(hand);
        if (!isPlastStack(stack)) {
            return;
        }
        this.pendingPlastPlacement = new PendingPlastPlacement(
                this.mc.getPlayer().getPos(), pitch, System.currentTimeMillis());
    }

    private void rememberStructureSound(PlaySoundS2CPacket packet) {
        if (!"FunTime".equalsIgnoreCase(ServerUtil.server)
                || packet.getCategory()
                != net.minecraft.sound.SoundCategory.RECORDS) {
            return;
        }
        long now = System.currentTimeMillis();
        this.recentStructureSounds.removeIf(sound ->
                now - sound.timestamp > 1_500L);
        this.recentStructureSounds.add(new RecentStructureSound(
                new Vec3d(packet.getX(), packet.getY(), packet.getZ()), now));
    }

    private boolean hasRecentStructureSound(Vec3d position, long now) {
        this.recentStructureSounds.removeIf(sound ->
                now - sound.timestamp > 1_500L);
        return this.recentStructureSounds.stream().anyMatch(sound ->
                position.squaredDistanceTo(sound.position) <= 144.0d);
    }

    private void handlePendingPlastSound(PlaySoundS2CPacket packet) {
        PendingPlastPlacement pending = this.pendingPlastPlacement;
        if (pending == null || pending.resolved
                || System.currentTimeMillis() - pending.startedAt > 1_400L
                || packet.getCategory() != net.minecraft.sound.SoundCategory.RECORDS) {
            return;
        }
        Vec3d soundPosition =
                new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        if (soundPosition.squaredDistanceTo(pending.playerPosition) <= 144.0d) {
            pending.soundPosition = soundPosition;
        }
    }

    private void updatePendingPlastPlacement() {
        long now = System.currentTimeMillis();
        this.recentStructureSounds.removeIf(sound ->
                now - sound.timestamp > 1_500L);
        PendingPlastPlacement pending = this.pendingPlastPlacement;
        if (pending == null) {
            return;
        }
        long age = now - pending.startedAt;
        if (!pending.resolved && age >= 600L && pending.soundPosition != null) {
            long duration = Math.abs(pending.pitch) >= 60.0f
                    ? 60_000L : 20_000L;
            addTrackedStructure(Items.DRIED_KELP, pending.soundPosition,
                    System.currentTimeMillis() + duration);
            pending.resolved = true;
        }
        if (age > 1_500L) {
            this.pendingPlastPlacement = null;
        }
    }

    private void analyzePlastBlockUpdates() {
        if (!this.structureTimer.isValue()
                || !"FunTime".equalsIgnoreCase(ServerUtil.server)
                || this.blockUpdates.isEmpty()) {
            return;
        }
        Set<BlockPos> remaining = new HashSet<>();
        this.blockUpdates.forEach((position, state) -> {
            if (state != null && !state.isAir()) {
                remaining.add(position.toImmutable());
            }
        });
        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            remaining.remove(seed);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            List<BlockPos> component = new ArrayList<>();
            queue.add(seed);
            while (!queue.isEmpty()) {
                BlockPos position = queue.removeFirst();
                component.add(position);
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) {
                                continue;
                            }
                            BlockPos neighbor = position.add(x, y, z);
                            if (remaining.remove(neighbor)) {
                                queue.addLast(neighbor);
                            }
                        }
                    }
                }
            }
            PlastGeometry geometry = classifyPlastGeometry(component);
            if (geometry == null) {
                continue;
            }
            long now = System.currentTimeMillis();
            if (!hasRecentStructureSound(geometry.center, now)) {
                continue;
            }
            PendingPlastPlacement pending = this.pendingPlastPlacement;
            if (pending != null
                    && now - pending.startedAt <= 1_500L
                    && geometry.center.squaredDistanceTo(
                    pending.soundPosition == null
                            ? pending.playerPosition : pending.soundPosition) <= 144.0d) {
                pending.resolved = true;
            }
            long duration = geometry.horizontal ? 60_000L : 20_000L;
            addTrackedStructure(Items.DRIED_KELP, geometry.center,
                    now + duration);
        }
    }

    private static PlastGeometry classifyPlastGeometry(
            List<BlockPos> component) {
        int count = component.size();
        if (count < 40 || count > 110) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos position : component) {
            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
            minZ = Math.min(minZ, position.getZ());
            maxX = Math.max(maxX, position.getX());
            maxY = Math.max(maxY, position.getY());
            maxZ = Math.max(maxZ, position.getZ());
        }
        int xExtent = maxX - minX + 1;
        int yExtent = maxY - minY + 1;
        int zExtent = maxZ - minZ + 1;
        int[] extents = {xExtent, yExtent, zExtent};
        Arrays.sort(extents);

        boolean small = count <= 60
                && extents[2] == 5
                && extents[1] >= 4
                && (extents[0] <= 2 || extents[0] >= 4);
        boolean large = count >= 70
                && extents[2] == 7
                && extents[1] >= 6
                && (extents[0] <= 2 || extents[0] >= 6);
        if (!small && !large) {
            return null;
        }
        Vec3d center = new Vec3d(
                (minX + maxX + 1.0d) / 2.0d,
                (minY + maxY + 1.0d) / 2.0d,
                (minZ + maxZ + 1.0d) / 2.0d
        );
        return new PlastGeometry(center, yExtent <= 2);
    }

    private static boolean isPlastStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || (!stack.isOf(Items.DRIED_KELP)
                && !stack.isOf(Items.SLIME_BALL))) {
            return false;
        }
        return stack.getName().getString().toLowerCase(Locale.ROOT)
                .contains("\u043f\u043b\u0430\u0441\u0442");
    }

    private void handleStructureSound(PlaySoundS2CPacket packet) {
        if (!"FunTime".equalsIgnoreCase(ServerUtil.server)) {
            return;
        }
        String soundPath = packet.getSound().value().id().getPath()
                .toLowerCase(Locale.ROOT);
        float pitch = packet.getPitch();
        float volume = packet.getVolume();
        Vec3d position = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        long now = System.currentTimeMillis();
        if (soundPath.contains("block.piston")
                && closeTo(pitch, 0.5f)
                && (closeTo(volume, 0.7f) || closeTo(volume, 0.5f))) {
            addTrackedStructure(Items.NETHERITE_SCRAP, position, now + 20_000L);
        }
    }

    private static boolean closeTo(float value, float expected) {
        return Math.abs(value - expected) <= 0.011f;
    }

    public boolean isSmallStructure(BlockPos blockPos) {
        BlockState blockState;
        int i = 0;
        for (BlockPos blockPos2 : BlockUtil.getCube(blockPos, 2.0f)) {
            if (blockPos2.toCenterPos().distanceTo(blockPos.toCenterPos()) < 2.0d) {
                BlockState blockState2 = this.blockUpdates.get(blockPos2);
                if (blockState2 != null && !blockState2.isAir()) {
                    i++;
                }
            } else if (!blockPos2.equals(blockPos.up(2).north().east()) && !blockPos2.equals(blockPos.up(2).north().west()) && !blockPos2.equals(blockPos.up(2).south().east()) && !blockPos2.equals(blockPos.up(2).south().west()) && ((blockState = this.blockUpdates.get(blockPos2)) == null || blockState.isAir())) {
                i++;
            }
            if (i > 5) {
                return false;
            }
        }
        return true;
    }

    public boolean isLargeStructure(BlockPos blockPos) {
        BlockState blockState;
        int i = 0;
        for (BlockPos blockPos2 : BlockUtil.getCube(blockPos, 3.0f)) {
            if (Math.abs(blockPos2.getX() - blockPos.getX()) <= 2 && Math.abs(blockPos2.getY() - blockPos.getY()) <= 2 && Math.abs(blockPos2.getZ() - blockPos.getZ()) <= 2) {
                BlockState blockState2 = this.blockUpdates.get(blockPos2);
                if (blockState2 != null && !blockState2.isAir()) {
                    i++;
                }
            } else if (!blockPos2.equals(blockPos.up(3)) && ((blockState = this.blockUpdates.get(blockPos2)) == null || blockState.isAir())) {
                i++;
            }
            if (i > 5) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void deactivate() {
        this.pendingPlastPlacement = null;
        this.recentStructureSounds.clear();
        this.cooldownNotifications.clear();
        this.blockUpdates.clear();
        this.pendingTasks.clear();
        this.trackedBosses.clear();
        this.structures.clear();
        super.deactivate();
    }

    public List<FtTrackedStructure> getStructures() {
        return this.structures;
    }

    private static final class PendingPlastPlacement {
        private final Vec3d playerPosition;
        private final float pitch;
        private final long startedAt;
        private Vec3d soundPosition;
        private boolean resolved;

        private PendingPlastPlacement(Vec3d playerPosition, float pitch,
                                      long startedAt) {
            this.playerPosition = playerPosition;
            this.pitch = pitch;
            this.startedAt = startedAt;
        }
    }

    private static final class RecentStructureSound {
        private final Vec3d position;
        private final long timestamp;

        private RecentStructureSound(Vec3d position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }

    private static final class PlastGeometry {
        private final Vec3d center;
        private final boolean horizontal;

        private PlastGeometry(Vec3d center, boolean horizontal) {
            this.center = center;
            this.horizontal = horizontal;
        }
    }

}
