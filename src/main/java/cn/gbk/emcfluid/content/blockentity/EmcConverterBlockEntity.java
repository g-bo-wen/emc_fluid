package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.content.block.MachineBlock;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmcConverterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 10_000;

    private final FluidTank redTank = new FluidTank(TANK_CAPACITY, this::canFillRed) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank blueTank = new FluidTank(TANK_CAPACITY, stack -> EmcFluidTierConfig.isEnabledEmcFluid(stack.getFluid())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler redInputHandler = new RedInputFluidHandler();
    private final IFluidHandler redOutputHandler = new RedOutputFluidHandler();
    private final IFluidHandler blueOutputHandler = new BlueOutputFluidHandler();
    private final IFluidHandler combinedHandler = new CombinedFluidHandler();
    private final LazyOptional<IFluidHandler> redInputCapability = LazyOptional.of(() -> redInputHandler);
    private final LazyOptional<IFluidHandler> redOutputCapability = LazyOptional.of(() -> redOutputHandler);
    private final LazyOptional<IFluidHandler> blueOutputCapability = LazyOptional.of(() -> blueOutputHandler);
    private final LazyOptional<IFluidHandler> combinedCapability = LazyOptional.of(() -> combinedHandler);
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> redTank.getFluidAmount();
                case 1 -> blueTank.getFluidAmount();
                case 2 -> mode.ordinal();
                case 3 -> EmcFluidTierConfig.tierOf(redTank.getFluid().getFluid()) + 1;
                case 4 -> EmcFluidTierConfig.tierOf(blueTank.getFluid().getFluid()) + 1;
                case 5 -> EmcFluidTierConfig.enabledTiers();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 2) {
                mode = Mode.byId(value);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private Mode mode = Mode.UPGRADE;
    private int conversionProgress;

    public EmcConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.EMC_CONVERTER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EmcConverterBlockEntity blockEntity) {
        blockEntity.convert();
    }

    private void convert() {
        ConversionPlan plan = createConversionPlan();
        if (plan == null) {
            conversionProgress = 0;
            return;
        }

        conversionProgress++;
        if (conversionProgress < EmcFluidConfig.CONVERTER_TICKS_PER_BATCH.get()) {
            return;
        }
        conversionProgress = 0;
        redTank.drain(plan.inputPerBatch(), IFluidHandler.FluidAction.EXECUTE);
        blueTank.fill(new FluidStack(plan.outputFluid(), plan.outputPerBatch()), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    @Nullable
    private ConversionPlan createConversionPlan() {
        FluidStack input = redTank.getFluid();
        if (input.isEmpty()) {
            return null;
        }
        int sourceTier = EmcFluidTierConfig.tierOf(input.getFluid());
        if (!EmcFluidTierConfig.isEnabledTier(sourceTier)) {
            return null;
        }

        int targetTier;
        int inputPerBatch;
        int outputPerBatch;
        if (mode == Mode.UPGRADE) {
            targetTier = sourceTier + 1;
            inputPerBatch = EmcFluidTierConfig.upgradeInputAmount(sourceTier);
            outputPerBatch = 1;
        } else {
            targetTier = sourceTier - 1;
            inputPerBatch = 1;
            outputPerBatch = EmcFluidTierConfig.downgradeOutputAmount(sourceTier);
        }
        if (!EmcFluidTierConfig.isEnabledTier(targetTier) || inputPerBatch <= 0 || outputPerBatch <= 0) {
            return null;
        }

        Fluid outputFluid = ModContent.getEmcFluidSource(targetTier).get();
        if (blueTank.fill(new FluidStack(outputFluid, outputPerBatch), IFluidHandler.FluidAction.SIMULATE) != outputPerBatch) {
            return null;
        }
        if (input.getAmount() < inputPerBatch) {
            return null;
        }
        return new ConversionPlan(outputFluid, inputPerBatch, outputPerBatch);
    }

    private boolean canFillRed(FluidStack stack) {
        int tier = EmcFluidTierConfig.tierOf(stack.getFluid());
        if (!EmcFluidTierConfig.isEnabledTier(tier)) {
            return false;
        }
        return mode == Mode.UPGRADE
                ? tier < EmcFluidTierConfig.enabledTiers() - 1 && EmcFluidTierConfig.upgradeInputAmount(tier) > 0
                : tier > 0 && EmcFluidTierConfig.downgradeOutputAmount(tier) > 0;
    }

    public void toggleMode() {
        mode = mode == Mode.UPGRADE ? Mode.DOWNGRADE : Mode.UPGRADE;
        conversionProgress = 0;
        setChanged();
    }

    public ContainerData getData() {
        return data;
    }

    public int getRedFluidAmount() {
        return redTank.getFluidAmount();
    }

    public int getBlueFluidAmount() {
        return blueTank.getFluidAmount();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("RedTank", redTank.writeToNBT(new CompoundTag()));
        tag.put("BlueTank", blueTank.writeToNBT(new CompoundTag()));
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("ConversionProgress", conversionProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        redTank.readFromNBT(tag.getCompound("RedTank"));
        blueTank.readFromNBT(tag.getCompound("BlueTank"));
        mode = Mode.byId(tag.getInt("Mode"));
        conversionProgress = tag.getInt("ConversionProgress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.emcfluid.emc_converter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EmcConverterMenu(containerId, inventory, this, data);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        redInputCapability.invalidate();
        redOutputCapability.invalidate();
        blueOutputCapability.invalidate();
        combinedCapability.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null) {
                return combinedCapability.cast();
            }
            Direction facing = getFacing();
            if (side == facing) {
                return redInputCapability.cast();
            }
            if (side == facing.getOpposite()) {
                return blueOutputCapability.cast();
            }
            return redOutputCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    private Direction getFacing() {
        return getBlockState().hasProperty(MachineBlock.FACING)
                ? getBlockState().getValue(MachineBlock.FACING)
                : Direction.NORTH;
    }

    private class RedInputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return redTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return redTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return canFillRed(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return redTank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private class RedOutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return redTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return redTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return redTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return redTank.drain(maxDrain, action);
        }
    }

    private class BlueOutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return blueTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return blueTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return blueTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return blueTank.drain(maxDrain, action);
        }
    }

    private class CombinedFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? redTank.getFluid() : blueTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? redTank.getCapacity() : blueTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? redTank.isFluidValid(stack) : blueTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = redTank.fill(resource, action);
            if (filled > 0 || resource.isEmpty()) {
                return filled;
            }
            return blueTank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack drained = redTank.drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
            return blueTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = redTank.drain(maxDrain, action);
            if (!drained.isEmpty()) {
                return drained;
            }
            return blueTank.drain(maxDrain, action);
        }
    }

    public enum Mode {
        UPGRADE,
        DOWNGRADE;

        public static Mode byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : UPGRADE;
        }
    }

    private record ConversionPlan(Fluid outputFluid, int inputPerBatch, int outputPerBatch) {
    }
}
