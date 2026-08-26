package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class EmcConvertLiquefierBlock extends MachineBlock {
    @Override
    protected int guiId() {
        return 1;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new EmcConvertLiquefierBlockEntity();
    }
}
