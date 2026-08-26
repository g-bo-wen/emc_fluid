package cn.gbk.emcfluid.util;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class EmcCraftingTargets {
    private EmcCraftingTargets() {
    }

    public static List<EmcCraftingTarget> fromPattern(ItemStack pattern) {
        List<EmcCraftingTarget> targets = new ArrayList<EmcCraftingTarget>();
        for (ItemStack knowledge : KnowledgePatternData.readForCrafting(pattern)) {
            ItemStack output = ProjectEAccess.normalizeKnowledgeStack(knowledge);
            if (output.isEmpty()) {
                continue;
            }
            long emc = ProjectEAccess.getEmcValue(output);
            Optional<List<EmcFluidInput>> inputs = CostResolver.resolve(emc);
            if (!inputs.isPresent()) {
                continue;
            }
            EmcCraftingTarget target = new EmcCraftingTarget(
                    EmcStackIdentity.fromStack(output), output, emc, inputs.get(), EmcFluidTierConfig.hash());
            if (target.isValid()) {
                targets.add(target);
            }
        }
        Collections.sort(targets, new java.util.Comparator<EmcCraftingTarget>() {
            @Override
            public int compare(EmcCraftingTarget left, EmcCraftingTarget right) {
                return left.info().compareTo(right.info());
            }
        });
        return Collections.unmodifiableList(targets);
    }
}
