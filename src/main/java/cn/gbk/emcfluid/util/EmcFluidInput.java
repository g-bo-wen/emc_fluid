package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.registry.ModContent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class EmcFluidInput {
    private final int tierIndex;
    private final int amount;

    public EmcFluidInput(int tierIndex, int amount) {
        if (tierIndex < 0 || tierIndex >= EmcFluidTierConfig.MAX_TIERS) {
            throw new IllegalArgumentException("Invalid EMC Fluid tier index: " + tierIndex);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.tierIndex = tierIndex;
        this.amount = amount;
    }

    public int tierIndex() {
        return tierIndex;
    }

    public int amount() {
        return amount;
    }

    public Fluid fluid() {
        return ModContent.getEmcFluid(tierIndex);
    }

    public FluidStack stack() {
        return new FluidStack(fluid(), amount);
    }

    public boolean matches(Fluid fluid) {
        return EmcFluidTierConfig.tierOf(fluid) == tierIndex;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EmcFluidInput)) {
            return false;
        }
        EmcFluidInput other = (EmcFluidInput) object;
        return tierIndex == other.tierIndex && amount == other.amount;
    }

    @Override
    public int hashCode() {
        return 31 * tierIndex + amount;
    }
}
