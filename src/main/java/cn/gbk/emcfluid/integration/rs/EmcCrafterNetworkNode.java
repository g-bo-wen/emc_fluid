package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.block.MachineBlock;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.node.ConnectivityStateChangeCause;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class EmcCrafterNetworkNode extends NetworkNode implements ICraftingPatternContainer {
    public static final ResourceLocation ID = new ResourceLocation(EmcFluid.MODID, "emc_crafter");
    private static final String NBT_UUID = "CrafterUuid";
    private static final String NBT_RS_OUTPUT_CACHE = "RsOutputCache";
    private static final String NBT_RS_OUTPUT_FLUSH_DELAY = "RsOutputFlushDelay";

    private final ItemStackHandler rsOutputCache = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markNodeDirty();
        }
    };
    private UUID uuid;
    private int rsOutputFlushDelay;
    @Nullable
    private EmcCraftingTarget resolvedTarget;

    public EmcCrafterNetworkNode(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Override
    public int getEnergyUsage() {
        return 0;
    }

    @Override
    public ItemStack getItemStack() {
        return ModContent.EMC_CRAFTER_ITEM.get().getDefaultInstance();
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        super.write(tag);
        tag.putUUID(NBT_UUID, getUuid());
        tag.put(NBT_RS_OUTPUT_CACHE, rsOutputCache.serializeNBT());
        tag.putInt(NBT_RS_OUTPUT_FLUSH_DELAY, rsOutputFlushDelay);
        return tag;
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.hasUUID(NBT_UUID)) {
            uuid = tag.getUUID(NBT_UUID);
        }
        if (tag.contains(NBT_RS_OUTPUT_CACHE)) {
            rsOutputCache.deserializeNBT(tag.getCompound(NBT_RS_OUTPUT_CACHE));
        }
        rsOutputFlushDelay = tag.getInt(NBT_RS_OUTPUT_FLUSH_DELAY);
    }

    @Override
    public void update() {
        super.update();
        flushRsOutputCache();
    }

    @Override
    protected void onConnectedStateChange(INetwork network, boolean state, ConnectivityStateChangeCause cause) {
        network.getCraftingManager().invalidate();
    }

    @Override
    public List<ICraftingPattern> getPatterns() {
        EmcCrafterBlockEntity crafter = getCrafter();
        if (crafter == null) {
            return List.of();
        }
        ItemStack patternStack = crafter.getItems().getStackInSlot(0).copy();
        return crafter.getTargets().stream()
                .map(target -> new EmcRsPattern(patternStack, target, this))
                .map(ICraftingPattern.class::cast)
                .toList();
    }

    public boolean canCacheOutput(ItemStack stack, int size) {
        EmcCrafterBlockEntity crafter = getCrafter();
        if (crafter == null) {
            return false;
        }
        ItemStack toCache = stack.copy();
        toCache.setCount(size);
        return crafter.insertIntoOutputCache(toCache, true);
    }

    public boolean cacheOutput(ItemStack stack) {
        EmcCrafterBlockEntity crafter = getCrafter();
        return crafter != null && crafter.insertIntoOutputCache(stack, false);
    }

    void rememberResolvedTarget(EmcCraftingTarget target) {
        resolvedTarget = target;
    }

    @Nullable
    private EmcCrafterBlockEntity getCrafter() {
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof EmcCrafterBlockEntity crafter ? crafter : null;
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

    @Override
    public boolean hasConnectedFluidInventory() {
        return true;
    }

    @Override
    public boolean insertFluidsIntoInventory(Collection<FluidStack> fluids, Action action) {
        if (fluids.isEmpty()) {
            return true;
        }
        EmcCraftingTarget target = findTargetForFluids(fluids);
        if (target == null) {
            return false;
        }
        ItemStack output = target.output().copy();
        boolean accepted = insertIntoRsOutputCache(output, action == Action.SIMULATE);
        if (accepted && action == Action.PERFORM) {
            rsOutputFlushDelay = Math.max(rsOutputFlushDelay, 1);
            resolvedTarget = null;
            markNodeDirty();
        }
        return accepted;
    }

    @Nullable
    @Override
    public BlockEntity getConnectedBlockEntity() {
        return null;
    }

    @Override
    public BlockEntity getFacingBlockEntity() {
        Direction direction = getDirection();
        return level.getBlockEntity(pos.relative(direction));
    }

    @Override
    public Direction getDirection() {
        if (level != null && level.getBlockState(pos).hasProperty(MachineBlock.FACING)) {
            return level.getBlockState(pos).getValue(MachineBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Nullable
    @Override
    public IItemHandlerModifiable getPatternInventory() {
        EmcCrafterBlockEntity crafter = getCrafter();
        return crafter == null ? null : crafter.getItems();
    }

    @Override
    public Component getName() {
        return Component.translatable("container.emcfluid.emc_crafter");
    }

    @Override
    public BlockPos getPosition() {
        return pos;
    }

    @Override
    public ICraftingPatternContainer getRootContainer() {
        return this;
    }

    @Override
    public UUID getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
            markDirty();
        }
        return uuid;
    }

    @Override
    public void unlock() {
    }

    private void flushRsOutputCache() {
        if (rsOutputFlushDelay > 0) {
            rsOutputFlushDelay--;
            markNodeDirty();
            return;
        }
        if (network == null || !network.canRun()) {
            return;
        }
        for (int i = 0; i < rsOutputCache.getSlots(); i++) {
            ItemStack stack = rsOutputCache.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            int untracked = network.getCraftingManager().track(stack, stack.getCount());
            if (untracked <= 0) {
                rsOutputCache.setStackInSlot(i, ItemStack.EMPTY);
                continue;
            }
            ItemStack toInsert = stack.copy();
            toInsert.setCount(untracked);
            ItemStack remainder = network.insertItem(toInsert, toInsert.getCount(), Action.PERFORM);
            rsOutputCache.setStackInSlot(i, remainder);
        }
    }

    private boolean insertIntoRsOutputCache(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < rsOutputCache.getSlots() && !remaining.isEmpty(); i++) {
            remaining = rsOutputCache.insertItem(i, remaining, simulate);
        }
        return remaining.isEmpty();
    }

    @Nullable
    private EmcCraftingTarget findTargetForFluids(Collection<FluidStack> fluids) {
        int[] amounts = fluidAmountsByTier(fluids);
        if (amounts == null) {
            return null;
        }
        if (resolvedTarget != null && matchesFluidInputs(resolvedTarget, amounts)) {
            return resolvedTarget;
        }

        EmcCrafterBlockEntity crafter = getCrafter();
        if (crafter == null) {
            return null;
        }
        EmcCraftingTarget matched = null;
        for (EmcCraftingTarget target : crafter.getTargets()) {
            if (!matchesFluidInputs(target, amounts)) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = target;
        }
        return matched;
    }

    @Nullable
    private int[] fluidAmountsByTier(Collection<FluidStack> fluids) {
        int[] amounts = new int[EmcFluidTierConfig.MAX_TIERS];
        boolean hasFluid = false;
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) {
                continue;
            }
            int tier = EmcFluidTierConfig.tierOf(fluid.getFluid());
            if (!EmcFluidTierConfig.isEnabledTier(tier)) {
                return null;
            }
            long amount = (long) amounts[tier] + fluid.getAmount();
            if (amount > Integer.MAX_VALUE) {
                return null;
            }
            amounts[tier] = (int) amount;
            hasFluid = true;
        }
        return hasFluid ? amounts : null;
    }

    private boolean matchesFluidInputs(EmcCraftingTarget target, int[] actualAmounts) {
        int[] expectedAmounts = new int[EmcFluidTierConfig.MAX_TIERS];
        for (EmcFluidInput input : target.fluidInputs()) {
            long amount = (long) expectedAmounts[input.tierIndex()] + input.amount();
            if (amount > Integer.MAX_VALUE) {
                return false;
            }
            expectedAmounts[input.tierIndex()] = (int) amount;
        }
        for (int i = 0; i < EmcFluidTierConfig.MAX_TIERS; i++) {
            if (expectedAmounts[i] != actualAmounts[i]) {
                return false;
            }
        }
        return true;
    }

    private void markNodeDirty() {
        if (level != null && !level.isClientSide) {
            markDirty();
        }
    }
}
