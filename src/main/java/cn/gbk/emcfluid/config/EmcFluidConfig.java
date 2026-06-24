package cn.gbk.emcfluid.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class EmcFluidConfig {
    public static final List<Long> DEFAULT_TIER_VALUES = List.of(1L, 100L, 10_000L, 1_000_000L, 100_000_000L);
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue ENABLED_TIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Long>> TIER_VALUES;
    public static final ForgeConfigSpec.IntValue CONVERTER_TICKS_PER_BATCH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("emc_fluid_tiers");
        ENABLED_TIERS = builder
                .comment("Number of enabled EMC Fluid tiers. Valid range: 1-5.")
                .defineInRange("enabled_tiers", 5, 1, 5);
        TIER_VALUES = builder
                .comment("EMC value per mB for each tier. T1 must be 1. Values should be ascending.")
                .defineList("tier_values", DEFAULT_TIER_VALUES, value -> value instanceof Number && ((Number) value).longValue() > 0);
        builder.pop();
        builder.push("machines");
        CONVERTER_TICKS_PER_BATCH = builder
                .comment("Number of ticks required for each EMC Converter conversion batch.")
                .defineInRange("converter_ticks_per_batch", 5, 1, 200);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private EmcFluidConfig() {
    }
}
