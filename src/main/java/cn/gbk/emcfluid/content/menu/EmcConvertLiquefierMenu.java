package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.SlotItemHandler;

public class EmcConvertLiquefierMenu extends EmcLiquefierMenu {
    private final EmcConvertLiquefierBlockEntity convertTile;

    public EmcConvertLiquefierMenu(InventoryPlayer inventory, EmcConvertLiquefierBlockEntity tile) {
        super(tile, 4);
        this.convertTile = tile;
        addSlotToContainer(new SlotItemHandler(tile.getItems(), 0, 44, 35));
        addPlayerInventory(inventory, 84, 142);
        refreshData();
    }

    @Override
    protected int readData(int index) {
        if (index < 2) {
            return super.readData(index);
        }
        return index == 2 ? convertTile.getSelectedTier() : EmcFluidTierConfig.enabledTiers();
    }

    public int getSelectedTier() {
        return data[2];
    }

    public int getEnabledTiers() {
        return data[3];
    }
}
