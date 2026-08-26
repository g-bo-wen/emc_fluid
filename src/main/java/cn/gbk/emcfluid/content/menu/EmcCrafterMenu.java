package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class EmcCrafterMenu extends Container {
    public static final int PATTERN_SLOT_X = 26;
    public static final int PATTERN_SLOT_Y = 35;

    private final EmcCrafterBlockEntity tile;

    public EmcCrafterMenu(InventoryPlayer inventory, EmcCrafterBlockEntity tile) {
        this.tile = tile;
        addSlotToContainer(new SlotItemHandler(tile.getPattern(), 0, PATTERN_SLOT_X, PATTERN_SLOT_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() == ModContent.knowledgePattern;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new SlotItemHandler(tile.getOutputCache(), col + row * 3, 98 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addPlayerInventory(inventory);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            moved = stack.copy();
            if (index == 0) {
                if (!mergeItemStack(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 10) {
                if (!mergeItemStack(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() == ModContent.knowledgePattern) {
                if (!mergeItemStack(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                if (!mergeItemStack(stack, 37, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(stack, 10, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return moved;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    private void addPlayerInventory(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }
}
