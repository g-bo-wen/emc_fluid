package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class EmcLiquefierBlockEntity extends MachineTileEntity implements ITickable {
    public static final int TANK_CAPACITY = 10_000;

    protected final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ProjectEAccess.isEmcHolder(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndNotify();
        }
    };
    protected final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == getAcceptedFluid() && getEmcPerMb() > 0L;
        }

        @Override
        protected void onContentsChanged() {
            markDirtyAndNotify();
        }
    };

    protected Mode mode = Mode.EMC_TO_FLUID;

    public EmcLiquefierBlockEntity() {
        tank.setTileEntity(this);
    }

    protected net.minecraftforge.fluids.Fluid getAcceptedFluid() {
        return ModContent.getEmcFluid(0);
    }

    protected long getEmcPerMb() {
        return 1L;
    }

    protected boolean isStoredFluidValid(FluidStack fluid) {
        return fluid != null && fluid.getFluid() == getAcceptedFluid();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        convert();
    }

    protected void convert() {
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        long tierValue = getEmcPerMb();
        if (tierValue <= 0L) {
            return;
        }

        if (mode == Mode.EMC_TO_FLUID) {
            int room = tank.getCapacity() - tank.getFluidAmount();
            long maxValue = room > 0 && tierValue <= Long.MAX_VALUE / room
                    ? (long) room * tierValue : Long.MAX_VALUE;
            long extractable = ProjectEAccess.extractEmc(stack, maxValue, false);
            int amount = (int) Math.min(room, extractable / tierValue);
            if (amount <= 0 || tank.fill(new FluidStack(getAcceptedFluid(), amount), false) != amount) {
                return;
            }
            long requested = amount * tierValue;
            long extracted = ProjectEAccess.extractEmc(stack, requested, true);
            int produced = (int) Math.min(amount, extracted / tierValue);
            long unused = extracted - produced * tierValue;
            if (unused > 0L) {
                ProjectEAccess.insertEmc(stack, unused, true);
            }
            if (produced > 0) {
                tank.fill(new FluidStack(getAcceptedFluid(), produced), true);
            }
        } else {
            int available = tank.getFluidAmount();
            if (available <= 0 || tierValue > Long.MAX_VALUE / available) {
                return;
            }
            long acceptable = ProjectEAccess.insertEmc(stack, available * tierValue, false);
            int amount = (int) Math.min(available, acceptable / tierValue);
            if (amount <= 0) {
                return;
            }
            FluidStack drained = tank.drain(amount, true);
            if (drained == null || drained.amount <= 0) {
                return;
            }
            long inserted = ProjectEAccess.insertEmc(stack, drained.amount * tierValue, true);
            int consumed = (int) Math.min(drained.amount, inserted / tierValue);
            long partialValue = inserted - consumed * tierValue;
            if (partialValue > 0L) {
                ProjectEAccess.extractEmc(stack, partialValue, true);
            }
            if (consumed < drained.amount) {
                tank.fill(new FluidStack(getAcceptedFluid(), drained.amount - consumed), true);
            }
        }
    }

    public void toggleMode() {
        mode = mode == Mode.EMC_TO_FLUID ? Mode.FLUID_TO_EMC : Mode.EMC_TO_FLUID;
        markDirtyAndNotify();
    }

    public Mode getMode() {
        return mode;
    }

    public int getFluidAmount() {
        return tank.getFluidAmount();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    public List<ItemStackHandler> getDroppableItemHandlers() {
        return Collections.singletonList(items);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", items.serializeNBT());
        compound.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("Mode", mode.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        items.deserializeNBT(compound.getCompoundTag("Items"));
        tank.readFromNBT(compound.getCompoundTag("Tank"));
        sanitizeItemHandler(items);
        sanitizeFluidTank(tank);
        if (tank.getFluid() != null && !isStoredFluidValid(tank.getFluid())) {
            tank.setFluid(null);
        }
        mode = Mode.byId(compound.getInteger("Mode"));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) items;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) tank;
        }
        return super.getCapability(capability, facing);
    }

    public enum Mode {
        EMC_TO_FLUID,
        FLUID_TO_EMC;

        public static Mode byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMC_TO_FLUID;
        }
    }
}
