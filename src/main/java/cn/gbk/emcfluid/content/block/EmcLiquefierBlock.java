package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class EmcLiquefierBlock extends MachineBlock {
    @Override
    protected int guiId() {
        return 0;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new EmcLiquefierBlockEntity();
    }
}
