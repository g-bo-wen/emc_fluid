package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class EmcConverterBlock extends MachineBlock {
    @Override
    protected int guiId() {
        return 3;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new EmcConverterBlockEntity();
    }
}
