package cn.gbk.emcfluid.util;

import moze_intel.projecte.api.ItemInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public record EmcCraftingTarget(ItemInfo info, ItemStack output, long emcValue, List<EmcFluidInput> fluidInputs,
                                int tierConfigHash) {
    public boolean isValid() {
        return emcValue > 0 && !output.isEmpty() && !fluidInputs.isEmpty();
    }

    public List<FluidStack> fluidStacksForQuantity(int quantity) {
        if (quantity <= 0) {
            return List.of();
        }
        return fluidInputs.stream()
                .map(input -> new FluidStack(input.fluid(), Math.toIntExact(Math.multiplyExact((long) input.amount(), quantity))))
                .toList();
    }
}
