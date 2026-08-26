package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class EmcCrafterNetworkNode extends NetworkNode implements ICraftingPatternContainer {
    private static final String NBT_UUID = "CrafterUuid";
    private static final String NBT_OUTPUT_CACHE = "RsOutputCache";

    private final ItemStackHandler outputCache = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };
    private final ItemStackHandler emptyItemInput = new ItemStackHandler(0);
    private final IFluidHandler craftingFluidHandler = new CraftingFluidHandler();

    private UUID uuid;
    @Nullable
    private EmcRsPattern selectedPattern;
    @Nullable
    private EmcRsPattern acceptingPattern;
    private int[] acceptedAmounts;

    EmcCrafterNetworkNode(World world, BlockPos pos) {
        super(world, pos);
    }

    @Override
    public int getEnergyUsage() {
        return 0;
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(ModContent.emcCrafter);
    }

    @Override
    public String getId() {
        return RsIntegration.NODE_ID;
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        super.write(tag);
        tag.setUniqueId(NBT_UUID, getUuid());
        tag.setTag(NBT_OUTPUT_CACHE, outputCache.serializeNBT());
        return tag;
    }

    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);
        if (tag.hasUniqueId(NBT_UUID)) {
            uuid = tag.getUniqueId(NBT_UUID);
        }
        if (tag.hasKey(NBT_OUTPUT_CACHE)) {
            outputCache.deserializeNBT(tag.getCompoundTag(NBT_OUTPUT_CACHE));
        }
    }

    @Override
    protected void onConnectedStateChange(INetwork network, boolean state) {
        super.onConnectedStateChange(network, state);
        network.getCraftingManager().rebuild();
    }

    @Override
    public void onConnected(INetwork network) {
        super.onConnected(network);
        // NetworkNode assigns its network field after the state-change hook.
        // Rebuild once more with canUpdate() now able to observe the network.
        network.getCraftingManager().rebuild();
    }

    @Override
    public void onDisconnected(INetwork network) {
        super.onDisconnected(network);
        List<UUID> toCancel = new ArrayList<UUID>();
        for (ICraftingTask task : network.getCraftingManager().getTasks()) {
            ICraftingPattern pattern = task.getPattern();
            if (pattern != null && pattern.getContainer() != null
                    && pos.equals(pattern.getContainer().getPosition())) {
                toCancel.add(task.getId());
            }
        }
        for (UUID taskId : toCancel) {
            network.getCraftingManager().cancel(taskId);
        }
    }

    @Override
    public void update() {
        super.update();
        flushOutputCache();
    }

    @Override
    public List<ICraftingPattern> getPatterns() {
        EmcCrafterBlockEntity crafter = getCrafter();
        if (crafter == null) {
            return Collections.emptyList();
        }
        List<ICraftingPattern> patterns = new ArrayList<ICraftingPattern>();
        for (EmcCraftingTarget target : crafter.getTargets()) {
            patterns.add(new EmcRsPattern(target, this));
        }
        return patterns;
    }

    void selectPattern(EmcRsPattern pattern) {
        selectedPattern = pattern;
    }

    boolean isTargetAuthorized(EmcCraftingTarget expected) {
        EmcCrafterBlockEntity crafter = getCrafter();
        if (crafter == null || expected == null) {
            return false;
        }
        for (EmcCraftingTarget current : crafter.getTargets()) {
            if (sameTarget(current, expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameTarget(EmcCraftingTarget left, EmcCraftingTarget right) {
        return left.info().equals(right.info())
                && left.emcValue() == right.emcValue()
                && left.tierConfigHash() == right.tierConfigHash()
                && left.fluidInputs().equals(right.fluidInputs());
    }

    @Nullable
    private EmcCrafterBlockEntity getCrafter() {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof EmcCrafterBlockEntity
                ? (EmcCrafterBlockEntity) tile : null;
    }

    private boolean canCache(ItemStack stack) {
        return insertIntoCache(stack, true);
    }

    private boolean cache(ItemStack stack) {
        return insertIntoCache(stack, false);
    }

    private boolean insertIntoCache(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < outputCache.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = outputCache.insertItem(slot, remaining, simulate);
        }
        return remaining.isEmpty();
    }

    private void flushOutputCache() {
        if (network == null || !canUpdate()) {
            return;
        }
        for (int slot = 0; slot < outputCache.getSlots(); slot++) {
            ItemStack stack = outputCache.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack simulatedRemainder = network.insertItem(
                    stack, stack.getCount(), Action.SIMULATE);
            if (simulatedRemainder != null) {
                continue;
            }

            ItemStack remainder = network.insertItemTracked(stack, stack.getCount());
            outputCache.setStackInSlot(slot,
                    remainder == null ? ItemStack.EMPTY : remainder);
        }
    }

    void dropCachedOutputs() {
        if (world.isRemote) {
            return;
        }
        for (int slot = 0; slot < outputCache.getSlots(); slot++) {
            ItemStack stack = outputCache.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                EntityItem entity = new EntityItem(world,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                entity.setDefaultPickupDelay();
                world.spawnEntity(entity);
            }
        }
    }

    boolean hasCachedOutputs() {
        for (int slot = 0; slot < outputCache.getSlots(); slot++) {
            if (!outputCache.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IItemHandler getConnectedInventory() {
        // RS 1.6.16 probes the item destination even for a processing pattern
        // that has only fluid inputs. A zero-slot handler represents that
        // valid empty item channel without exposing a real inventory.
        return emptyItemInput;
    }

    @Nullable
    @Override
    public IFluidHandler getConnectedFluidInventory() {
        return craftingFluidHandler;
    }

    @Nullable
    @Override
    public TileEntity getConnectedTile() {
        return getCrafter();
    }

    @Override
    public TileEntity getFacingTile() {
        return getCrafter();
    }

    @Override
    public EnumFacing getDirection() {
        if (world.getBlockState(pos).getBlock() == ModContent.emcCrafter) {
            return world.getBlockState(pos).getValue(net.minecraft.block.BlockHorizontal.FACING);
        }
        return EnumFacing.NORTH;
    }

    @Nullable
    @Override
    public IItemHandlerModifiable getPatternInventory() {
        return null;
    }

    @Override
    public String getName() {
        return "tile.emcfluid.emc_crafter.name";
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
    public int getUpdateInterval() {
        return 0;
    }

    @Override
    public int getMaximumSuccessfulCraftingUpdates() {
        return 1;
    }

    private final class CraftingFluidHandler implements IFluidHandler {
        private final IFluidTankProperties[] noProperties = new IFluidTankProperties[0];

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return noProperties;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            EmcRsPattern pattern = selectedPattern;
            if (resource == null || resource.amount <= 0 || pattern == null
                    || !pattern.isAuthorized()) {
                return 0;
            }

            int tier = cn.gbk.emcfluid.util.EmcFluidTierConfig.tierOf(resource.getFluid());
            int expected = expectedAmount(pattern.target(), tier);
            if (expected <= 0) {
                return 0;
            }

            if (acceptingPattern == null || !sameTarget(
                    acceptingPattern.target(), pattern.target())) {
                if (doFill) {
                    acceptingPattern = pattern;
                    acceptedAmounts = new int[cn.gbk.emcfluid.util.EmcFluidTierConfig.MAX_TIERS];
                }
            }

            int alreadyAccepted = acceptingPattern != null && acceptedAmounts != null
                    && sameTarget(acceptingPattern.target(), pattern.target())
                    ? acceptedAmounts[tier] : 0;
            if (resource.amount > expected - alreadyAccepted) {
                return 0;
            }

            ItemStack output = pattern.target().output();
            if (!canCache(output)) {
                return 0;
            }
            if (!doFill) {
                return resource.amount;
            }

            acceptedAmounts[tier] += resource.amount;
            if (hasAllInputs(pattern.target(), acceptedAmounts)) {
                if (!cache(output)) {
                    throw new IllegalStateException(
                            "RS output cache rejected an output after successful simulation");
                }
                acceptingPattern = null;
                acceptedAmounts = null;
                selectedPattern = null;
            }
            return resource.amount;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }

        private int expectedAmount(EmcCraftingTarget target, int tier) {
            for (EmcFluidInput input : target.fluidInputs()) {
                if (input.tierIndex() == tier) {
                    return input.amount();
                }
            }
            return 0;
        }

        private boolean hasAllInputs(EmcCraftingTarget target, int[] received) {
            for (EmcFluidInput input : target.fluidInputs()) {
                if (received[input.tierIndex()] != input.amount()) {
                    return false;
                }
            }
            return true;
        }
    }
}
