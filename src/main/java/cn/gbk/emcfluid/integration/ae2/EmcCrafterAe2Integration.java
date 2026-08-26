package cn.gbk.emcfluid.integration.ae2;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Conditionally registered EMC Crafter TileEntity used only when the fixed
 * AE2 + AE2FC pair is installed. Keeping AE interfaces on this subclass lets
 * the normal TileEntity remain loadable when both optional mods are absent.
 */
public final class EmcCrafterAe2Integration extends EmcCrafterBlockEntity
        implements IGridProxyable, IActionHost, ICraftingProvider, ITickable {
    private static final String NODE_NBT = "EmcFluidAeNode";

    private final AENetworkProxy proxy;
    private final MachineSource actionSource;
    private boolean providerRefreshPending = true;

    public EmcCrafterAe2Integration() {
        proxy = new AENetworkProxy(this, NODE_NBT,
                new ItemStack(ModContent.emcCrafter), true);
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        proxy.setIdlePowerUsage(0.0D);
        actionSource = new MachineSource(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            providerRefreshPending = true;
            proxy.onReady();
        }
    }

    @Override
    public void onChunkUnload() {
        proxy.onChunkUnload();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        proxy.invalidate();
        super.invalidate();
    }

    @Override
    public void onBlockBroken() {
        proxy.invalidate();
        super.onBlockBroken();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound result = super.writeToNBT(compound);
        proxy.writeToNBT(result);
        return result;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        proxy.readFromNBT(compound);
    }

    @Override
    public void update() {
        if (world != null && !world.isRemote) {
            // A 1.12 TileEntity added late in a server tick can miss the
            // initial proxy-ready window when another integration rebuilds
            // its network in the same tick. Re-establishing a missing node is
            // idempotent and keeps the AE2 host recoverable from that ordering
            // as well as from delayed chunk-load callbacks.
            if (proxy.getNode() == null) {
                proxy.onReady();
            }
            if (providerRefreshPending && world.getMinecraftServer() != null
                    && world.getMinecraftServer().isServerRunning()) {
                providerRefreshPending = false;
                refreshCraftingProviders();
            }
            flushOutputCache();
        }
    }

    @Override
    public void refreshCraftingProviders() {
        super.refreshCraftingProviders();
        IGridNode node = proxy.getNode();
        if (node != null) {
            node.getGrid().postEvent(new MENetworkCraftingPatternChange(this, node));
        }
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (world == null || world.getMinecraftServer() == null
                || !world.getMinecraftServer().isServerRunning()) {
            providerRefreshPending = true;
            return;
        }
        // CraftingGridCache discovers providers while a newly joined grid is
        // commonly still booting and before channels are assigned. Publishing
        // is therefore independent of the current active state; pushPattern()
        // remains the execution-time guard for power/channel loss.
        for (EmcCraftingTarget target : getTargets()) {
            craftingTracker.addCraftingOption(this, new EmcAePattern(target));
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (!(patternDetails instanceof EmcAePattern) || !proxy.isActive()) {
            return false;
        }
        EmcCraftingTarget target = ((EmcAePattern) patternDetails).target();
        if (!isTargetStillAuthorized(target) || !hasExactFluidInputs(target, table)) {
            return false;
        }
        ItemStack output = target.output();
        if (!queueOutput(output, true)) {
            return false;
        }
        return queueOutput(output, false);
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || hasCachedOutputs();
    }

    @Override
    public AENetworkProxy getProxy() {
        return proxy;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return proxy.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        if (world != null && !world.isRemote) {
            world.destroyBlock(pos, true);
        }
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        IGridNode node = proxy.getNode();
        if (node == null) {
            throw new IllegalStateException("EMC Crafter AE2 node is not ready");
        }
        return node;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {
        markDirty();
    }

    private boolean hasExactFluidInputs(EmcCraftingTarget target, InventoryCrafting table) {
        long[] supplied = new long[EmcFluidTierConfig.MAX_TIERS];
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack stack = table.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            FluidStack fluid = EmcAePattern.fluidFromItem(stack);
            if (fluid == null || fluid.amount <= 0) {
                return false;
            }
            int tier = EmcFluidTierConfig.tierOf(fluid.getFluid());
            if (tier < 0 || tier >= supplied.length
                    || supplied[tier] > Long.MAX_VALUE - fluid.amount) {
                return false;
            }
            supplied[tier] += fluid.amount;
        }

        long[] expected = new long[EmcFluidTierConfig.MAX_TIERS];
        for (EmcFluidInput input : target.fluidInputs()) {
            expected[input.tierIndex()] += input.amount();
        }
        for (int tier = 0; tier < expected.length; tier++) {
            if (supplied[tier] != expected[tier]) {
                return false;
            }
        }
        return true;
    }

    private boolean isTargetStillAuthorized(EmcCraftingTarget target) {
        if (ProjectEAccess.getEmcValue(target.info()) != target.emcValue()) {
            return false;
        }
        for (EmcCraftingTarget current : getTargets()) {
            if (current.info().equals(target.info())
                    && current.emcValue() == target.emcValue()
                    && current.fluidInputs().equals(target.fluidInputs())
                    && current.tierConfigHash() == target.tierConfigHash()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCachedOutputs() {
        for (int slot = 0; slot < getOutputCache().getSlots(); slot++) {
            if (!getOutputCache().getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void flushOutputCache() {
        IGridNode node = proxy.getNode();
        if (node == null || !node.isActive()) {
            return;
        }
        IItemStorageChannel itemChannel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
        IStorageGrid storage = node.getGrid().getCache(IStorageGrid.class);
        IEnergyGrid energy = node.getGrid().getCache(IEnergyGrid.class);
        IMEMonitor<IAEItemStack> inventory = storage.getInventory(itemChannel);

        for (int slot = 0; slot < getOutputCache().getSlots(); slot++) {
            ItemStack stack = getOutputCache().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            IAEItemStack input = itemChannel.createStack(stack);
            if (input == null) {
                continue;
            }
            IAEItemStack remaining = AEApi.instance().storage().poweredInsert(
                    energy, inventory, input, actionSource, Actionable.MODULATE);
            if (remaining == null || remaining.getStackSize() <= 0L) {
                getOutputCache().setStackInSlot(slot, ItemStack.EMPTY);
            } else if (remaining.getStackSize() < stack.getCount()) {
                ItemStack remainder = stack.copy();
                remainder.setCount((int) remaining.getStackSize());
                getOutputCache().setStackInSlot(slot, remainder);
            }
        }
    }
}
