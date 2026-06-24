package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EmcFluidTierConfig {
    public static final int MAX_TIERS = 5;
    private static boolean warnedInvalidConfig;

    private EmcFluidTierConfig() {
    }

    public static List<Long> tierValues() {
        int enabled = Math.max(1, Math.min(MAX_TIERS, EmcFluidConfig.ENABLED_TIERS.get()));
        List<? extends Long> configured = EmcFluidConfig.TIER_VALUES.get();
        if (configured.size() < enabled) {
            warnInvalidConfig("Configured tier_values has fewer entries than enabled_tiers");
            return EmcFluidConfig.DEFAULT_TIER_VALUES.subList(0, enabled);
        }

        List<Long> values = new ArrayList<>(enabled);
        long previous = 0;
        for (int i = 0; i < enabled; i++) {
            Object raw = configured.get(i);
            if (!(raw instanceof Number number)) {
                warnInvalidConfig("Configured tier_values contains a non-number entry");
                return EmcFluidConfig.DEFAULT_TIER_VALUES.subList(0, enabled);
            }
            long value = number.longValue();
            if (value <= 0 || (i == 0 && value != 1) || value <= previous) {
                warnInvalidConfig("Configured tier_values must be positive, ascending, and start with T1 = 1");
                return EmcFluidConfig.DEFAULT_TIER_VALUES.subList(0, enabled);
            }
            values.add(value);
            previous = value;
        }
        return List.copyOf(values);
    }

    public static int enabledTiers() {
        return tierValues().size();
    }

    public static long value(int tierIndex) {
        return tierValues().get(tierIndex);
    }

    public static int hash() {
        return Objects.hash(tierValues());
    }

    public static boolean isEnabledTier(int tierIndex) {
        return tierIndex >= 0 && tierIndex < enabledTiers();
    }

    public static int tierOf(Fluid fluid) {
        for (int i = 0; i < MAX_TIERS; i++) {
            if (fluid == ModContent.getEmcFluidSource(i).get() || fluid == ModContent.getEmcFluidFlowing(i).get()) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isEnabledEmcFluid(Fluid fluid) {
        int tier = tierOf(fluid);
        return isEnabledTier(tier);
    }

    public static int upgradeInputAmount(int sourceTier) {
        if (!isEnabledTier(sourceTier) || !isEnabledTier(sourceTier + 1)) {
            return 0;
        }
        long sourceValue = value(sourceTier);
        long targetValue = value(sourceTier + 1);
        if (targetValue % sourceValue != 0 || targetValue / sourceValue > Integer.MAX_VALUE) {
            return 0;
        }
        return Math.toIntExact(targetValue / sourceValue);
    }

    public static int downgradeOutputAmount(int sourceTier) {
        if (!isEnabledTier(sourceTier) || !isEnabledTier(sourceTier - 1)) {
            return 0;
        }
        long sourceValue = value(sourceTier);
        long targetValue = value(sourceTier - 1);
        if (sourceValue % targetValue != 0 || sourceValue / targetValue > Integer.MAX_VALUE) {
            return 0;
        }
        return Math.toIntExact(sourceValue / targetValue);
    }

    private static void warnInvalidConfig(String message) {
        if (!warnedInvalidConfig) {
            warnedInvalidConfig = true;
            EmcFluid.LOGGER.warn("{}; using default EMC Fluid tier values", message);
        }
    }
}
