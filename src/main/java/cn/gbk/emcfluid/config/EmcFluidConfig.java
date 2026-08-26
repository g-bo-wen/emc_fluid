package cn.gbk.emcfluid.config;

import cn.gbk.emcfluid.EmcFluid;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmcFluidConfig {
    private static final long[] DEFAULT_TIER_VALUES = {1L, 100L, 10_000L, 1_000_000L, 100_000_000L};
    private static int enabledTiers = 5;
    private static int converterTicksPerBatch = 5;
    private static List<Long> tierValues = defaults();

    private EmcFluidConfig() {
    }

    public static void load(File file) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            enabledTiers = configuration.getInt("enabled_tiers", "emc_fluid_tiers", 5, 1, 5,
                    "Number of enabled EMC Fluid tiers.");
            converterTicksPerBatch = configuration.getInt("converter_ticks_per_batch", "machines", 5, 1, 200,
                    "Ticks required for one converter batch.");
            String[] raw = configuration.getStringList("tier_values", "emc_fluid_tiers",
                    new String[]{"1", "100", "10000", "1000000", "100000000"},
                    "Positive ascending EMC value per mB; T1 must equal 1.");
            tierValues = parseTierValues(raw);
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }

    public static int getEnabledTiers() {
        return enabledTiers;
    }

    public static int getConverterTicksPerBatch() {
        return converterTicksPerBatch;
    }

    public static List<Long> getTierValues() {
        return tierValues;
    }

    private static List<Long> parseTierValues(String[] raw) {
        if (raw.length < enabledTiers) {
            return invalid("tier_values has fewer entries than enabled_tiers");
        }
        List<Long> parsed = new ArrayList<Long>(enabledTiers);
        long previous = 0L;
        for (int i = 0; i < enabledTiers; i++) {
            try {
                long value = Long.parseLong(raw[i]);
                if (value <= 0L || (i == 0 && value != 1L) || value <= previous) {
                    return invalid("tier_values must be positive, ascending, and start at 1");
                }
                parsed.add(value);
                previous = value;
            } catch (NumberFormatException exception) {
                return invalid("tier_values contains a non-integer entry");
            }
        }
        return Collections.unmodifiableList(parsed);
    }

    private static List<Long> invalid(String reason) {
        if (EmcFluid.logger != null) {
            EmcFluid.logger.warn("{}; using default EMC Fluid tier values", reason);
        }
        return defaults().subList(0, enabledTiers);
    }

    private static List<Long> defaults() {
        List<Long> values = new ArrayList<Long>(DEFAULT_TIER_VALUES.length);
        for (long value : DEFAULT_TIER_VALUES) {
            values.add(value);
        }
        return Collections.unmodifiableList(values);
    }
}
