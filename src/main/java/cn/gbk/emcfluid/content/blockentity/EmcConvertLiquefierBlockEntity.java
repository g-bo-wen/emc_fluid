package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
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

public class EmcConvertLiquefierBlockEntity extends BlockEntity implements MenuProvider {
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
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, this::isSelectedFluid) {
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
                case 2 -> selectedTier;
                case 3 -> EmcFluidTierConfig.enabledTiers();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) {
                mode = EmcLiquefierBlockEntity.Mode.byId(value);
            } else if (index == 2) {
                selectedTier = clampTier(value);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private EmcLiquefierBlockEntity.Mode mode = EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID;
    private int selectedTier;

    public EmcConvertLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.EMC_CONVERT_LIQUEFIER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EmcConvertLiquefierBlockEntity blockEntity) {
        blockEntity.convert();
    }

    private void convert() {
        selectedTier = clampTier(selectedTier);
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        long tierValue = EmcFluidTierConfig.value(selectedTier);
        if (mode == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID) {
            int room = tank.getCapacity() - tank.getFluidAmount();
            if (room <= 0 || tierValue > Integer.MAX_VALUE) {
                return;
            }
            long maxMb = Math.min(room, Long.MAX_VALUE / tierValue);
            long extractable = ProjectEAccess.extractEmc(stack, maxMb * tierValue, false);
            int produced = Math.toIntExact(extractable / tierValue);
            if (produced > 0) {
                ProjectEAccess.extractEmc(stack, produced * tierValue, true);
                tank.fill(new FluidStack(ModContent.getEmcFluidSource(selectedTier).get(), produced), IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        } else {
            int available = tank.getFluidAmount();
            if (available <= 0 || tierValue > Long.MAX_VALUE / available) {
                return;
            }
            long acceptable = ProjectEAccess.insertEmc(stack, available * tierValue, false);
            int drained = Math.toIntExact(acceptable / tierValue);
            if (drained > 0) {
                ProjectEAccess.insertEmc(stack, drained * tierValue, true);
                tank.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        }
    }

    public void toggleMode() {
        mode = mode == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID
                ? EmcLiquefierBlockEntity.Mode.FLUID_TO_EMC
                : EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID;
        setChanged();
    }

    public void changeTier(int delta) {
        int newTier = clampTier(selectedTier + delta);
        if (newTier == selectedTier || !tank.isEmpty()) {
            return;
        }
        selectedTier = newTier;
        setChanged();
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
        tag.putInt("SelectedTier", selectedTier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        selectedTier = clampTier(tag.getInt("SelectedTier"));
        tank.readFromNBT(tag.getCompound("Tank"));
        mode = EmcLiquefierBlockEntity.Mode.byId(tag.getInt("Mode"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.emcfluid.emc_convert_liquefier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EmcConvertLiquefierMenu(containerId, inventory, this, data);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    private boolean isSelectedFluid(FluidStack stack) {
        return stack.getFluid() == ModContent.getEmcFluidSource(selectedTier).get();
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

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(tier, EmcFluidTierConfig.enabledTiers() - 1));
    }
}
