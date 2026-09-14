package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.MovementInputEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.math.DirectionalInput;
import ru.spectra.client.model.SlotSearchResult2;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.AutoSwapItemType;
import ru.spectra.client.type.CombatPauseManager;
import ru.spectra.client.type.InventoryScope;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.KeybindSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.util.GrimDelayHandler;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.util.ItemMatcher;
import ru.spectra.client.util.PvPModeDetector;
import ru.spectra.client.util.SwapUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aliases(aliases = {"Swap", "Auto Swap", "Smart Swap", "Delayed Swap", "Offhand Swap"})
public final class AutoSwapModule extends Module {
    private static final long BETWEEN_SWAPS_DELAY_MS = 500L;
    private static final float SMART_SCORE_MARGIN = 1.5f;
    private static final float SMART_DEFENSE_SCORE_MARGIN = 0.65f;
    private static final float SMART_ATTACK_SCORE_MARGIN = 1.2f;
    private static final Pattern STAT_LINE_PATTERN =
            Pattern.compile("([+\\-]?\\d+(?:[\\.,]\\d+)?)\\s*%?\\s*(.*)");

    public final ModeSetting<AutoSwapItemType> swapFrom =
            new ModeSetting<AutoSwapItemType>(Translation.clearText("Swap from")).values(AutoSwapItemType.class);
    public final ModeSetting<AutoSwapItemType> swapTo =
            new ModeSetting<AutoSwapItemType>(Translation.clearText("Swap to")).values(AutoSwapItemType.class);
    public final KeybindSetting swapKey = new KeybindSetting(Translation.clearText("Swap key"));
    public final NumberSetting swapTime = new NumberSetting(Translation.clearText("Swap time"))
            .currentValue(200.0f).range(200.0f, 500.0f).step(5.0f).unit(SettingUnit.MILLISECONDS);
    public final BooleanSetting priorityEnchanted =
            new BooleanSetting(Translation.clearText("Priority enchanted")).setValue(false);
    public final BooleanSetting preventDuplicates =
            new BooleanSetting(Translation.clearText("Prevent duplicates")).setValue(true);
    public final BooleanSetting notifications =
            new BooleanSetting(Translation.clearText("Swap notifications")).setValue(true);
    public final BooleanSetting smartSwap =
            new BooleanSetting(Translation.clearText("Smart swap")).setValue(false);
    public final BooleanSetting pvpAnalyzer =
            new BooleanSetting(Translation.clearText("PvP analyzer")).setValue(false)
                    .visible(smartSwap::isValue);

    private ItemMatcher pendingMatcher;
    private long pendingSince;
    private long cooldownUntil;
    private boolean pending;
    private boolean swapPerformed;
    private long lastSmartAnalysis;

    public AutoSwapModule() {
        super(ModuleTab.COMBAT, "Auto Swap");
        serverAccessPolicy(ServerAccessPolicy.denyOnServers("Space-Times"));
        swapFrom.select(AutoSwapItemType.TOTEM);
        swapTo.select(AutoSwapItemType.SPHERE);
        addSettings(swapFrom, swapTo, swapKey, swapTime, notifications);
        swapKey.consumer(ignored -> requestManualSwap());
        register(PlayerTickEvent.class, event -> {
            if (!event.isPre() || !isState() || !Mc.INSTANCE.isWorldLoaded()) {
                return;
            }
            updatePendingSwap();
            updateSmartSwap();
        });
        register(MovementInputEvent.class, event -> {
            if (isState() && pending) {
                event.setInput(DirectionalInput.NONE);
                event.setJumping(false);
                event.setSneaking(false);
                event.setSprinting(false);
            }
        });
    }

    public void requestManualSwap() {
        if (!canStartSwap()) {
            return;
        }
        pendingMatcher = resolveItemMatcher(swapFrom.currentValue(), swapTo.currentValue());
        if (bestCandidate(pendingMatcher).isEmpty()) {
            notifyMissing(pendingMatcher.displayItem());
            return;
        }
        beginDelayedSwap();
    }

