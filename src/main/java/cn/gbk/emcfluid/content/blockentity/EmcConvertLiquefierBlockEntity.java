package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class EmcConvertLiquefierBlockEntity extends EmcLiquefierBlockEntity {
    private int selectedTier;

    @Override
    protected Fluid getAcceptedFluid() {
        selectedTier = clampRegisteredTier(selectedTier);
        return ModContent.getEmcFluid(selectedTier);
    }

    @Override
    protected long getEmcPerMb() {
        selectedTier = clampRegisteredTier(selectedTier);
        return EmcFluidTierConfig.isEnabledTier(selectedTier) ? EmcFluidTierConfig.value(selectedTier) : 0L;
    }

    @Override
    protected boolean isStoredFluidValid(FluidStack fluid) {
        return fluid != null && EmcFluidTierConfig.tierOf(fluid.getFluid()) >= 0;
    }

    public int getSelectedTier() {
        return selectedTier;
    }

    public void changeTier(int delta) {
        int next = clampTier(selectedTier + (delta > 0 ? 1 : -1));
        if (next != selectedTier && tank.getFluidAmount() == 0) {
            selectedTier = next;
            markDirtyAndNotify();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("SelectedTier", selectedTier);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        selectedTier = clampRegisteredTier(compound.getInteger("SelectedTier"));
        super.readFromNBT(compound);
        FluidStack fluid = tank.getFluid();
        if (fluid != null) {
            int fluidTier = EmcFluidTierConfig.tierOf(fluid.getFluid());
            if (fluidTier >= 0) {
                selectedTier = fluidTier;
            }
        } else {
            selectedTier = clampTier(selectedTier);
        }
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(tier, EmcFluidTierConfig.enabledTiers() - 1));
    }

    private static int clampRegisteredTier(int tier) {
        return Math.max(0, Math.min(tier, EmcFluidTierConfig.MAX_TIERS - 1));
    }
}
