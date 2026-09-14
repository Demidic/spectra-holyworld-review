package ru.spectra.client.ui;

import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.type.Mc;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.WeightedEngine;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class InventoryHudWidget extends Draggable {
    private static final float WIDTH = 420.0f;
    private static final float HEIGHT = 180.0f;
    private static final float SLOT_WIDTH = 39.0f;
    private static final float SLOT_HEIGHT = 38.0f;
    private static final float SLOT_GAP = 6.125f;
    private final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont countFont = Fonts.INTER_SEMIBOLD.get();
    private final ItemStack[] displayedStacks = new ItemStack[27];
    private final List<ItemStack> previewStacks = List.of(
            Items.NETHERITE_HELMET.getDefaultStack(),
            Items.NETHERITE_CHESTPLATE.getDefaultStack(),
            Items.NETHERITE_LEGGINGS.getDefaultStack(),
            Items.NETHERITE_BOOTS.getDefaultStack(),
            Items.NETHERITE_SWORD.getDefaultStack(),
            Items.GOLDEN_APPLE.getDefaultStack(),
            Items.ENDER_PEARL.getDefaultStack(),
            Items.TOTEM_OF_UNDYING.getDefaultStack(),
            Items.EXPERIENCE_BOTTLE.getDefaultStack()
    );

    public InventoryHudWidget(BooleanSupplier visibility) {
        super("Inventory", visibility);
        this.x = 35.0f;
        this.y = 942.0f;
    }

    @Override
    public void layout(DragRenderContext context) {
    }

    @Override
    public void render(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }

        DrawEngine draw = context.drawEngine();
        MatrixStack matrices = context.matrixStack();
        SpectraHudStyle.panel(draw, matrices, this.x, this.y, WIDTH, HEIGHT, SpectraHudStyle.RADIUS);
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.titleFont, "Inventory",
                this.x + 10.0f, this.y + 13.0f, 13.0f, 0.05f, SpectraHudStyle.TEXT);

        ItemStack[] stacks = inventoryStacks();
        float startX = this.x + 10.0f;
        float startY = this.y + 42.0f;
        for (int index = 0; index < 27; index++) {
            int column = index % 9;
            int row = index / 9;
            float slotX = startX + column * (SLOT_WIDTH + SLOT_GAP);
            float slotY = startY + row * (SLOT_HEIGHT + SLOT_GAP);
            SpectraHudStyle.outlinedRect(
                    draw, matrices, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT, 8.0f,
                    SpectraHudStyle.BORDER,
                    SpectraHudStyle.panelColor(draw.colorStack(), SpectraHudStyle.PANEL)
            );
            ItemStack stack = stacks[index];
            if (!stack.isEmpty()) {
                draw.itemStack(matrices.peek().getPositionMatrix(), stack, slotX + 10.5f, slotY + 10.0f, 0.5625f, 1.0f);
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    float countX = slotX + SLOT_WIDTH - 3.0f - this.countFont.getWidth(count, 9.0f);
                    float countY = slotY + SLOT_HEIGHT - 3.0f - this.countFont.getHeight(9.0f);
                    draw.roundedRectangle(
                            matrices.peek().getPositionMatrix(),
                            countX - 2.0f,
                            countY - 1.0f,
                            this.countFont.getWidth(count, 9.0f) + 4.0f,
                            this.countFont.getHeight(9.0f) + 2.0f,
                            2.0f,
                            0x99000000
                    );
                    draw.msdfFont(matrices.peek().getPositionMatrix(), this.countFont, count, countX, countY, 9.0f, 0.0f, 0xFFFFFFFF);
                }
            }
        }
    }

    private ItemStack[] inventoryStacks() {
        Arrays.fill(this.displayedStacks, ItemStack.EMPTY);
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            for (int index = 9; index <= 35; index++) {
                this.displayedStacks[index - 9] = player.getInventory().getStack(index);
            }
        }
        boolean empty = true;
        for (ItemStack stack : this.displayedStacks) {
            if (!stack.isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty && HudEditorScreen.isEditing()) {
            for (int index = 0; index < this.previewStacks.size(); index++) {
                this.displayedStacks[index] = this.previewStacks.get(index);
            }
        }
        return this.displayedStacks;
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return hovered && !input.intercepted();
    }

    @Override
    public void animate(WeightedEngine engine) {
    }

    @Override
    public void update() {
    }

    @Override
    public float width() {
        return WIDTH;
    }

    @Override
    public float height() {
        return HEIGHT;
    }
}
