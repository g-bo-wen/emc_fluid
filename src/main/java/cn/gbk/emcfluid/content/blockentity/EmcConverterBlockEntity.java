package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.content.block.MachineBlock;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class EmcConverterBlockEntity extends MachineTileEntity implements ITickable {
    public static final int TANK_CAPACITY = 10_000;

    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack stack) {
            return stack != null && canAcceptInput(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            markDirtyAndNotify();
        }
    };
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack stack) {
            return stack != null && EmcFluidTierConfig.isEnabledEmcFluid(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            markDirtyAndNotify();
        }
    };
    private final IFluidHandler inputHandler = new TankView(inputTank, true, false);
    private final IFluidHandler inputOutputHandler = new TankView(inputTank, false, true);
    private final IFluidHandler convertedOutputHandler = new TankView(outputTank, false, true);
    private final IFluidHandler combinedHandler = new CombinedHandler();

    private Mode mode = Mode.UPGRADE;
    private int conversionProgress;

    public EmcConverterBlockEntity() {
        inputTank.setTileEntity(this);
        outputTank.setTileEntity(this);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        ConversionPlan plan = createConversionPlan();
        if (plan == null) {
            conversionProgress = 0;
            return;
        }
        conversionProgress++;
        if (conversionProgress < EmcFluidConfig.getConverterTicksPerBatch()) {
            return;
        }
        conversionProgress = 0;
        FluidStack drained = inputTank.drain(plan.inputAmount, true);
        if (drained == null || drained.amount != plan.inputAmount) {
            if (drained != null && drained.amount > 0) {
                inputTank.fill(drained, true);
            }
            return;
        }
        int filled = outputTank.fill(new FluidStack(plan.outputFluid, plan.outputAmount), true);
        if (filled != plan.outputAmount) {
            if (filled > 0) {
                outputTank.drain(new FluidStack(plan.outputFluid, filled), true);
            }
            inputTank.fill(drained, true);
        }
    }

    @Nullable
    public ConversionPlan createConversionPlan() {
        FluidStack input = inputTank.getFluid();
        if (input == null || input.amount <= 0) {
            return null;
        }
        int sourceTier = EmcFluidTierConfig.tierOf(input.getFluid());
        if (!EmcFluidTierConfig.isEnabledTier(sourceTier)) {
            return null;
        }
        int targetTier;
        int inputAmount;
        int outputAmount;
        if (mode == Mode.UPGRADE) {
            targetTier = sourceTier + 1;
            inputAmount = EmcFluidTierConfig.upgradeInputAmount(sourceTier);
            outputAmount = 1;
        } else {
            targetTier = sourceTier - 1;
            inputAmount = 1;
            outputAmount = EmcFluidTierConfig.downgradeOutputAmount(sourceTier);
        }
        if (!EmcFluidTierConfig.isEnabledTier(targetTier)
                || inputAmount <= 0 || outputAmount <= 0 || input.amount < inputAmount) {
            return null;
        }
        Fluid output = ModContent.getEmcFluid(targetTier);
        if (outputTank.fill(new FluidStack(output, outputAmount), false) != outputAmount) {
            return null;
        }
        return new ConversionPlan(output, inputAmount, outputAmount);
    }

    private boolean canAcceptInput(Fluid fluid) {
        int tier = EmcFluidTierConfig.tierOf(fluid);
        return EmcFluidTierConfig.isEnabledTier(tier)
                && (mode == Mode.UPGRADE
                ? tier + 1 < EmcFluidTierConfig.enabledTiers() && EmcFluidTierConfig.upgradeInputAmount(tier) > 0
                : tier > 0 && EmcFluidTierConfig.downgradeOutputAmount(tier) > 0);
    }

    public void toggleMode() {
        mode = mode == Mode.UPGRADE ? Mode.DOWNGRADE : Mode.UPGRADE;
        conversionProgress = 0;
        markDirtyAndNotify();
    }

    public Mode getMode() {
        return mode;
    }

    public int getInputAmount() {
        return inputTank.getFluidAmount();
    }

    public int getOutputAmount() {
        return outputTank.getFluidAmount();
    }

    public int getInputTier() {
        FluidStack fluid = inputTank.getFluid();
        return fluid == null ? -1 : EmcFluidTierConfig.tierOf(fluid.getFluid());
    }

    public int getOutputTier() {
        FluidStack fluid = outputTank.getFluid();
        return fluid == null ? -1 : EmcFluidTierConfig.tierOf(fluid.getFluid());
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("RedTank", inputTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("BlueTank", outputTank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("Mode", mode.ordinal());
        compound.setInteger("ConversionProgress", conversionProgress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inputTank.readFromNBT(compound.getCompoundTag("RedTank"));
        outputTank.readFromNBT(compound.getCompoundTag("BlueTank"));
        sanitizeFluidTank(inputTank);
        sanitizeFluidTank(outputTank);
        clearNonEmcFluid(inputTank);
        clearNonEmcFluid(outputTank);
        mode = Mode.byId(compound.getInteger("Mode"));
        int maximumProgress = Math.max(0, EmcFluidConfig.getConverterTicksPerBatch() - 1);
        conversionProgress = Math.max(0,
                Math.min(maximumProgress, compound.getInteger("ConversionProgress")));
    }

    private static void clearNonEmcFluid(FluidTank tank) {
        FluidStack fluid = tank.getFluid();
        if (fluid != null && EmcFluidTierConfig.tierOf(fluid.getFluid()) < 0) {
            tank.setFluid(null);
        }
    }

    private EnumFacing getFacing() {
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof MachineBlock && state.getProperties().containsKey(net.minecraft.block.BlockHorizontal.FACING)) {
                return state.getValue(net.minecraft.block.BlockHorizontal.FACING);
            }
        }
        return EnumFacing.NORTH;
    }

    private IFluidHandler handlerFor(@Nullable EnumFacing side) {
        if (side == null) {
            return combinedHandler;
        }
        EnumFacing facing = getFacing();
        if (side == facing) {
            return inputHandler;
        }
        if (side == facing.getOpposite()) {
            return convertedOutputHandler;
        }
        return inputOutputHandler;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) handlerFor(facing);
        }
        return super.getCapability(capability, facing);
    }

    public enum Mode {
        UPGRADE,
        DOWNGRADE;

        public static Mode byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : UPGRADE;
        }
    }

    public static final class ConversionPlan {
        public final Fluid outputFluid;
        public final int inputAmount;
        public final int outputAmount;

        private ConversionPlan(Fluid outputFluid, int inputAmount, int outputAmount) {
            this.outputFluid = outputFluid;
            this.inputAmount = inputAmount;
            this.outputAmount = outputAmount;
        }
    }

    private static final class TankView implements IFluidHandler {
        private final FluidTank tank;
        private final boolean fill;
        private final boolean drain;

        private TankView(FluidTank tank, boolean fill, boolean drain) {
            this.tank = tank;
            this.fill = fill;
            this.drain = drain;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return tank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return fill ? tank.fill(resource, doFill) : 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return drain ? tank.drain(resource, doDrain) : null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return drain ? tank.drain(maxDrain, doDrain) : null;
        }
    }

    private final class CombinedHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] first = inputTank.getTankProperties();
            IFluidTankProperties[] second = outputTank.getTankProperties();
            IFluidTankProperties[] result = new IFluidTankProperties[first.length + second.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            return result;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return inputTank.fill(resource, doFill);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack drained = inputTank.drain(resource, doDrain);
            return drained != null ? drained : outputTank.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack drained = inputTank.drain(maxDrain, doDrain);
            return drained != null ? drained : outputTank.drain(maxDrain, doDrain);
        }
    }
}