    private void updatePendingSwap() {
        if (!pending) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - pendingSince;
        long performAt = Math.max(75L, Math.round(swapTime.currentValue() * 0.375f));
        if (!swapPerformed && elapsed >= performAt) {
            swapPerformed = true;
            if (pendingMatcher != null && !CombatPauseManager.INSTANCE.shouldPauseAutoSwap()) {
                bestCandidate(pendingMatcher).ifPresentOrElse(result -> {
                    if (swapItemToOffhand(result)) {
                        cooldownUntil = now + BETWEEN_SWAPS_DELAY_MS;
                    }
                }, () -> notifyMissing(pendingMatcher.displayItem()));
            }
        }
        if (elapsed < Math.round(swapTime.currentValue())) {
            return;
        }
        pending = false;
        swapPerformed = false;
        pendingMatcher = null;
    }

    private void updateSmartSwap() {
        if (!smartSwap.isValue() || pending || System.currentTimeMillis() < cooldownUntil) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSmartAnalysis < 200L) {
            return;
        }
        lastSmartAnalysis = now;
        AutoSwapItemType desired = configuredSmartTarget();
        ItemStack offhand = Mc.INSTANCE.getPlayer().getOffHandStack();
        ItemMatcher matcher = new ItemMatcher(desired.filter(), desired.displayItem());
        SmartContext context = buildSmartContext();
        Optional<SlotSearchResult2> candidate = bestSmartCandidate(matcher, offhand, context);
        if (candidate.isEmpty()
                || sameItemAndComponents(candidate.get().stack(), offhand)) {
            return;
        }
        pendingMatcher = new ItemMatcher(
                stack -> desired.filter().test(stack)
                        && sameItemAndComponents(stack, candidate.get().stack()),
                desired.displayItem()
        );
        beginDelayedSwap();
    }

    private AutoSwapItemType configuredSmartTarget() {
        ItemStack offhand = Mc.INSTANCE.getPlayer().getOffHandStack();
        return swapFrom.currentValue().filter().test(offhand)
                ? swapTo.currentValue()
                : swapFrom.currentValue();
    }

    private Optional<SlotSearchResult2> bestSmartCandidate(
            ItemMatcher matcher, ItemStack current, SmartContext context) {
        ItemStats currentStats = parseItemStats(current);
        float currentScore = calculateSmartScore(currentStats, context);
        return Spectra.INSTANCE.inventoryService().searcher()
                .findAllItems(matcher.predicate(),
                        InventoryScope.HOTBAR, InventoryScope.INVENTORY)
                .stream()
                .filter(result -> {
                    ItemStats stats = parseItemStats(result.stack());
                    if (!stats.hasAnyStats) {
                        return false;
                    }
                    float margin = smartScoreMargin(stats, currentStats, context);
                    return calculateSmartScore(stats, context)
                            > currentScore + margin;
                })
                .max(Comparator.comparingDouble(result ->
                        calculateSmartScore(parseItemStats(result.stack()), context)));
    }

    private Optional<SlotSearchResult2> bestCandidate(ItemMatcher matcher) {
        List<SlotSearchResult2> candidates = Spectra.INSTANCE.inventoryService().searcher()
                .findAllItems(matcher.predicate(), InventoryScope.HOTBAR, InventoryScope.INVENTORY);
        ItemStack offhand = Mc.INSTANCE.getPlayer().getOffHandStack();
        return candidates.stream()
                .max(Comparator.comparingDouble(result -> score(result.stack())));
    }

    private double score(ItemStack stack) {
        double score = 0.0;
        if (priorityEnchanted.isValue() && stack.hasEnchantments()) {
            score += 10_000.0;
        }
        if (stack.isDamageable()) {
            score += (stack.getMaxDamage() - stack.getDamage()) * 0.01;
        }
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            score += food.nutrition() * 10.0 + food.saturation();
        }
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            score += 500.0;
        }
        return score;
    }

    private SmartContext buildSmartContext() {
        var player = Mc.INSTANCE.getPlayer();
        SmartContext context = new SmartContext();
        context.ownHealthPercent =
                (player.getHealth() + player.getAbsorptionAmount())
                        / Math.max(1.0f, player.getMaxHealth()) * 100.0f;

        LivingEntity target = smartTarget();
        if (target != null) {
            context.targetHealthPercent =
                    target.getHealth() / Math.max(1.0f, target.getMaxHealth())
                            * 100.0f;
            context.weAreDominating = context.ownHealthPercent
                    >= context.targetHealthPercent + 18.0f;
            context.enemyIsDominating = context.targetHealthPercent
                    >= context.ownHealthPercent + 18.0f;
        }
        int nearbyPlayers = 0;
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other != player && other.isAlive() && !other.isSpectator()
                    && other.squaredDistanceTo(player) <= 64.0d) {
                nearbyPlayers++;
            }
        }
        context.multiEnemyPressure = nearbyPlayers >= 2;
        context.combatActive = PvPModeDetector.isPvPMode()
                || CombatPauseManager.INSTANCE.isInCombat()
                || player.hurtTime > 0;
        return context;
    }

    private LivingEntity smartTarget() {
        if (Mc.INSTANCE.getCrosshairTarget() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living
                && living != Mc.INSTANCE.getPlayer()
                && living.isAlive()) {
            return living;
        }
        Entity attacker = Mc.INSTANCE.getPlayer().getAttacker();
        return attacker instanceof LivingEntity living && living.isAlive()
                ? living : null;
    }

    private float calculateSmartScore(ItemStats stats, SmartContext context) {
        if (stats == null || !stats.hasAnyStats) {
            return -1000.0f;
        }
        float attack = positive(stats, SmartAttribute.ATTACK_DAMAGE)
                + positive(stats, SmartAttribute.ATTACK_SPEED) * 0.45f
                + positive(stats, SmartAttribute.ATTACK_KNOCKBACK) * 0.65f;
        float survival = positive(stats, SmartAttribute.MAX_HEALTH) * 1.8f
                + positive(stats, SmartAttribute.ARMOR) * 1.55f
                + positive(stats, SmartAttribute.ARMOR_TOUGHNESS) * 1.35f
                + positive(stats, SmartAttribute.KNOCKBACK_RESISTANCE) * 2.0f;
        float risk = stats.negative(SmartAttribute.MAX_HEALTH) * 1.7f
                + stats.negative(SmartAttribute.ARMOR) * 1.35f
                + stats.negative(SmartAttribute.ARMOR_TOUGHNESS) * 1.25f
                + stats.negative(SmartAttribute.MOVEMENT_SPEED) * 0.35f
                + stats.negative(SmartAttribute.ATTACK_DAMAGE) * 0.25f;
        float utility = stats.get(SmartAttribute.KNOCKBACK_RESISTANCE) * 2.0f
                + stats.get(SmartAttribute.LUCK) * 0.15f;
        if (preferDefense(context)) {
            return survival * 5.0f + utility + attack * 0.55f
                    - risk * 7.5f;
        }
        if (preferAttack(context)) {
            float targetLowBonus = context.targetHealthPercent > 0.0f
                    && context.targetHealthPercent <= 55.0f ? 1.0f : 0.0f;
            return attack * (3.7f + targetLowBonus)
                    + survival * 0.65f + utility * 0.35f
                    - risk * (context.ownHealthPercent >= 90.0f ? 0.9f : 1.8f);
        }
        return attack * 1.55f + survival * 2.5f + utility * 0.5f
                - risk * (context.ownHealthPercent >= 75.0f ? 2.3f : 4.2f);
    }

    private float smartScoreMargin(
            ItemStats candidate, ItemStats current, SmartContext context) {
        if (preferDefense(context)
                && current != null
                && current.risk() > candidate.risk() + 1.0f) {
            return SMART_DEFENSE_SCORE_MARGIN;
        }
        if (preferAttack(context)) {
            if (current != null
                    && candidate.risk() > current.risk() + 2.0f
                    && context.ownHealthPercent < 90.0f) {
                return SMART_ATTACK_SCORE_MARGIN + 2.0f;
            }
            return SMART_ATTACK_SCORE_MARGIN;
        }
        return SMART_SCORE_MARGIN;
    }

    private static boolean preferDefense(SmartContext context) {
        return context.multiEnemyPressure
                || context.ownHealthPercent <= 55.0f
                || context.enemyIsDominating
                && context.ownHealthPercent < 75.0f;
    }

    private static boolean preferAttack(SmartContext context) {
        return !context.multiEnemyPressure
                && context.ownHealthPercent >= 75.0f
                && (context.weAreDominating
                || context.targetHealthPercent > 0.0f
                && context.targetHealthPercent <= 65.0f
                || !context.combatActive);
    }

    private static float positive(ItemStats stats, SmartAttribute attribute) {
        return Math.max(0.0f, stats.get(attribute));
    }

    private ItemStats parseItemStats(ItemStack stack) {
        ItemStats stats = new ItemStats();
        if (stack == null || stack.isEmpty() || Mc.INSTANCE.getPlayer() == null) {
            return stats;
        }
        try {
            List<Text> tooltip = stack.getTooltip(
                    Item.TooltipContext.DEFAULT,
                    Mc.INSTANCE.getPlayer(),
                    TooltipType.BASIC
            );
            for (Text line : tooltip) {
                parseStatLine(line.getString(), stats);
            }
        } catch (RuntimeException ignored) {
            // A malformed server component must not break swapping. The item
            // simply does not participate in smart scoring on that tick.
        }
        return stats;
    }

    private static void parseStatLine(String raw, ItemStats stats) {
        if (raw == null) {
            return;
        }
        String line = raw.toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ')
                .replaceAll("\u00a7[0-9a-fk-or]", "")
                .trim();
        Matcher matcher = STAT_LINE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return;
        }
        float value;
        try {
            value = Float.parseFloat(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return;
        }
        SmartAttribute attribute = SmartAttribute.fromText(matcher.group(2));
        if (attribute != null) {
            stats.add(attribute, value);
        }
    }

    private static boolean sameItemAndComponents(ItemStack left, ItemStack right) {
        return left != null && right != null
                && left.getItem() == right.getItem()
                && Objects.equals(left.getComponents(), right.getComponents());
    }

    private void beginDelayedSwap() {
        pending = true;
        swapPerformed = false;
        pendingSince = System.currentTimeMillis();
    }

    private boolean canStartSwap() {
        return isState() && Mc.INSTANCE.isWorldLoaded() && !pending
                && System.currentTimeMillis() >= cooldownUntil
                && !CombatPauseManager.INSTANCE.shouldPauseAutoSwap();
    }

    private boolean swapItemToOffhand(SlotSearchResult2 result) {
        if (Mc.INSTANCE.getCurrentScreen() != null || Mc.INSTANCE.getInteractionManager() == null) {
            return false;
        }
        var player = Mc.INSTANCE.getPlayer();
        // Slot clicks mutate the inventory-backed stack referenced by the search
        // result. Preserve what is being swapped before the first click so both
        // the notification text and its item icon remain available afterwards.
        ItemStack displayedStack = result.stack().copy();
        int sourceSlot = result.slotReference().increasedSlot();
        int syncId = player.currentScreenHandler.syncId;
        Mc.INSTANCE.getInteractionManager().clickSlot(syncId, sourceSlot, 0,
                SlotActionType.PICKUP, player);
        Mc.INSTANCE.getInteractionManager().clickSlot(syncId, 45, 0,
                SlotActionType.PICKUP, player);
        Mc.INSTANCE.getInteractionManager().clickSlot(syncId, sourceSlot, 0,
                SlotActionType.PICKUP, player);
        if (notifications.isValue()) {
            Spectra.INSTANCE.notificationRepository().post(
                    Text.literal(ClientLocalization.text(
                            "Swapped to ", "Заменено на ")
                            + ClientLocalization.itemName(displayedStack)),
                    displayedStack, 3L, TimeUnit.SECONDS);
        }
        return true;
    }

    private static void notifyMissing(Item item) {
        Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR,
                Text.literal(ClientLocalization.text(
                        "Item not found: ", "Предмет не найден: ")
                        + ClientLocalization.minecraft(
                                item.getTranslationKey(),
                                item.getName().getString())),
                3L, TimeUnit.SECONDS);
    }

    public ItemMatcher resolveItemMatcher(AutoSwapItemType from, AutoSwapItemType to) {
        return from.filter().test(Mc.INSTANCE.getPlayer().getOffHandStack())
                ? new ItemMatcher(to.filter(), to.displayItem())
                : new ItemMatcher(from.filter(), from.displayItem());
    }

    @Override
    public void deactivate() {
        pending = false;
        swapPerformed = false;
        pendingMatcher = null;
        super.deactivate();
    }

    private static final class SmartContext {
        float ownHealthPercent;
        float targetHealthPercent;
        boolean weAreDominating;
        boolean enemyIsDominating;
        boolean multiEnemyPressure;
        boolean combatActive;
    }

    private static final class ItemStats {
        private final EnumMap<SmartAttribute, Float> positive =
                new EnumMap<>(SmartAttribute.class);
        private final EnumMap<SmartAttribute, Float> negative =
                new EnumMap<>(SmartAttribute.class);
        private boolean hasAnyStats;

        void add(SmartAttribute attribute, float value) {
            if (attribute == null || value == 0.0f) {
                return;
            }
            this.hasAnyStats = true;
            Map<SmartAttribute, Float> values =
                    value > 0.0f ? this.positive : this.negative;
            values.merge(attribute, Math.abs(value), Float::sum);
        }

        float get(SmartAttribute attribute) {
            return this.positive.getOrDefault(attribute, 0.0f)
                    - this.negative.getOrDefault(attribute, 0.0f);
        }

        float negative(SmartAttribute attribute) {
            return this.negative.getOrDefault(attribute, 0.0f);
        }

        float risk() {
            return negative(SmartAttribute.MAX_HEALTH) * 1.7f
                    + negative(SmartAttribute.ARMOR) * 1.35f
                    + negative(SmartAttribute.ARMOR_TOUGHNESS) * 1.25f
                    + negative(SmartAttribute.MOVEMENT_SPEED) * 0.35f
                    + negative(SmartAttribute.ATTACK_DAMAGE) * 0.25f;
        }
    }

    private enum SmartAttribute {
        MAX_HEALTH("max health", "health", "\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u0435 \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u0435", "\u0437\u0434\u043e\u0440\u043e\u0432\u044c\u0435"),
        KNOCKBACK_RESISTANCE("knockback resistance", "\u0441\u043e\u043f\u0440\u043e\u0442\u0438\u0432\u043b\u0435\u043d\u0438\u0435 \u043e\u0442\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u044e", "\u043e\u0442\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u0435"),
        MOVEMENT_SPEED("movement speed", "speed", "\u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u043f\u0435\u0440\u0435\u0434\u0432\u0438\u0436\u0435\u043d\u0438\u044f", "\u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c"),
        ATTACK_DAMAGE("attack damage", "damage", "\u0443\u0440\u043e\u043d", "\u0430\u0442\u0430\u043a\u0430"),
        ATTACK_KNOCKBACK("attack knockback", "knockback", "\u043e\u0442\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u043d\u0438\u0435 \u0430\u0442\u0430\u043a\u0438"),
        ATTACK_SPEED("attack speed", "\u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0430\u0442\u0430\u043a\u0438"),
        ARMOR("armor", "\u0431\u0440\u043e\u043d\u044f"),
        ARMOR_TOUGHNESS("armor toughness", "toughness", "\u0442\u0432\u0435\u0440\u0434\u043e\u0441\u0442\u044c \u0431\u0440\u043e\u043d\u0438", "\u0442\u0432\u0451\u0440\u0434\u043e\u0441\u0442\u044c \u0431\u0440\u043e\u043d\u0438"),
        LUCK("luck", "\u0443\u0434\u0430\u0447\u0430");

        private final String[] aliases;

        SmartAttribute(String... aliases) {
            this.aliases = aliases;
        }

        static SmartAttribute fromText(String text) {
            String normalized = text.toLowerCase(Locale.ROOT)
                    .replace('_', ' ')
                    .replace('.', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();
            SmartAttribute best = null;
            int bestLength = -1;
            for (SmartAttribute attribute : values()) {
                for (String alias : attribute.aliases) {
                    if (normalized.contains(alias)
                            && alias.length() > bestLength) {
                        best = attribute;
                        bestLength = alias.length();
                    }
                }
            }
            return best;
        }
    }
}
