package cn.gbk.emcfluid.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CostResolver {
    private static final ConcurrentMap<CacheKey, Optional<List<EmcFluidInput>>> CACHE =
            new ConcurrentHashMap<CacheKey, Optional<List<EmcFluidInput>>>();

    private CostResolver() {
    }

    public static Optional<List<EmcFluidInput>> resolve(long emcValue) {
        if (emcValue <= 0L) {
            return Optional.empty();
        }
        CacheKey key = new CacheKey(EmcFluidTierConfig.tierValues(), emcValue);
        Optional<List<EmcFluidInput>> cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<List<EmcFluidInput>> resolved = resolveUncached(emcValue, key.tierValues);
        Optional<List<EmcFluidInput>> previous = CACHE.putIfAbsent(key, resolved);
        return previous == null ? resolved : previous;
    }

    private static Optional<List<EmcFluidInput>> resolveUncached(long emcValue, List<Long> tierValues) {
        long remaining = emcValue;
        List<EmcFluidInput> inputs = new ArrayList<EmcFluidInput>();
        for (int tier = tierValues.size() - 1; tier >= 0; tier--) {
            long value = tierValues.get(tier);
            long amount = remaining / value;
            remaining %= value;
            if (amount > 0L) {
                if (amount > Integer.MAX_VALUE) {
                    return Optional.empty();
                }
                inputs.add(new EmcFluidInput(tier, (int) amount));
            }
        }
        return remaining == 0L
                ? Optional.of(Collections.unmodifiableList(inputs))
                : Optional.<List<EmcFluidInput>>empty();
    }

    private static final class CacheKey {
        private final List<Long> tierValues;
        private final long emcValue;

        private CacheKey(List<Long> tierValues, long emcValue) {
            this.tierValues = Collections.unmodifiableList(new ArrayList<Long>(tierValues));
            this.emcValue = emcValue;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) object;
            return emcValue == other.emcValue && tierValues.equals(other.tierValues);
        }

        @Override
        public int hashCode() {
            int result = tierValues.hashCode();
            result = 31 * result + (int) (emcValue ^ (emcValue >>> 32));
            return result;
        }
    }
}
