package cn.gbk.emcfluid.content.blockentity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class MachineTileEntity extends TileEntity {
    protected static void sanitizeItemHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!handler.isItemValid(slot, stack)) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
                continue;
            }
            int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            if (limit <= 0) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            } else if (stack.getCount() > limit) {
                ItemStack clamped = stack.copy();
                clamped.setCount(limit);
                handler.setStackInSlot(slot, clamped);
            }
        }
    }

    protected static void sanitizeFluidTank(FluidTank tank) {
        FluidStack fluid = tank.getFluid();
        if (fluid == null) {
            return;
        }
        if (fluid.amount <= 0) {
            tank.setFluid(null);
        } else if (fluid.amount > tank.getCapacity()) {
            FluidStack clamped = fluid.copy();
            clamped.amount = tank.getCapacity();
            tank.setFluid(clamped);
        }
    }

    protected void markDirtyAndNotify() {
        markDirty();
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    public List<ItemStackHandler> getDroppableItemHandlers() {
        return Collections.emptyList();
    }

    public void dropContents() {
        if (world == null || world.isRemote) {
            return;
        }
        for (ItemStackHandler handler : getDroppableItemHandlers()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    ItemStack dropped = handler.extractItem(slot, stack.getCount(), false);
                    EntityItem entity = new EntityItem(
                            world,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            dropped
                    );
                    entity.setDefaultPickupDelay();
                    world.spawnEntity(entity);
                }
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
}
