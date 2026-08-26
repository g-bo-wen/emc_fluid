package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class EmcCrafterBlock extends MachineBlock {
    @Override
    protected int guiId() {
        return 2;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return EmcCrafterIntegrations.createCrafterTileEntity();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof EmcCrafterBlockEntity) {
            ((EmcCrafterBlockEntity) tile).onBlockBroken();
        }
        super.breakBlock(world, pos, state);
    }
}
