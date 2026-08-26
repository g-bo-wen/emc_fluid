package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraftforge.fluids.Fluid;

import java.util.List;

public final class EmcFluidTierConfig {
    public static final int MAX_TIERS = 5;

    private EmcFluidTierConfig() {
    }

    public static List<Long> tierValues() {
        return EmcFluidConfig.getTierValues();
    }

    public static int enabledTiers() {
        return EmcFluidConfig.getEnabledTiers();
    }

    public static long value(int tierIndex) {
        return tierValues().get(tierIndex);
    }

    public static int hash() {
        return 31 * enabledTiers() + tierValues().hashCode();
    }

    public static boolean isEnabledTier(int tierIndex) {
        return tierIndex >= 0 && tierIndex < enabledTiers();
    }

    public static int tierOf(Fluid fluid) {
        for (int i = 0; i < MAX_TIERS; i++) {
            if (fluid == ModContent.getEmcFluid(i)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isEnabledEmcFluid(Fluid fluid) {
        return isEnabledTier(tierOf(fluid));
    }

    public static int upgradeInputAmount(int sourceTier) {
        if (!isEnabledTier(sourceTier) || !isEnabledTier(sourceTier + 1)) {
            return 0;
        }
        long sourceValue = value(sourceTier);
        long targetValue = value(sourceTier + 1);
        long ratio = targetValue / sourceValue;
        return targetValue % sourceValue == 0L && ratio <= Integer.MAX_VALUE ? (int) ratio : 0;
    }

    public static int downgradeOutputAmount(int sourceTier) {
        if (!isEnabledTier(sourceTier) || !isEnabledTier(sourceTier - 1)) {
            return 0;
        }
        long sourceValue = value(sourceTier);
        long targetValue = value(sourceTier - 1);
        long ratio = sourceValue / targetValue;
        return sourceValue % targetValue == 0L && ratio <= Integer.MAX_VALUE ? (int) ratio : 0;
    }
}
