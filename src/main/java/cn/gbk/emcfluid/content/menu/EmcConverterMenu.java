package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class EmcConverterMenu extends Container {
    private final EmcConverterBlockEntity tile;
    private final int[] data = new int[6];

    public EmcConverterMenu(InventoryPlayer inventory, EmcConverterBlockEntity tile) {
        this.tile = tile;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 110 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 168));
        }
        refreshData();
    }

    private int readData(int index) {
        switch (index) {
            case 0: return tile.getInputAmount();
            case 1: return tile.getOutputAmount();
            case 2: return tile.getMode().ordinal();
            case 3: return tile.getInputTier() + 1;
            case 4: return tile.getOutputTier() + 1;
            case 5: return EmcFluidTierConfig.enabledTiers();
            default: return 0;
        }
    }

    private void refreshData() {
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

    public BlockPos getBlockPos() { return tile.getPos(); }
    public int getRedAmount() { return data[0]; }
    public int getBlueAmount() { return data[1]; }
    public int getModeId() { return data[2]; }
    public int getRedTier() { return data[3]; }
    public int getBlueTier() { return data[4]; }
    public int getEnabledTiers() { return data[5]; }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }
}
