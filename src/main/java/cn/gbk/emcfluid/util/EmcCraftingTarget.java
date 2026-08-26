package cn.gbk.emcfluid.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmcCraftingTarget {
    private final EmcStackIdentity info;
    private final ItemStack output;
    private final long emcValue;
    private final List<EmcFluidInput> fluidInputs;
    private final int tierConfigHash;

    public EmcCraftingTarget(EmcStackIdentity info, ItemStack output, long emcValue,
                             List<EmcFluidInput> fluidInputs, int tierConfigHash) {
        this.info = info;
        this.output = output.copy();
        this.output.setCount(1);
        this.emcValue = emcValue;
        this.fluidInputs = Collections.unmodifiableList(new ArrayList<EmcFluidInput>(fluidInputs));
        this.tierConfigHash = tierConfigHash;
    }

    public EmcStackIdentity info() {
        return info;
    }

    public ItemStack output() {
        return output.copy();
    }

    public long emcValue() {
        return emcValue;
    }

    public List<EmcFluidInput> fluidInputs() {
        return fluidInputs;
    }

    public int tierConfigHash() {
        return tierConfigHash;
    }

    public boolean isValid() {
        return emcValue > 0L && !output.isEmpty() && !fluidInputs.isEmpty();
    }

    public List<FluidStack> fluidStacksForQuantity(int quantity) {
        if (quantity <= 0) {
            return Collections.emptyList();
        }
        List<FluidStack> stacks = new ArrayList<FluidStack>(fluidInputs.size());
        for (EmcFluidInput input : fluidInputs) {
            long amount = (long) input.amount() * quantity;
            if (amount > Integer.MAX_VALUE) {
                throw new ArithmeticException("EMC fluid input exceeds integer mB range");
            }
            stacks.add(new FluidStack(input.fluid(), (int) amount));
        }
        return stacks;
    }
}
