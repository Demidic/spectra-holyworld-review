package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.accessor.ArmorItemAccessor;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.Spectra;
import ru.spectra.client.util.GrimDelayHandler;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.type.InventoryScope;
import ru.spectra.client.net.InventoryService;
import ru.spectra.client.util.InventoryTask;
import ru.spectra.client.util.InventoryUtil;
import ru.spectra.client.event.JumpEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.ui.setting.KeybindSetting;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.util.MovementInputHelper;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.event.PacketSendEvent;
import ru.spectra.client.util.PlayerActionUtil;
import ru.spectra.client.model.ScopedSlot;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.model.SlotSearchResult2;
import ru.spectra.client.math.Stopwatch;
import ru.spectra.client.util.SwapUtil;
import ru.spectra.client.util.PvPModeDetector;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.math.DirectionalInput;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@Aliases(aliases = {"Elytra Helper", "Elytra Manager", "Auto Elytra", "Flight Helper", "Elytra Control", "Elytra Swap"})
public class ElytraHelperModule extends Module {
    public final KeybindSetting swapKey;
    public final NumberSetting swapTime;
    public final BooleanSetting autoFly;
    public final BooleanSetting blockInPvp;
    public final Mc mc;
    public final Stopwatch swapTimer;
    public final Stopwatch jumpTimer;
    private boolean pendingSwap;
    private boolean pendingWasElytra;
    private boolean pendingPerformed;
    private long pendingSince;
    private boolean pendingAutoFly;
    private long autoFlyAt;

    public ElytraHelperModule() {
        super(ModuleTab.MISC, "Elytra Helper");
        this.swapKey = new KeybindSetting(Lang.ELYTRAHELPER_SWAPKEY);
        this.swapTime = new NumberSetting(Translation.clearText("Swap time"))
                .currentValue(200.0f).range(200.0f, 500.0f).step(5.0f).unit(SettingUnit.MILLISECONDS);
        this.autoFly = new BooleanSetting(Translation.clearText("Auto fly")).setValue(false);
        this.blockInPvp = new BooleanSetting(Translation.clearText("Block in PvP")).setValue(true);
        this.mc = Mc.INSTANCE;
        this.swapTimer = new Stopwatch();
        this.jumpTimer = new Stopwatch();
        addSettings(this.swapKey, this.swapTime, this.autoFly, this.blockInPvp);
        this.swapKey.consumer(class664Var -> {
            trySwap();
        });
        register(PlayerTickEvent.class, event -> {
            if (!event.isPre() || !isState() || !mc.isWorldLoaded()) {
                return;
            }
            if (pendingAutoFly && System.currentTimeMillis() >= autoFlyAt) {
                pendingAutoFly = false;
                if (Mc.INSTANCE.getNetworkHandler() != null) {
                    Mc.INSTANCE.getNetworkHandler().sendChatCommand("fly");
                }
            }
            if (!pendingSwap) return;
            long elapsed = System.currentTimeMillis() - pendingSince;
            long performAt = Math.max(75L, Math.round(swapTime.currentValue() * 0.375f));
            if (!pendingPerformed && elapsed >= performAt) {
                pendingPerformed = true;
                performSwap(pendingWasElytra);
            }
            if (elapsed >= Math.round(swapTime.currentValue())) {
                pendingSwap = false;
                pendingPerformed = false;
                swapTimer.reset();
            }
        });
        register(MovementInputEvent.class, class040Var -> {
            if (isState() && pendingSwap) {
                class040Var.setInput(DirectionalInput.NONE);
                class040Var.setJumping(false);
                class040Var.setSneaking(false);
                class040Var.setSprinting(false);
            }
        });
    }

    public boolean canStart() {
        ClientPlayerEntity player = this.mc.getPlayer();
        boolean z = (!isState() || !MovementInputHelper.hasPlayerMovement() || !this.jumpTimer.hasElapsed(50L) || player.getAbilities().flying || player.hasVehicle() || player.isClimbing() || player.isTouchingWater() || player.getEquippedStack(EquipmentSlot.CHEST).willBreakNextUse() || player.hasStatusEffect(StatusEffects.LEVITATION) || !LivingEntity.canGlideWith(player.getEquippedStack(EquipmentSlot.CHEST), EquipmentSlot.CHEST)) ? false : true;
        if (z) {
            this.jumpTimer.reset();
        }
        return z;
    }

    public boolean isFlyingWithElytra(ClientPlayerEntity clientPlayerEntity) {
        ItemStack equippedStack = clientPlayerEntity.getEquippedStack(EquipmentSlot.CHEST);
        return (clientPlayerEntity.getAbilities().flying || clientPlayerEntity.hasVehicle() || clientPlayerEntity.isOnGround() || !clientPlayerEntity.isGliding() || clientPlayerEntity.isClimbing() || clientPlayerEntity.isTouchingWater() || clientPlayerEntity.hasStatusEffect(StatusEffects.LEVITATION) || !equippedStack.isOf(Items.ELYTRA) || equippedStack.willBreakNextUse()) ? false : true;
    }

