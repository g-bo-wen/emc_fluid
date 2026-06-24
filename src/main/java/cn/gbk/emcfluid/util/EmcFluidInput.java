package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public record EmcFluidInput(int tierIndex, int amount) {
    public EmcFluidInput {
        if (tierIndex < 0 || tierIndex >= EmcFluidTierConfig.MAX_TIERS) {
            throw new IllegalArgumentException("Invalid EMC Fluid tier index: " + tierIndex);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public Fluid fluid() {
        return ModContent.getEmcFluidSource(tierIndex).get();
    }

    public FluidStack stack() {
        return new FluidStack(fluid(), amount);
    }

    public boolean matches(Fluid fluid) {
        return EmcFluidTierConfig.tierOf(fluid) == tierIndex;
    }
}
