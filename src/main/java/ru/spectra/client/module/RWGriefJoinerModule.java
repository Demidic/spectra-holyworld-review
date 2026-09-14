package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.Spectra;
import ru.spectra.client.event.HandledScreenRenderEvent;
import ru.spectra.client.util.InventoryUtil;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.util.PlayerActionUtil;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.math.Rotation;
import ru.spectra.client.util.ScoreboardHelper;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.ui.setting.TextFieldSetting;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.WavSoundPlayer;

import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.StringHelper;

@Aliases(aliases = {"Grief Joiner", "ReallyWorld", "Auto Join", "Server Joiner"})
public class RWGriefJoinerModule extends Module {
    public final BooleanSetting megaGrief;
    public final TextFieldSetting griefNumber;
    public final Stopwatch queueStopwatch;
    public final Stopwatch clickStopwatch;
    public final Stopwatch pageStopwatch;

    public RWGriefJoinerModule() {
        super(ModuleTab.MISC, "RW Grief Joiner");
        this.megaGrief = new BooleanSetting(Translation.clearText("MEGA Grief"));
        this.griefNumber = new TextFieldSetting(Lang.GRIEFJOINER_GRIEF_NUMBER, Lang.GRIEFJOINER_GRIEF_NUMBER_DESC).setText("1").setPlaceholder(Lang.GRIEFJOINER_GRIEF_NUMBER_PLACEHOLDER).setMax(3).setOnlyDigits(true).visible(() -> {
            return Boolean.valueOf(!this.megaGrief.isValue());
        });
        this.queueStopwatch = new Stopwatch(false);
        this.clickStopwatch = new Stopwatch();
        this.pageStopwatch = new Stopwatch();
        addSettings(this.griefNumber, this.megaGrief);
        register(PacketReceiveEvent.class, class051Var -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                if ((class051Var.getPacket()) instanceof GameMessageS2CPacket packet ) {
                    String strStripTextFormat = StringHelper.stripTextFormat(packet.content().getString().toLowerCase());
                    if (strStripTextFormat.contains("к сожалению сервер переполнен") || strStripTextFormat.contains("подождите 20 секунд!") || strStripTextFormat.contains("большой поток игроков") || strStripTextFormat.contains("imperator") || strStripTextFormat.contains("подождите несколько секунд")) {
                        this.queueStopwatch.reset();
                    }
                }
            }
        });
        register(HandledScreenRenderEvent.class, class015Var -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                Screen currentScreen = Mc.INSTANCE.getCurrentScreen();
                if (currentScreen instanceof GenericContainerScreen) {
                    String strStripTextFormat = StringHelper.stripTextFormat(currentScreen.getTitle().getString());
                    if (this.clickStopwatch.hasElapsed(500L, TimeUnit.MILLISECONDS)) {
                        if (strStripTextFormat.contains("Выбор сервера")) {
                            int iMethod002 = findSlotByName(this.megaGrief.isValue() ? "мега гриф (1.16.5)" : "гриферское выживание (1.16.5-1.20.4)");
                            if (iMethod002 != -1) {
                                if (this.queueStopwatch.hasElapsed(300L, TimeUnit.MILLISECONDS) || !this.megaGrief.isValue()) {
                                    PlayerActionUtil.INSTANCE.windowClick(SlotActionType.QUICK_MOVE, iMethod002, 0);
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        if (strStripTextFormat.contains("Выбор мира грифа") && this.queueStopwatch.hasElapsed(5L, TimeUnit.SECONDS) && !this.megaGrief.isValue()) {
                            int i = 1;
                            try {
                                i = Integer.parseInt(this.griefNumber.getText());
                            } catch (Exception e) {
                            }
                            int iMethod003 = findSlotByName("гриф #" + i + " (1.16.5+)");
                            if (iMethod003 != -1) {
                                PlayerActionUtil.INSTANCE.windowClick(SlotActionType.QUICK_MOVE, iMethod003, 0);
                                this.clickStopwatch.reset();
                                return;
                            }
                            if (this.pageStopwatch.hasElapsed(350L, TimeUnit.MILLISECONDS)) {
                                boolean z = false;
                                if (i > 36) {
                                    int iMethod004 = findSlotContaining("следующая страница");
                                    if (iMethod004 != -1) {
                                        PlayerActionUtil.INSTANCE.windowClick(SlotActionType.QUICK_MOVE, iMethod004, 0);
                                        z = true;
                                    }
                                } else {
                                    int iMethod005 = findSlotContaining("предыдущая страница");
                                    if (iMethod005 != -1) {
                                        PlayerActionUtil.INSTANCE.windowClick(SlotActionType.QUICK_MOVE, iMethod005, 0);
                                        z = true;
                                    }
                                }
                                if (z) {
                                    this.pageStopwatch.reset();
                                    this.clickStopwatch.reset();
                                }
                            }
                        }
                    }
                }
            }
        });
        register(PlayerTickEvent.class, class130Var -> {
            if (isState() && class130Var.isPre() && Mc.INSTANCE.isWorldLoaded() && Mc.INSTANCE.isWorldLoaded()) {
                Screen currentScreen = Mc.INSTANCE.getCurrentScreen();
                if (!ServerUtil.isConnectedToServer("reallyworld")) {
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal(Lang.GRIEFJOINER_WRONG_SERVER.effective()), 3L, TimeUnit.SECONDS);
                    switchState();
                } else if (StringHelper.stripTextFormat(ScoreboardHelper.INSTANCE.getHeaderAsString()).toLowerCase().contains("гриферское выживание")) {
                    WavSoundPlayer.INSTANCE.playSound("apple_pay", 80.0f, false);
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.SUCCESS, (Text) Text.literal(Lang.GRIEFJOINER_JOIN_SUCCESS.effective()), 3L, TimeUnit.SECONDS);
                    switchState();
                } else {
                    if (currentScreen instanceof GenericContainerScreen) {
                        return;
                    }
                    findAndUseCompass();
                }
            }
        });
    }

    @Override
    public void activate() {
        findAndUseCompass();
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    public int findSlotByName(String str) {
        GenericContainerScreenHandler currentScreenHandler = (GenericContainerScreenHandler) (Mc.INSTANCE.getCurrentScreenHandler());
        if (!(currentScreenHandler instanceof GenericContainerScreenHandler)) {
            return -1;
        }
        GenericContainerScreenHandler genericContainerScreenHandler = currentScreenHandler;
        for (int i = 0; i < genericContainerScreenHandler.getInventory().size(); i++) {
            ItemStack stack = genericContainerScreenHandler.getSlot(i).getStack();
            if (stack != null && StringHelper.stripTextFormat(stack.getName().getString().toLowerCase()).equalsIgnoreCase(str.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    public int findSlotContaining(String str) {
        GenericContainerScreenHandler currentScreenHandler = (GenericContainerScreenHandler) (Mc.INSTANCE.getCurrentScreenHandler());
        if (!(currentScreenHandler instanceof GenericContainerScreenHandler)) {
            return -1;
        }
        GenericContainerScreenHandler genericContainerScreenHandler = currentScreenHandler;
        String lowerCase = str.toLowerCase();
        for (int i = 0; i < genericContainerScreenHandler.getInventory().size(); i++) {
            ItemStack stack = genericContainerScreenHandler.getSlot(i).getStack();
            if (stack != null && !stack.isEmpty() && StringHelper.stripTextFormat(stack.getName().getString().toLowerCase()).contains(lowerCase)) {
                return i;
            }
        }
        return -1;
    }

    public void findAndUseCompass() {
        int iFindItemSlot;
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player == null || (iFindItemSlot = InventoryUtil.INSTANCE.findItemSlot(Items.COMPASS, false, false)) == -1) {
            return;
        }
        if (player.getInventory().selectedSlot != iFindItemSlot) {
            player.getInventory().selectedSlot = iFindItemSlot;
        }
        PlayerActionUtil.INSTANCE.interactItem(Hand.MAIN_HAND, Rotation.playerRotation(), false);
    }
}
