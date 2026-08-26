package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.integration.EmcCrafterIntegration;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeProxy;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class EmcCrafterRsIntegration
        implements EmcCrafterIntegration, INetworkNodeProxy<EmcCrafterNetworkNode> {
    private final EmcCrafterBlockEntity owner;
    private EmcCrafterNetworkNode clientNode;
    private boolean broken;

    public EmcCrafterRsIntegration(EmcCrafterBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public void onLoad() {
        broken = false;
        World world = owner.getWorld();
        if (world != null && !world.isRemote && RsIntegration.API != null) {
            getNode();
            RsIntegration.API.discoverNode(world, owner.getPos());
        }
    }

    @Override
    public void onChunkUnload() {
        // RS keeps nodes in its WorldSavedData while chunks are unloaded. The
        // node remains the single source of truth for cached outputs.
    }

    @Override
    public void onBlockBroken() {
        if (broken) {
            return;
        }
        broken = true;
        World world = owner.getWorld();
        if (world == null || world.isRemote || RsIntegration.API == null) {
            return;
        }

        INetworkNodeManager manager = RsIntegration.API.getNetworkNodeManager(world);
        INetworkNode existing = manager.getNode(owner.getPos());
        if (!(existing instanceof EmcCrafterNetworkNode)) {
            return;
        }
        EmcCrafterNetworkNode node = (EmcCrafterNetworkNode) existing;
        INetwork network = node.getNetwork();
        node.dropCachedOutputs();
        manager.removeNode(owner.getPos());
        manager.markForSaving();
        if (network != null) {
            network.getNodeGraph().invalidate(
                    Action.PERFORM, network.world(), network.getPosition());
        }
    }

    @Override
    public void invalidate() {
        // Block removal is handled before TileEntity invalidation so cached
        // output can be dropped at the exact machine position.
    }

    @Override
    public void refreshCraftingProviders() {
        World world = owner.getWorld();
        if (world == null || world.isRemote || RsIntegration.API == null) {
            return;
        }
        EmcCrafterNetworkNode node = getNode();
        if (node.getNetwork() != null) {
            node.getNetwork().getCraftingManager().rebuild();
        }
    }

    @Override
    public boolean isBusy() {
        World world = owner.getWorld();
        if (world == null || world.isRemote || RsIntegration.API == null) {
            return false;
        }
        EmcCrafterNetworkNode node = getNode();
        INetwork network = node.getNetwork();
        if (network == null) {
            return node.hasCachedOutputs();
        }
        if (node.hasCachedOutputs()) {
            return true;
        }
        for (ICraftingTask task : network.getCraftingManager().getTasks()) {
            if (task.getPattern() != null && task.getPattern().getContainer() != null
                    && owner.getPos().equals(task.getPattern().getContainer().getPosition())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return capability == CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY) {
            return (T) this;
        }
        return null;
    }

    @Override
    public EmcCrafterNetworkNode getNode() {
        World world = owner.getWorld();
        if (world == null) {
            throw new IllegalStateException("EMC Crafter has no world");
        }
        if (world.isRemote) {
            if (clientNode == null) {
                clientNode = new EmcCrafterNetworkNode(world, owner.getPos());
            }
            return clientNode;
        }
        if (RsIntegration.API == null) {
            throw new IllegalStateException("Refined Storage API is unavailable");
        }

        INetworkNodeManager manager = RsIntegration.API.getNetworkNodeManager(world);
        INetworkNode existing = manager.getNode(owner.getPos());
        if (existing instanceof EmcCrafterNetworkNode) {
            return (EmcCrafterNetworkNode) existing;
        }

        EmcCrafterNetworkNode node = new EmcCrafterNetworkNode(world, owner.getPos());
        manager.setNode(owner.getPos(), node);
        manager.markForSaving();
        return node;
    }
}
