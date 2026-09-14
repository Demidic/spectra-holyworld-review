package ru.spectra.client.module;

import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class ItemRadiusModule extends Module {
    public final MultiSelectSetting<RadiusItem> items =
            new MultiSelectSetting<RadiusItem>(Translation.clearText("Items")).values(RadiusItem.class);
    public final BooleanSetting showOtherPlayers =
            new BooleanSetting(Translation.clearText("Show other players")).setValue(false);

    public ItemRadiusModule() {
        super(ModuleTab.RENDER, "Item Radius");
        items.select(RadiusItem.values());
        addSettings(items, showOtherPlayers);
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        renderFor(event, Mc.INSTANCE.getPlayer());
        if (showOtherPlayers.isValue()) {
            for (PlayerEntity player : Mc.INSTANCE.getWorld().getPlayers()) {
                if (player != Mc.INSTANCE.getPlayer()
                        && player.squaredDistanceTo(Mc.INSTANCE.getPlayer()) <= 64.0 * 64.0) {
                    renderFor(event, player);
                }
            }
        }
    }

    private void renderFor(WorldRenderEvent event, PlayerEntity player) {
        RadiusItem type = heldType(player.getMainHandStack(), player.getOffHandStack());
        if (type == null || !items.selectedValues().isEmpty() && !items.isSelected(type)) {
            return;
        }
        if (type == RadiusItem.TRAP) {
            renderTrapCube(event, player);
            return;
        }
        int radius = type.radius;
        // The 1.16 renderer added 1.6 blocks again when constructing BlockPos.
        // Keeping only -1.4 put the entire visualization almost two blocks too low.
        Vec3d center = player.getLerpedPos(Mc.INSTANCE.getTickDelta()).add(0.0, 0.2, 0.0);
        boolean playerInRange = Mc.INSTANCE.getWorld().getPlayers().stream()
                .anyMatch(other -> other != player && !other.isSpectator()
                        && other.getPos().squaredDistanceTo(center) <= radius * radius);
        int color = playerInRange ? 0xFFAA2828 : 0xFF24AA45;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (Math.abs(distance - radius) > 0.7) {
                    continue;
                }
                Box marker = new Box(
                        Math.floor(center.x) + x, Math.floor(center.y), Math.floor(center.z) + z,
                        Math.floor(center.x) + x + 1.0, Math.floor(center.y) + 1.0,
                        Math.floor(center.z) + z + 1.0);
                ShapeRenderer.INSTANCE.addOutline(event.matrixStack().peek().getPositionMatrix(),
                        marker, color, 2.0f, true, true);
            }
        }
    }

    private void renderTrapCube(WorldRenderEvent event, PlayerEntity player) {
        Vec3d position = player.getLerpedPos(Mc.INSTANCE.getTickDelta());
        double centerX = Math.floor(position.x) + 0.5;
        double centerY = Math.floor(position.y) + 2.125;
        double centerZ = Math.floor(position.z) + 0.5;
        Box cube = new Box(
                centerX - 2.0, centerY - 2.0, centerZ - 2.0,
                centerX + 2.0, centerY + 2.0, centerZ + 2.0
        );
        boolean playerInRange = Mc.INSTANCE.getWorld().getPlayers().stream()
                .anyMatch(other -> other != player && !other.isSpectator()
                        && cube.contains(other.getPos()));
        int color = playerInRange ? 0xFFAA2828 : 0xFF24AA45;
        ShapeRenderer.INSTANCE.addOutline(event.matrixStack().peek().getPositionMatrix(),
                cube, color, 3.0f, true, true);
    }

    private RadiusItem heldType(ItemStack main, ItemStack offhand) {
        for (RadiusItem value : RadiusItem.values()) {
            if (main.isOf(value.item) || offhand.isOf(value.item)) {
                return value;
            }
        }
        return null;
    }

    public enum RadiusItem implements DisplayNamed {
        DISORIENTATION("Disorientation", Items.ENDER_EYE, 10),
        EXPLICIT_DUST("Explicit dust", Items.SUGAR, 10),
        FIRE_CHARGE("Fire charge", Items.FIRE_CHARGE, 10),
        GOD_AURA("God aura", Items.PHANTOM_MEMBRANE, 2),
        TRAP("Trap", Items.NETHERITE_SCRAP, 2),
        PLAST("Plast", Items.DRIED_KELP, 2);

        private final Translation name;
        private final Item item;
        private final int radius;

        RadiusItem(String name, Item item, int radius) {
            this.name = Translation.clearText(name);
            this.item = item;
            this.radius = radius;
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
