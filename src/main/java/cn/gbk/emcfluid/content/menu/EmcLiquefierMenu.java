package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

public class EmcLiquefierMenu extends Container {
    protected final EmcLiquefierBlockEntity tile;
    protected final int[] data;

    public EmcLiquefierMenu(InventoryPlayer inventory, EmcLiquefierBlockEntity tile) {
        this(tile, 2);
        addSlotToContainer(new SlotItemHandler(tile.getItems(), 0, 44, 35));
        addPlayerInventory(inventory, 84, 142);
        refreshData();
    }

    protected EmcLiquefierMenu(EmcLiquefierBlockEntity tile, int dataSize) {
        this.tile = tile;
        this.data = new int[dataSize];
    }

    protected void addPlayerInventory(InventoryPlayer inventory, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, hotbarY));
        }
    }

    protected int readData(int index) {
        if (index == 0) {
            return tile.getFluidAmount();
        }
        return index == 1 ? tile.getMode().ordinal() : 0;
    }

    protected void refreshData() {
        for (int i = 0; i < data.length; i++) {
            data[i] = readData(i);
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        for (int i = 0; i < data.length; i++) {
            listener.sendWindowProperty(this, i, readData(i));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < data.length; i++) {
            int value = readData(i);
            if (value != data[i]) {
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, i, value);
                }
                data[i] = value;
            }
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id >= 0 && id < data.length) {
            data[id] = value;
        }
    }

    public int getFluidAmount() {
        return data[0];
    }

    public EmcLiquefierBlockEntity.Mode getMode() {
        return EmcLiquefierBlockEntity.Mode.byId(data[1]);
    }

    public BlockPos getBlockPos() {
        return tile.getPos();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            moved = stack.copy();
            if (index == 0) {
                if (!mergeItemStack(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ProjectEAccess.isEmcHolder(stack)) {
                if (!mergeItemStack(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 28) {
                if (!mergeItemStack(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(stack, 1, 28, false)) {
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
}
