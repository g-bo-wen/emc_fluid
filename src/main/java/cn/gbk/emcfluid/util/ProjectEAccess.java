package cn.gbk.emcfluid.util;

import moze_intel.projecte.api.item.IItemEmc;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProjectEAccess {
    private ProjectEAccess() {
    }

    public static boolean isEmcHolder(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof IItemEmc;
    }

    public static long getStoredEmc(ItemStack stack) {
        IItemEmc item = getHolder(stack);
        return item == null ? 0L : Math.max(0L, item.getStoredEmc(stack));
    }

    public static long getMaximumEmc(ItemStack stack) {
        IItemEmc item = getHolder(stack);
        return item == null ? 0L : Math.max(0L, item.getMaximumEmc(stack));
    }

    public static long getEmcValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        return proxy == null ? 0L : Math.max(0L, proxy.getValue(normalizeKnowledgeStack(stack)));
    }

    public static long getEmcValue(EmcStackIdentity identity) {
        return identity == null ? 0L : getEmcValue(identity.createStack());
    }

    public static List<ItemStack> getKnowledge(UUID playerUUID) {
        if (playerUUID == null) {
            return Collections.emptyList();
        }
        ITransmutationProxy proxy = ProjectEAPI.getTransmutationProxy();
        if (proxy == null) {
            return Collections.emptyList();
        }
        return copyKnowledge(proxy.getKnowledgeProviderFor(playerUUID));
    }

    public static boolean hasFullKnowledge(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        ITransmutationProxy proxy = ProjectEAPI.getTransmutationProxy();
        return proxy != null && proxy.getKnowledgeProviderFor(playerUUID).hasFullKnowledge();
    }

    public static ItemStack normalizeKnowledgeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return normalized;
    }

    public static List<ItemStack> copyKnowledge(IKnowledgeProvider provider) {
        if (provider == null) {
            return Collections.emptyList();
        }
        Map<EmcStackIdentity, ItemStack> unique = new LinkedHashMap<EmcStackIdentity, ItemStack>();
        for (ItemStack stack : provider.getKnowledge()) {
            ItemStack normalized = normalizeKnowledgeStack(stack);
            if (normalized.isEmpty() || normalized.getItem().getRegistryName() == null) {
                continue;
            }
            EmcStackIdentity identity = EmcStackIdentity.fromStack(normalized);
            if (!unique.containsKey(identity)) {
                unique.put(identity, normalized);
            }
        }
        List<Map.Entry<EmcStackIdentity, ItemStack>> entries =
                new ArrayList<Map.Entry<EmcStackIdentity, ItemStack>>(unique.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<EmcStackIdentity, ItemStack>>() {
            @Override
            public int compare(Map.Entry<EmcStackIdentity, ItemStack> left,
                               Map.Entry<EmcStackIdentity, ItemStack> right) {
                return left.getKey().compareTo(right.getKey());
            }
        });
        List<ItemStack> result = new ArrayList<ItemStack>(entries.size());
        for (Map.Entry<EmcStackIdentity, ItemStack> entry : entries) {
            result.add(entry.getValue().copy());
        }
        return Collections.unmodifiableList(result);
    }

    public static long extractEmc(ItemStack stack, long amount, boolean execute) {
        if (amount <= 0L) {
            return 0L;
        }
        IItemEmc item = getHolder(stack);
        if (item == null) {
            return 0L;
        }
        long available = Math.min(amount, Math.max(0L, item.getStoredEmc(stack)));
        if (!execute || available <= 0L) {
            return available;
        }
        return clampResult(item.extractEmc(stack, available), available);
    }

    public static long insertEmc(ItemStack stack, long amount, boolean execute) {
        if (amount <= 0L) {
            return 0L;
        }
        IItemEmc item = getHolder(stack);
        if (item == null) {
            return 0L;
        }
        long stored = Math.max(0L, item.getStoredEmc(stack));
        long maximum = Math.max(stored, item.getMaximumEmc(stack));
        long room = maximum - stored;
        long acceptable = Math.min(amount, room);
        if (!execute || acceptable <= 0L) {
            return acceptable;
        }
        return clampResult(item.addEmc(stack, acceptable), acceptable);
    }

    private static IItemEmc getHolder(ItemStack stack) {
        return isEmcHolder(stack) ? (IItemEmc) stack.getItem() : null;
    }

    private static long clampResult(long result, long requested) {
        return Math.max(0L, Math.min(result, requested));
    }
}
