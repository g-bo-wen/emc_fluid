package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EmcCrafterBlock extends MachineBlock {
    public EmcCrafterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmcCrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : ticker(type, ModContent.EMC_CRAFTER_BE.get(), EmcCrafterBlockEntity::serverTick);
    }
}
