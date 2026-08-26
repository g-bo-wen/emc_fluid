package cn.gbk.emcfluid.data;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public final class TierConfigWorldData extends WorldSavedData {
    private static final String DATA_NAME = EmcFluid.MODID + "_tier_config";
    private static final String TAG_HASH = "TierHash";
    private int tierHash;

    public TierConfigWorldData() {
        super(DATA_NAME);
    }

    public TierConfigWorldData(String name) {
        super(name);
    }

    public static void checkAndUpdate(MinecraftServer server) {
        World world = server.getWorld(0);
        MapStorage storage = world.getMapStorage();
        TierConfigWorldData data = (TierConfigWorldData) storage.getOrLoadData(TierConfigWorldData.class, DATA_NAME);
        if (data == null) {
            data = new TierConfigWorldData();
            storage.setData(DATA_NAME, data);
        }
        int currentHash = EmcFluidTierConfig.hash();
        if (data.tierHash != 0 && data.tierHash != currentHash) {
            EmcFluid.logger.warn("EMC Fluid tier values changed; existing fluid is revalued by the new configuration");
        }
        if (data.tierHash != currentHash) {
            data.tierHash = currentHash;
            data.markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        tierHash = nbt.getInteger(TAG_HASH);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger(TAG_HASH, tierHash);
        return compound;
    }
}
