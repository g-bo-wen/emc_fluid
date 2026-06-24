package cn.gbk.emcfluid.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.integration.EmcCrafterIntegration;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmcCrafterAe2Integration implements EmcCrafterIntegration, IInWorldGridNodeHost, IActionHost, ICraftingProvider {
    private static final int OUTPUT_FLUSH_DELAY_TICKS = 1;

    private final EmcCrafterBlockEntity owner;
    private final LazyOptional<IInWorldGridNodeHost> hostCapability = LazyOptional.of(() -> this);
    private final IManagedGridNode aeNode = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
            .setInWorldNode(true)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setIdlePowerUsage(0)
            .setVisualRepresentation(ModContent.EMC_CRAFTER_ITEM.get())
            .addService(ICraftingProvider.class, this);

    public EmcCrafterAe2Integration(EmcCrafterBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public void clearRemoved() {
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            GridHelper.onFirstTick(owner, ignored -> createAeNode());
        }
    }

    private void createAeNode() {
        if (!aeNode.isReady() && owner.getLevel() != null && !owner.getLevel().isClientSide) {
            aeNode.create(owner.getLevel(), owner.getBlockPos());
        }
    }

    @Override
    public void onChunkUnloaded() {
        aeNode.destroy();
    }

    @Override
    public void setRemoved(boolean chunkUnloaded) {
        aeNode.destroy();
    }

    @Override
    public void invalidateCaps() {
        hostCapability.invalidate();
    }

    @Override
    public void load(CompoundTag tag) {
        aeNode.loadFromNBT(tag.getCompound("AeNode"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        CompoundTag aeTag = new CompoundTag();
        aeNode.saveToNBT(aeTag);
        tag.put("AeNode", aeTag);
    }

    @Override
    public void refreshCraftingProviders() {
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            ICraftingProvider.requestUpdate(aeNode);
        }
    }

    @Override
    public ItemStack insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var grid = aeNode.getGrid();
        AEItemKey outputKey = AEItemKey.of(stack);
        if (grid == null || outputKey == null) {
            return stack;
        }
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);
        long accepted = storage.insert(outputKey, stack.getCount(), Actionable.SIMULATE, source);
        if (accepted <= 0) {
            return stack;
        }
        long inserted = storage.insert(outputKey, Math.min(accepted, stack.getCount()), Actionable.MODULATE, source);
        ItemStack remaining = stack.copy();
        remaining.shrink(Math.toIntExact(inserted));
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return hostCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return owner.getTargets().stream().map(EmcAePattern::new).map(IPatternDetails.class::cast).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!(patternDetails instanceof EmcAePattern pattern) || inputHolder.length == 0 || aeNode.getNode() == null) {
            return false;
        }
        if (!isTargetStillAuthorized(pattern.target())) {
            return false;
        }
        List<EmcFluidInput> expectedInputs = pattern.target().fluidInputs();
        if (inputHolder.length < expectedInputs.size()) {
            return false;
        }
        for (int i = 0; i < expectedInputs.size(); i++) {
            EmcFluidInput expected = expectedInputs.get(i);
            long amount = inputHolder[i].get(AEFluidKey.of(expected.fluid()));
            if (amount < expected.amount()) {
                return false;
            }
        }
        return owner.queueOutput(pattern.target().output().copy(), OUTPUT_FLUSH_DELAY_TICKS);
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean isTargetStillAuthorized(EmcCraftingTarget target) {
        if (ProjectEAccess.getEmcValue(target.info()) != target.emcValue()) {
            return false;
        }
        return owner.getTargets().stream().anyMatch(current -> current.info().equals(target.info())
                && current.emcValue() == target.emcValue()
                && current.fluidInputs().equals(target.fluidInputs())
                && current.tierConfigHash() == target.tierConfigHash());
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return aeNode.getNode();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return aeNode.getNode();
    }

    private enum NodeListener implements IGridNodeListener<EmcCrafterAe2Integration> {
        INSTANCE;

        @Override
        public void onSaveChanges(EmcCrafterAe2Integration integration, IGridNode node) {
            integration.owner.setChanged();
        }
    }
}
