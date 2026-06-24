package cn.gbk.emcfluid.util;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProjectEAccess {
    private ProjectEAccess() {
    }

    public static Optional<IItemEmcHolder> getEmcHolder(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
    }

    public static boolean isEmcHolder(ItemStack stack) {
        return getEmcHolder(stack).isPresent();
    }

    public static long extractEmc(ItemStack stack, long amount, boolean execute) {
        return getEmcHolder(stack)
                .map(holder -> holder.extractEmc(stack, amount, EmcAction.get(execute)))
                .orElse(0L);
    }

    public static long insertEmc(ItemStack stack, long amount, boolean execute) {
        return getEmcHolder(stack)
                .map(holder -> holder.insertEmc(stack, amount, EmcAction.get(execute)))
                .orElse(0L);
    }

    public static long getEmcValue(ItemInfo info) {
        return IEMCProxy.INSTANCE.getValue(info);
    }

    public static List<ItemInfo> getKnowledge(UUID playerUUID) {
        return copyKnowledge(ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(playerUUID));
    }

    private static List<ItemInfo> copyKnowledge(IKnowledgeProvider provider) {
        List<ItemInfo> result = new ArrayList<>(provider.getKnowledge());
        result.sort(Comparator.comparing(ItemInfo::toString));
        return result;
    }
}
