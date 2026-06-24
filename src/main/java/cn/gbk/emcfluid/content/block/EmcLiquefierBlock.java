package cn.gbk.emcfluid.content.block;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EmcLiquefierBlock extends MachineBlock {
    public EmcLiquefierBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmcLiquefierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : ticker(type, ModContent.EMC_LIQUEFIER_BE.get(), EmcLiquefierBlockEntity::serverTick);
    }
}
