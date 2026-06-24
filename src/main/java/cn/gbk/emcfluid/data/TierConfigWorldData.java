package cn.gbk.emcfluid.data;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class TierConfigWorldData extends SavedData {
    private static final String DATA_NAME = EmcFluid.MODID + "_tier_config";
    private static final String TAG_HASH = "TierHash";
    private int tierHash;

    public static TierConfigWorldData load(CompoundTag tag) {
        TierConfigWorldData data = new TierConfigWorldData();
        data.tierHash = tag.getInt(TAG_HASH);
        return data;
    }

    public static void checkAndUpdate(MinecraftServer server) {
        TierConfigWorldData data = server.overworld().getDataStorage()
                .computeIfAbsent(TierConfigWorldData::load, TierConfigWorldData::new, DATA_NAME);
        int currentHash = EmcFluidTierConfig.hash();
        if (data.tierHash != 0 && data.tierHash != currentHash) {
            EmcFluid.LOGGER.warn("EMC Fluid tier values changed. Existing stored EMC fluids will be revalued according to the new configuration.");
        }
        if (data.tierHash != currentHash) {
            data.tierHash = currentHash;
            data.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG_HASH, tierHash);
        return tag;
    }
}
