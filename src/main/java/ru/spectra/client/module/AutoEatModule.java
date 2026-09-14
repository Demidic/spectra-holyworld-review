package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.util.ItemUseController;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.PvPModeDetector;

import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.util.Hand;

@Aliases(aliases = {"Auto Eat", "Auto Food", "Automatic Eating", "Food Helper", "Auto Hunger Restore", "Eat Manager", "Auto Satiety", "Food Auto"})
public class AutoEatModule extends Module {
    public final BooleanSetting ignoreGoldenApplesSetting;
    public final BooleanSetting ignoreEnchantedGoldenApplesSetting;
    public boolean eating;
    public int savedHotbarSlot;
    private int eatingHotbarSlot = -1;
    public final Mc mc;
    public static final Set<StatusEffect> negativeEffects = Set.of((StatusEffect) StatusEffects.HUNGER.value(), (StatusEffect) StatusEffects.POISON.value(), (StatusEffect) StatusEffects.NAUSEA.value(), (StatusEffect) StatusEffects.WEAKNESS.value(), (StatusEffect) StatusEffects.BLINDNESS.value());

    public AutoEatModule() {
        super(ModuleTab.PLAYER, "Auto Eat");
        this.ignoreGoldenApplesSetting = new BooleanSetting(Lang.AUTOEAT_IGNORE_GOLDEN_APPLES);
        this.ignoreEnchantedGoldenApplesSetting = new BooleanSetting(Lang.AUTOEAT_IGNORE_ENCHANTED_GOLDEN_APPLES);
        this.savedHotbarSlot = -1;
        this.mc = Mc.INSTANCE;
        addSettings(this.ignoreGoldenApplesSetting, this.ignoreEnchantedGoldenApplesSetting);
        register(PlayerTickEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || !event.isPre()) {
                return;
            }
            ClientPlayerEntity player = this.mc.getPlayer();
            GameOptions options = this.mc.getGameOptions();
            if (PvPModeDetector.isPvPMode()) {
                stopEating(options, player);
                setState(false);
                return;
            }
            if (this.eating && player.getInventory().selectedSlot != this.eatingHotbarSlot) {
                // A manual slot change cancels our action; do not override it.
                stopEating(options, player);
                return;
            }
            if (!player.getHungerManager().isNotFull()) {
                stopEating(options, player);
                return;
            }
            if (getFoodHand(player) == Hand.MAIN_HAND) {
                this.eatingHotbarSlot = player.getInventory().selectedSlot;
                this.eating = true;
                ItemUseController.INSTANCE.useHand(Hand.MAIN_HAND);
            } else if (this.eating) {
                stopEating(options, player);
            } else {
                selectFood(player);
            }
        });
    }

    public Hand getFoodHand(ClientPlayerEntity player) {
        int slot = player.getInventory().selectedSlot;
        return slot >= 0 && slot < 9 && isEdible(player.getInventory().getStack(slot))
                ? Hand.MAIN_HAND : null;
    }

    public boolean isEating() {
        return this.eating;
    }

    public void stopEating(GameOptions options, ClientPlayerEntity player) {
        if (this.eating) {
            ItemUseController.INSTANCE.setUseItem(false);
            if (player != null && this.mc.getInteractionManager() != null
                    && player.isUsingItem() && player.getActiveHand() == Hand.MAIN_HAND
                    && player.getInventory().selectedSlot == this.eatingHotbarSlot) {
                this.mc.getInteractionManager().stopUsingItem(player);
            }
        }
        if (player != null) {
            restoreSlots(player);
        } else {
            this.savedHotbarSlot = -1;
        }
        this.eating = false;
        this.eatingHotbarSlot = -1;
    }

    public void restoreSlots(ClientPlayerEntity player) {
        if (this.savedHotbarSlot >= 0 && this.savedHotbarSlot < 9
                && player.getInventory().selectedSlot == this.eatingHotbarSlot) {
            player.getInventory().selectedSlot = this.savedHotbarSlot;
        }
        this.savedHotbarSlot = -1;
    }

    public void selectFood(ClientPlayerEntity player) {
        int bestSlot = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!isEdible(stack)) {
                continue;
            }
            ItemStack best = bestSlot < 0 ? ItemStack.EMPTY
                    : player.getInventory().getStack(bestSlot);
            if (bestSlot < 0 || getFoodPriority(stack) < getFoodPriority(best)
                    || (getFoodPriority(stack) == getFoodPriority(best)
                    && getFoodValue(stack) > getFoodValue(best))) {
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) {
            if (this.savedHotbarSlot < 0) {
                this.savedHotbarSlot = player.getInventory().selectedSlot;
            }
            this.eatingHotbarSlot = bestSlot;
            player.getInventory().selectedSlot = bestSlot;
        }
    }

    public int getFoodPriority(ItemStack itemStack) {
        boolean zMethod007 = isGoldenApple(itemStack.getItem());
        boolean zMethod001 = isSafeFood(itemStack);
        boolean zMethod012 = isEnchantedGoldenApple(itemStack.getItem());
        if (!zMethod001 || zMethod007 || zMethod012) {
            return (zMethod007 || zMethod012) ? 2 : 3;
        }
        return 1;
    }

    public boolean isSafeFood(ItemStack itemStack) {
        ConsumableComponent consumableComponent = (ConsumableComponent) itemStack.getComponents().get(DataComponentTypes.CONSUMABLE);
        if (consumableComponent == null) {
            return false;
        }
        for (net.minecraft.item.consume.ConsumeEffect consumeEffect : consumableComponent.onConsumeEffects()) {
            if (consumeEffect instanceof ApplyEffectsConsumeEffect) {
                ApplyEffectsConsumeEffect applyEffectsConsumeEffect = (ApplyEffectsConsumeEffect) consumeEffect;
                Iterator it = applyEffectsConsumeEffect.effects().iterator();
                while (it.hasNext()) {
                    if (negativeEffects.contains(((StatusEffectInstance) it.next()).getEffectType().value())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public float getFoodValue(ItemStack itemStack) {
        FoodComponent foodComponent = (FoodComponent) itemStack.getComponents().get(DataComponentTypes.FOOD);
        if (foodComponent == null) {
            return 0.0f;
        }
        return foodComponent.nutrition() * foodComponent.saturation();
    }

    public boolean isGoldenApple(Item item) {
        return item == Items.GOLDEN_APPLE;
    }

    public boolean isEnchantedGoldenApple(Item item) {
        return item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public boolean shouldIgnore(Item item) {
        if (this.ignoreGoldenApplesSetting.isValue() && isGoldenApple(item)) {
            return true;
        }
        return this.ignoreEnchantedGoldenApplesSetting.isValue() && isEnchantedGoldenApple(item);
    }

    public boolean isEdible(ItemStack stack) {
        return !stack.isEmpty() && stack.get(DataComponentTypes.FOOD) != null
                && stack.get(DataComponentTypes.CONSUMABLE) != null
                && !shouldIgnore(stack.getItem());
    }

    @Override
    public void deactivate() {
        stopEating(this.mc.getGameOptions(), this.mc.getPlayer());
        super.deactivate();
    }
}
