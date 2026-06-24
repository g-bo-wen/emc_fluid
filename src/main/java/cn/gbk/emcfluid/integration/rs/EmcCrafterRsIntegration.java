package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.integration.EmcCrafterIntegration;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeProxy;
import com.refinedmods.refinedstorage.capability.NetworkNodeProxyCapability;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class EmcCrafterRsIntegration implements EmcCrafterIntegration, INetworkNodeProxy<EmcCrafterNetworkNode> {
    private final EmcCrafterBlockEntity owner;
    private final LazyOptional<INetworkNodeProxy<EmcCrafterNetworkNode>> nodeCapability = LazyOptional.of(() -> this);
    private EmcCrafterNetworkNode clientNode;

    public EmcCrafterRsIntegration(EmcCrafterBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public void clearRemoved() {
        ensureRsNode();
    }

    @Override
    public void serverTick() {
        getNode().update();
    }

    @Override
    public void setRemoved(boolean chunkUnloaded) {
        Level level = owner.getLevel();
        if (!chunkUnloaded && level instanceof ServerLevel serverLevel && RsIntegration.API != null) {
            EmcCrafterNetworkNode node = getNode();
            var network = node.getNetwork();
            RsIntegration.API.getNetworkNodeManager(serverLevel).removeNode(owner.getBlockPos());
            RsIntegration.API.getNetworkNodeManager(serverLevel).markForSaving();
            if (network != null) {
                network.getNodeGraph().invalidate(com.refinedmods.refinedstorage.api.util.Action.PERFORM, network.getLevel(), network.getPosition());
            }
        }
    }

    @Override
    public void invalidateCaps() {
        nodeCapability.invalidate();
    }

    @Override
    public void refreshCraftingProviders() {
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            EmcCrafterNetworkNode node = getNode();
            if (node.getNetwork() != null) {
                node.getNetwork().getCraftingManager().invalidate();
            }
        }
    }

    @Override
    public ItemStack insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EmcCrafterNetworkNode node = getNode();
        if (node.getNetwork() == null) {
            return stack;
        }
        return node.getNetwork().insertItem(stack, stack.getCount(), com.refinedmods.refinedstorage.api.util.Action.PERFORM);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == NetworkNodeProxyCapability.NETWORK_NODE_PROXY_CAPABILITY) {
            return nodeCapability.cast();
        }
        return LazyOptional.empty();
    }

    private void ensureRsNode() {
        Level level = owner.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || RsIntegration.API == null) {
            return;
        }
        var manager = RsIntegration.API.getNetworkNodeManager(serverLevel);
        if (manager.getNode(owner.getBlockPos()) == null) {
            manager.setNode(owner.getBlockPos(), new EmcCrafterNetworkNode(level, owner.getBlockPos()));
            manager.markForSaving();
        }
    }

    @Override
    public EmcCrafterNetworkNode getNode() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide || RsIntegration.API == null) {
            if (clientNode == null) {
                clientNode = new EmcCrafterNetworkNode(level, owner.getBlockPos());
            }
            return clientNode;
        }
        var manager = RsIntegration.API.getNetworkNodeManager((ServerLevel) level);
        var node = manager.getNode(owner.getBlockPos());
        if (node instanceof EmcCrafterNetworkNode crafterNode) {
            return crafterNode;
        }
        EmcCrafterNetworkNode crafterNode = new EmcCrafterNetworkNode(level, owner.getBlockPos());
        manager.setNode(owner.getBlockPos(), crafterNode);
        manager.markForSaving();
        return crafterNode;
    }
}
