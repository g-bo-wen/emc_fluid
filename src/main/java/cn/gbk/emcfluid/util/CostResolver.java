package cn.gbk.emcfluid.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CostResolver {
    private static final ConcurrentMap<CacheKey, Optional<List<EmcFluidInput>>> CACHE = new ConcurrentHashMap<>();

    private CostResolver() {
    }

    public static Optional<List<EmcFluidInput>> resolve(long emcValue) {
        if (emcValue <= 0) {
            return Optional.empty();
        }
        List<Long> tierValues = EmcFluidTierConfig.tierValues();
        return CACHE.computeIfAbsent(new CacheKey(tierValues, emcValue),
                key -> resolveUncached(key.emcValue(), key.tierValues()));
    }

    private static Optional<List<EmcFluidInput>> resolveUncached(long emcValue, List<Long> tierValues) {
        long remaining = emcValue;
        List<EmcFluidInput> inputs = new ArrayList<>();
        for (int tier = tierValues.size() - 1; tier >= 0; tier--) {
            long value = tierValues.get(tier);
            long amount = remaining / value;
            remaining %= value;
            if (amount > 0) {
                if (amount > Integer.MAX_VALUE) {
                    return Optional.empty();
                }
                inputs.add(new EmcFluidInput(tier, Math.toIntExact(amount)));
            }
        }
        return remaining == 0 ? Optional.of(List.copyOf(inputs)) : Optional.empty();
    }

    private record CacheKey(List<Long> tierValues, long emcValue) {
        private CacheKey {
            tierValues = List.copyOf(tierValues);
        }
    }
}
