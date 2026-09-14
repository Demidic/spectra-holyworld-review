package ru.spectra.mixin;

import ru.spectra.client.event.HandledScreenRenderEvent;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.ShulkerPreviewModule;
import ru.spectra.client.util.ColorUtil;
import ru.spectra.client.event.FocusedSlotEvent;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.event.SlotScrollEvent;
import ru.spectra.client.type.Mc;
import ru.spectra.client.resource.ProtectedTextureRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HandledScreen.class})
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    public int backgroundWidth;

    @Shadow
    public int backgroundHeight;

    @Shadow
    protected abstract void onMouseClick(int i);

    @Shadow
    protected abstract boolean isPointOverSlot(Slot slot, double d, double d2);

    protected HandledScreenMixin(Text text) {
        super(text);
    }

    @Inject(method = {"keyPressed"}, at = {@At("HEAD")})
    private void onKeyPressed(int i, int i2, int i3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new FocusedSlotEvent(this.focusedSlot));
    }

    @Inject(method = {"render"}, at = {@At("RETURN")})
    public void onRender(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new HandledScreenRenderEvent(drawContext, this.backgroundWidth, this.backgroundHeight));
    }

    @Inject(method = {"render"}, at = {@At("HEAD")})
    private void render(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        ScreenHandler screenHandler = Mc.INSTANCE.getPlayer().currentScreenHandler;
        for (int i3 = 0; i3 < screenHandler.slots.size(); i3++) {
            Slot slot = (Slot) screenHandler.slots.get(i3);
            if (isPointOverSlot(slot, i, i2) && slot.isEnabled()) {
                Spectra.INSTANCE.eventDispatcher().dispatch(new SlotScrollEvent(slot, slot.id));
            }
        }
    }

    @Inject(method = {"render"}, at = {@At("RETURN")})
    public void renderReturn(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null
                || !Spectra.INSTANCE.moduleRepository().get(ShulkerPreviewModule.class).isState()) {
            return;
        }
        Optional.ofNullable(this.focusedSlot).ifPresent(slot -> drawShulkerItems(slot, drawContext, i, i2));
    }

    @Unique
    public void drawShulkerItems(Slot slot, DrawContext drawContext, int i, int i2) {
        ItemStack stack = slot.getStack();
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                List<ItemStack> containerStacks = getContainerStacks(stack);
                if (containerStacks == null || containerStacks.isEmpty()) {
                    return;
                }
                int iMultBright = ColorUtil.multBright(ColorUtil.replAlpha(blockItem.getBlock().getDefaultMapColor().color, 1.0f), 1.0f);
                MatrixStack matrices = drawContext.getMatrices();
                int itemX = 7;
                int itemY = 6;
                matrices.push();
                matrices.translate(i + 8, i2 - 84, 1000.0f);
                GlStateManager._enableBlend();
                GlStateManager._disableDepthTest();
                Identifier texture = ProtectedTextureRegistry.get("textures/container.png");
                drawContext.drawTexture(RenderLayer::getGuiTextured, texture,
                        0, 0, 0.0f, 0.0f, 176, 67, 176, 67, iMultBright);
                if (containerStacks.size() > 27) {
                    drawContext.enableScissor(0, -18, 176, 5);
                    drawContext.drawTexture(RenderLayer::getGuiTextured, texture,
                            0, -18, 0.0f, 0.0f, 176, 67, 176, 67, iMultBright);
                    drawContext.disableScissor();
                    itemY = -12;
                }
                for (ItemStack itemStack : containerStacks) {
                    drawContext.drawItem(itemStack, itemX, itemY);
                    drawContext.drawStackOverlay(this.textRenderer, itemStack, itemX, itemY);
                    itemX += 18;
                    if (itemX >= 165) {
                        itemY += 18;
                        itemX = 7;
                    }
                }
                GlStateManager._enableDepthTest();
                GlStateManager._disableBlend();
                matrices.pop();
            }
        }
    }

    @Unique
    public List<ItemStack> getContainerStacks(ItemStack itemStack) {
        ContainerComponent container = itemStack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            DefaultedList<ItemStack> slots = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.copyTo(slots);
            if (spectra$containsItems(slots)) {
                return new ArrayList<>(slots);
            }
        }

        NbtComponent blockEntityData = itemStack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            List<ItemStack> decoded = spectra$readItemList(blockEntityData.copyNbt(), "Items", 27);
            if (decoded != null && spectra$containsItems(decoded)) {
                return decoded;
            }
        }

        ArrayList<ItemStack> arrayList = new ArrayList<>();
        if (ServerUtil.isConnectedToServer("holyworld")) {
            NbtCompound nbtCompoundCopyNbt = ((NbtComponent) itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)).copyNbt();
            Optional.ofNullable(nbtCompoundCopyNbt.getList("backpack-inventory", 10)).ifPresent(nbtList -> {
                HashMap map = new HashMap();
                Iterator it = nbtList.iterator();
                while (it.hasNext()) {
                    NbtElement nbtElement = (NbtElement) it.next();
                    if (nbtElement instanceof NbtCompound) {
                        NbtCompound nbtCompound2 = (NbtCompound) nbtElement;
                        NbtCompound compound = nbtCompound2.getCompound("item");
                        map.put(Integer.valueOf(nbtCompound2.getInt("slot")), new ItemStack((ItemConvertible) Registries.ITEM.get(Identifier.of(compound.getString("id"))), compound.getInt("Count")));
                    }
                }
                if (map.isEmpty()) {
                    return;
                }
                IntStream.range(0, Math.max(maxBackPackSlots(nbtCompoundCopyNbt), 27)).forEach(i -> {
                    arrayList.add((ItemStack) map.getOrDefault(Integer.valueOf(i), Items.AIR.getDefaultStack()));
                });
            });
        }
        return arrayList;
    }

    @Unique
    private boolean spectra$containsItems(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private List<ItemStack> spectra$readItemList(NbtCompound root, String key, int minimumSize) {
        if (root == null || !root.contains(key, NbtElement.LIST_TYPE)) {
            return null;
        }
        NbtList itemList = root.getList(key, NbtElement.COMPOUND_TYPE);
        int size = minimumSize;
        for (NbtElement element : itemList) {
            if (element instanceof NbtCompound compound) {
                size = Math.max(size, (compound.getByte("Slot") & 255) + 1);
            }
        }
        DefaultedList<ItemStack> result = DefaultedList.ofSize(size, ItemStack.EMPTY);
        for (NbtElement element : itemList) {
            if (!(element instanceof NbtCompound compound)) {
                continue;
            }
            int slot = compound.getByte("Slot") & 255;
            if (slot < 0 || slot >= result.size()) {
                continue;
            }
            ItemStack.fromNbt(Mc.INSTANCE.getWorld().getRegistryManager(), compound)
                    .ifPresent(stack -> result.set(slot, stack));
        }
        return new ArrayList<>(result);
    }

    @Unique
    public int maxBackPackSlots(NbtCompound nbtCompound) {
        switch (nbtCompound.getCompound("PublicBukkitValues").getString("litebackpacks:backpack").replace("\"", "")) {
            case "infinity":
                return 36;
            case "huge":
                return 27;
            case "big":
                return 21;
            case "normal":
                return 15;
            case "mini":
                return 9;
            default:
                return -1;
        }
    }
}
