package cn.gbk.emcfluid.util;

import moze_intel.projecte.api.ItemInfo;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class EmcCraftingTargets {
    private EmcCraftingTargets() {
    }

    public static List<EmcCraftingTarget> fromPattern(ItemStack pattern) {
        List<EmcCraftingTarget> targets = new ArrayList<>();
        for (ItemInfo info : KnowledgePatternData.readForCrafting(pattern)) {
            long emc = ProjectEAccess.getEmcValue(info);
            CostResolver.resolve(emc)
                    .map(inputs -> new EmcCraftingTarget(info, info.createStack(), emc, inputs, EmcFluidTierConfig.hash()))
                    .filter(EmcCraftingTarget::isValid)
                    .ifPresent(targets::add);
        }
        return targets;
    }
}