    public void trySwap() {
        if (isState() && this.mc.isWorldLoaded() && !pendingSwap
                && this.swapTimer.hasElapsed((long) swapTime.currentValue())) {
            boolean z = this.mc.getPlayer().getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
            if (!z && blockInPvp.isValue() && PvPModeDetector.isPvPMode()) {
                Spectra.INSTANCE.notificationRepository().post(NotificationType.WARNING,
                        Text.literal(ClientLocalization.text(
                                "Elytra swap is blocked while PvP mode is active",
                                "Замена элитр заблокирована во время PvP")),
                        3L, TimeUnit.SECONDS);
                return;
            }
            if (findSwapItem(z).isEmpty()) {
                Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR,
                        Text.literal(ClientLocalization.text(
                                "Required chest item was not found",
                                "Нужный предмет для нагрудного слота не найден")),
                        3L, TimeUnit.SECONDS);
                return;
            }
            pendingWasElytra = z;
            pendingPerformed = false;
            pendingSince = System.currentTimeMillis();
            pendingSwap = true;
        }
    }

    public Optional<SlotSearchResult2> findSwapItem(boolean z) {
        return Spectra.INSTANCE.inventoryService().searcher().findItem(z ? this::isChestplate : this::isElytra, InventoryScope.HOTBAR, InventoryScope.INVENTORY);
    }

    public boolean isChestplate(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem item)) {
            return false;
        }
        return ((ArmorItemAccessor) item).spectra_ru$getType().getEquipmentSlot() == EquipmentSlot.CHEST
                && itemStack.getItem() != Items.ELYTRA;
    }

    public boolean isElytra(ItemStack itemStack) {
        return itemStack.getItem() == Items.ELYTRA;
    }

    public void useFirework(boolean z) {
        if (isState() && this.mc.isWorldLoaded() && this.mc.getPlayer().isGliding()) {
            InventoryService class011VarInventoryService = Spectra.INSTANCE.inventoryService();
            Predicate<ItemStack> predicate = itemStack -> {
                return itemStack.getItem() == Items.FIREWORK_ROCKET;
            };
            if (GrimDelayHandler.script.isFinished()) {
                class011VarInventoryService.searcher().findItem(predicate, InventoryScope.ALL).ifPresentOrElse(class329Var -> {
                    class011VarInventoryService.addTask(InventoryTask.create(predicate, class329Var, SwapUtil.needsStop(), false, false), this);
                }, () -> {
                    if (z) {
                        Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal(Lang.NO_ITEM_FOUND.effective().replace("{item}", String.valueOf(Formatting.RED) + Items.FIREWORK_ROCKET.getName().getString() + String.valueOf(Formatting.RESET))), 2L, TimeUnit.SECONDS);
                    }
                });
            }
        }
    }

    public void performSwap(boolean z) {
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        findSwapItem(z).ifPresentOrElse(class329Var -> {
            if (Mc.INSTANCE.getCurrentScreen() != null || Mc.INSTANCE.getInteractionManager() == null) {
                return;
            }
            int sourceSlot = class329Var.slotReference().increasedSlot();
            int syncId = player.currentScreenHandler.syncId;
            Mc.INSTANCE.getInteractionManager().clickSlot(syncId, sourceSlot, 0,
                    SlotActionType.PICKUP, player);
            Mc.INSTANCE.getInteractionManager().clickSlot(syncId, 6, 0,
                    SlotActionType.PICKUP, player);
            Mc.INSTANCE.getInteractionManager().clickSlot(syncId, sourceSlot, 0,
                    SlotActionType.PICKUP, player);
            ItemStack displayed = (z ? Items.NETHERITE_CHESTPLATE : Items.ELYTRA)
                    .getDefaultStack();
            Spectra.INSTANCE.notificationRepository().post((Text) Text.literal(
                    Lang.ELYTRAHELPER_SWAPPED_TO.effective().replace(
                            "{item}", String.valueOf(Formatting.RED)
                                    + ClientLocalization.itemName(displayed)
                                    + String.valueOf(Formatting.RESET))),
                    displayed, 3L, TimeUnit.SECONDS);
            if (!z && autoFly.isValue() && Mc.INSTANCE.getNetworkHandler() != null) {
                pendingAutoFly = true;
                autoFlyAt = System.currentTimeMillis() + 100L;
            }
        }, () -> {
            ItemStack missing = (z ? Items.NETHERITE_CHESTPLATE : Items.ELYTRA)
                    .getDefaultStack();
            Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR,
                    (Text) Text.literal(Lang.NO_ITEM_FOUND.effective().replace(
                            "{item}", String.valueOf(Formatting.RED)
                                    + ClientLocalization.itemName(missing)
                                    + String.valueOf(Formatting.RESET))),
                    3L, TimeUnit.SECONDS);
        });
    }

    public void holySwap(int i, int i2) {
        GrimDelayHandler.script.addTickStep(0, () -> {
            PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, i2, true);
        }).addTickStep(1, () -> {
            PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, 6, i2, true);
        }).addTickStep(2, () -> {
            PlayerActionUtil.INSTANCE.windowClick(SlotActionType.SWAP, i, i2, false);
            PlayerActionUtil.INSTANCE.updateSlots(true);
        });
    }

    @Override
    public void deactivate() {
        pendingSwap = false;
        pendingPerformed = false;
        pendingAutoFly = false;
        super.deactivate();
    }
}
