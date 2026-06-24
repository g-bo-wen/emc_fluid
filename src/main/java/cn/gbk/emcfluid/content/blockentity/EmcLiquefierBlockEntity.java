package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmcLiquefierBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 10_000;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ProjectEAccess.isEmcHolder(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == ModContent.EMC_FLUID_T1_SOURCE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.getFluidAmount();
                case 1 -> mode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) {
                mode = Mode.byId(value);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private Mode mode = Mode.EMC_TO_FLUID;

    public EmcLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.EMC_LIQUEFIER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EmcLiquefierBlockEntity blockEntity) {
        blockEntity.convert();
    }

    private void convert() {
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        if (mode == Mode.EMC_TO_FLUID) {
            int room = tank.getCapacity() - tank.getFluidAmount();
            if (room <= 0) {
                return;
            }
            long extracted = ProjectEAccess.extractEmc(stack, room, true);
            if (extracted > 0) {
                tank.fill(new FluidStack(ModContent.EMC_FLUID_T1_SOURCE.get(), Math.toIntExact(extracted)), IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        } else {
            int available = tank.getFluidAmount();
            if (available <= 0) {
                return;
            }
            long accepted = ProjectEAccess.insertEmc(stack, available, true);
            if (accepted > 0) {
                tank.drain(Math.toIntExact(accepted), IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        }
    }

    public void toggleMode() {
        mode = mode == Mode.EMC_TO_FLUID ? Mode.FLUID_TO_EMC : Mode.EMC_TO_FLUID;
        setChanged();
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

    public ContainerData getData() {
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("Mode", mode.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        tank.readFromNBT(tag.getCompound("Tank"));
        mode = Mode.byId(tag.getInt("Mode"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.emcfluid.emc_liquefier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EmcLiquefierMenu(containerId, inventory, this, data);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    public enum Mode {
        EMC_TO_FLUID,
        FLUID_TO_EMC;

        public static Mode byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMC_TO_FLUID;
        }
    }
}
