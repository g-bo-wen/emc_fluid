package cn.gbk.emcfluid.integration.rs;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.refinedmods.refinedstorage.api.network.INetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

class RestoredPatternContainer implements ICraftingPatternContainer {
    private final INetwork network;
    private final UUID uuid = UUID.randomUUID();

    RestoredPatternContainer(INetwork network) {
        this.network = network;
    }

    @Nullable
    @Override
    public IItemHandler getConnectedInventory() {
        return null;
    }

    @Nullable
    @Override
    public IFluidHandler getConnectedFluidInventory() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity getConnectedBlockEntity() {
        return null;
    }

    @Override
    public BlockEntity getFacingBlockEntity() {
        return null;
    }

    @Override
    public Direction getDirection() {
        return Direction.NORTH;
    }

    @Override
    public List<ICraftingPattern> getPatterns() {
        return List.of();
    }

    @Nullable
    @Override
    public IItemHandlerModifiable getPatternInventory() {
        return null;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.emcfluid.emc_crafter");
    }

    @Override
    public BlockPos getPosition() {
        return network.getPosition();
    }

    @Override
    public ICraftingPatternContainer getRootContainer() {
        return this;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void unlock() {
    }
}
