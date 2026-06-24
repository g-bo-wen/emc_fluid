package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.block.MachineBlock;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.refinedmods.refinedstorage.api.network.INetwork;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EmcCrafterNetworkNode extends NetworkNode implements ICraftingPatternContainer {
    public static final ResourceLocation ID = new ResourceLocation(EmcFluid.MODID, "emc_crafter");
    private static final String NBT_UUID = "CrafterUuid";

    private UUID uuid;

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
        return tag;
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.hasUUID(NBT_UUID)) {
            uuid = tag.getUUID(NBT_UUID);
        }
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
}
